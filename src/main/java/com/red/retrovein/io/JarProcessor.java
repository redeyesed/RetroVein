package com.red.retrovein.io;

import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.mapping.Mapping;
import com.red.retrovein.mapping.MappingBuilder;
import com.red.retrovein.mapping.file.MappingWriter;
import com.red.retrovein.transform.ClassTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class JarProcessor {
	private final List<ClassTransformer> transformers;
	private final int threads;

	/*
	 * Количество задач, которые могут находиться в состоянии submitted/running
	 * одновременно. threads * 2 позволяет workers не простаивать, но не даёт
	 * создать Future на каждый класс JAR.
	 */
	private static final int IN_FLIGHT_MULTIPLIER = 2;

	public JarProcessor(List<ClassTransformer> transformers, int threads) {
		if (transformers == null || transformers.isEmpty()) {
			throw new IllegalArgumentException("transformers must not be empty");
		}

		if (threads <= 0) {
			throw new IllegalArgumentException("threads must be greater than zero");
		}

		this.transformers = new ArrayList<ClassTransformer>(transformers);
		this.threads = threads;
	}

	public void process(Path input, Path output) throws IOException {
		ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);

		try (JarFile jar = new JarFile(input.toFile())) {

			/*
			 * Scan
			 */
			JarClassScanner scanner = new JarClassScanner();

			List<ClassInfo> classInfos = scanner.scan(jar);

			/*
			 * Mapping
			 */
			RetroLogger.info(LogCategory.Mapping, "Building mappings for {} classes", classInfos.size());

			MappingBuilder mappingBuilder = new MappingBuilder();

			Mapping mapping = mappingBuilder.build(classInfos);

			/*
			 * После построения mapping список больше не нужен. ClassInfo содержит byte[],
			 * поэтому освобождаем ссылку как можно раньше. Сам GC решит, когда память будет
			 * возвращена.
			 */
			classInfos.clear();

			/*
			 * Manifest
			 */
			Manifest manifest = createManifest(jar, mapping);

			/*
			 * Output
			 */
			Path outputParent = output.getParent();

			if (outputParent != null) {
				Files.createDirectories(outputParent);
			}

			/*
			 * Future существует только для ограниченного количества одновременно
			 * обрабатываемых классов.
			 */
			try (OutputStream fileOutput = Files.newOutputStream(output);
					JarOutputStream outputJar = new JarOutputStream(fileOutput, manifest)) {

				Set<String> writtenNames = new HashSet<String>();

				/*
				 * Сначала выясняем порядок entries, а данные читаем только тогда, когда до них
				 * дошла очередь. В памяти хранится только список имён/metadata JarEntry, а не
				 * содержимое всех ресурсов.
				 */
				List<JarEntry> entriesToWrite = collectEntries(jar);

				ExecutorCompletionService<ClassResult> completionService = new ExecutorCompletionService<ClassResult>(
						executor);

				/*
				 * Максимальное количество одновременно submitted/running задач.
				 */
				int maxInFlight = Math.max(threads, threads * IN_FLIGHT_MULTIPLIER);

				List<PendingClass> pendingClasses = new ArrayList<PendingClass>(maxInFlight);

				int resourceCount = 0;
				int classCount = 0;
				int transformedClasses = 0;

				for (JarEntry entry : entriesToWrite) {
					String entryName = entry.getName();

					if (entryName.endsWith(".class")) {

						byte[] classData;

						try (InputStream inputStream = jar.getInputStream(entry)) {
							classData = readAll(inputStream);
						}

						Future<ClassResult> future = completionService
								.submit(() -> transformClass(entryName, classData, mapping));

						pendingClasses.add(new PendingClass(entryName, future));
						classCount++;

						/*
						 * Как только достигнут лимит, ждём завершения самого раннего entry. Это
						 * ограничивает количество одновременно живущих Future и byte[].
						 */
						if (pendingClasses.size() >= maxInFlight) {
							PendingClass pending = pendingClasses.remove(0);

							ClassResult result = waitForResult(pending.future, pending.entryName, pendingClasses);

							writeClassResult(outputJar, writtenNames, result);

							transformedClasses++;

						}

					} else {

						byte[] data;

						try (InputStream inputStream = jar.getInputStream(entry)) {
							data = readAll(inputStream);
						}

						if (!writtenNames.add(entryName)) {
							throw new IOException("Duplicate output JAR entry: " + entryName);
						}

						writeEntry(outputJar, entryName, data);

						resourceCount++;

						RetroLogger.trace(LogCategory.Resource, "Wrote resource: {}", entryName);
					}
				}

				for (PendingClass pending : pendingClasses) {
					ClassResult result = waitForResult(pending.future, pending.entryName, pendingClasses);

					writeClassResult(outputJar, writtenNames, result);

					transformedClasses++;
				}

				pendingClasses.clear();

				RetroLogger.debug(LogCategory.Transform, "Submitted {} classes for transformation", classCount);

				RetroLogger.info(LogCategory.Transform, "Transformed {} classes", transformedClasses);

				RetroLogger.info(LogCategory.Resource, "Copied {} resources", resourceCount);
			}

			RetroLogger.info(LogCategory.Jar, "JAR written successfully");

			/*
			 * Mapping
			 */
			Path mappingOutput = getMappingOutputPath(output);

			MappingWriter mappingWriter = new MappingWriter();

			mappingWriter.write(mapping, mappingOutput);

			RetroLogger.info(LogCategory.Mapping, "Mapping written to {}", mappingOutput);

		} finally {

			executor.shutdown();

			RetroLogger.debug(LogCategory.Jar, "Worker executor shut down");
		}
	}

	/**
	 * Собирает metadata entries без чтения содержимого файлов. Это позволяет не
	 * хранить resources в памяти до момента записи.
	 */
	private static List<JarEntry> collectEntries(JarFile jar) {
		List<JarEntry> entriesToWrite = new ArrayList<JarEntry>();

		java.util.Enumeration<JarEntry> entries = jar.entries();

		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			if (entry.isDirectory()) {
				continue;
			}

			String entryName = entry.getName();

			if ("META-INF/MANIFEST.MF".equalsIgnoreCase(entryName)) {
				continue;
			}

			if (isSignatureEntry(entry)) {
				RetroLogger.debug(LogCategory.Jar, "Skipping JAR signature: {}", entryName);

				continue;
			}

			entriesToWrite.add(entry);
		}

		return entriesToWrite;
	}

	private ClassResult waitForResult(Future<ClassResult> future, String entryName, List<PendingClass> pendingClasses)
			throws IOException {

		try {

			return future.get();

		} catch (InterruptedException e) {

			for (PendingClass pending : pendingClasses) {
				pending.future.cancel(true);
			}

			Thread.currentThread().interrupt();

			throw new IOException("Interrupted while processing class: " + entryName, e);

		} catch (ExecutionException e) {

			for (PendingClass pending : pendingClasses) {
				pending.future.cancel(true);
			}

			Throwable cause = e.getCause();

			if (cause instanceof IOException) {
				throw (IOException) cause;
			}

			throw new IOException("Failed to transform class: " + entryName, cause);
		}
	}

	private static void writeClassResult(JarOutputStream outputJar, Set<String> writtenNames, ClassResult result)
			throws IOException {

		if (!writtenNames.add(result.name)) {
			throw new IOException("Duplicate output JAR entry: " + result.name);
		}

		writeEntry(outputJar, result.name, result.bytecode);

		RetroLogger.trace(LogCategory.Transform, "Wrote transformed class: {}", result.name);
	}

	private static Path getMappingOutputPath(Path output) {
		String fileName = output.getFileName().toString();

		int extensionIndex = fileName.lastIndexOf('.');

		if (extensionIndex == -1) {
			return output.resolveSibling(fileName + ".rvm");
		}

		return output.resolveSibling(fileName.substring(0, extensionIndex) + ".rvm");
	}

	private Manifest createManifest(JarFile jar, Mapping mapping) throws IOException {

		Manifest manifest = jar.getManifest();

		if (manifest == null) {

			RetroLogger.debug(LogCategory.Jar, "Input JAR has no manifest");

			manifest = new Manifest();

		} else {

			RetroLogger.debug(LogCategory.Jar, "Reading input manifest");

			manifest = copyManifest(manifest);

			removeDigests(manifest);
		}

		Attributes mainAttributes = manifest.getMainAttributes();

		if (mainAttributes.getValue(Attributes.Name.MANIFEST_VERSION) == null) {

			mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		}

		String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);

		if (mainClass != null) {

			String internalName = mainClass.replace('.', '/');

			String mappedName = mapping.getClassName(internalName);

			String outputName = mappedName.replace('/', '.');

			mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), outputName);

			RetroLogger.debug(LogCategory.Jar, "Manifest Main-Class: {} -> {}", mainClass, outputName);
		}

		return manifest;
	}

	private static Manifest copyManifest(Manifest source) {
		Manifest copy = new Manifest();

		copy.getMainAttributes().putAll(source.getMainAttributes());

		for (String name : source.getEntries().keySet()) {

			Attributes attributes = source.getAttributes(name);

			if (attributes != null) {
				copy.getEntries().put(name, new Attributes(attributes));
			}
		}

		return copy;
	}

	private ClassResult transformClass(String entryName, byte[] bytecode, Mapping mapping) {
		String className = entryName.substring(0, entryName.length() - ".class".length());

		RetroLogger.trace(LogCategory.Transform, "Transforming class: {}", className);

		byte[] transformed = bytecode;

		for (ClassTransformer transformer : transformers) {

			RetroLogger.trace(LogCategory.Transform, "Applying transformer {} to {}",
					transformer.getClass().getSimpleName(), className);

			transformed = transformer.transform(className, transformed, mapping);
		}

		String mappedClassName = mapping.getClassName(className);
		String outputName = mappedClassName + ".class";

		RetroLogger.trace(LogCategory.Transform, "Class output: {} -> {}", className, mappedClassName);

		return new ClassResult(outputName, transformed);
	}

	/**
	 * Проверяет, является ли запись файлом цифровой подписи JAR. Такие файлы
	 * необходимо исключить из выходного архива, поскольку после изменения байткода
	 * классов подпись становится недействительной.
	 */
	private static boolean isSignatureEntry(JarEntry entry) {
		String name = entry.getName();
		if (!name.startsWith("META-INF/")) {
			return false;
		}
		String fileName = name.substring("META-INF/".length()).toUpperCase();
		return fileName.endsWith(".SF") || fileName.endsWith(".RSA") || fileName.endsWith(".DSA")
				|| fileName.endsWith(".EC");
	}

	/**
	 * Очищает Digest-атрибуты из записей Manifest, которые становятся
	 * недействительными, а также удаляет оставшиеся пустые записи.
	 */
	private static void removeDigests(Manifest manifest) {
		Iterator<Map.Entry<String, Attributes>> iterator = manifest.getEntries().entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Attributes> entry = iterator.next();
			Attributes attributes = entry.getValue();
			Iterator<Object> attributeIterator = attributes.keySet().iterator();
			while (attributeIterator.hasNext()) {
				Attributes.Name name = (Attributes.Name) attributeIterator.next();
				if (name.toString().endsWith("-Digest")) {
					attributeIterator.remove();
				}
			}
			if (attributes.isEmpty()) {
				iterator.remove();
			}
		}
	}

	private static void writeEntry(JarOutputStream output, String name, byte[] data) throws IOException {
		JarEntry entry = new JarEntry(name);

		output.putNextEntry(entry);

		try {
			output.write(data);
		} finally {
			output.closeEntry();
		}
	}

	private static byte[] readAll(InputStream inputStream) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		byte[] buffer = new byte[8192];

		int count;

		while ((count = inputStream.read(buffer)) != -1) {
			output.write(buffer, 0, count);
		}

		return output.toByteArray();
	}

	private static final class PendingClass {
		private final String entryName;
		private final Future<ClassResult> future;

		private PendingClass(String entryName, Future<ClassResult> future) {
			this.entryName = entryName;
			this.future = future;
		}
	}

	private static final class ClassResult {
		private final String name;
		private final byte[] bytecode;

		private ClassResult(String name, byte[] bytecode) {
			this.name = name;
			this.bytecode = bytecode;
		}
	}
}
