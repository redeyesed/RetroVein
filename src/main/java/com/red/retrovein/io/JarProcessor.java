package com.red.retrovein.io;

import com.red.retrovein.mapping.Mapping;
import com.red.retrovein.mapping.MappingBuilder;
import com.red.retrovein.transform.ClassTransformer;
import com.red.retrovein.transform.TransformationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class JarProcessor {
	private final List<ClassTransformer> transformers;
	private final int threads;

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
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		try (JarFile jar = new JarFile(input.toFile())) {

			JarClassScanner scanner = new JarClassScanner();

			List<ClassInfo> classInfos = scanner.scan(jar);

			MappingBuilder mappingBuilder = new MappingBuilder();

			Mapping mapping = mappingBuilder.build(classInfos);

			TransformationContext context = new TransformationContext(mapping);

			Manifest manifest = createManifest(jar, mapping);

			try (OutputStream fileOutput = Files.newOutputStream(output);

					JarOutputStream outputJar = new JarOutputStream(fileOutput, manifest)) {

				List<Future<ClassResult>> tasks = new ArrayList<Future<ClassResult>>();

				java.util.Enumeration<JarEntry> entries = jar.entries();

				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();

					if (entry.isDirectory()) {
						continue;
					}

					/*
					 * Manifest уже записан JarOutputStream. Поэтому не копируем его повторно.
					 */
					if ("META-INF/MANIFEST.MF".equalsIgnoreCase(entry.getName())) {
						continue;
					}

					byte[] data;

					try (InputStream inputStream = jar.getInputStream(entry)) {

						data = readAll(inputStream);
					}

					if (entry.getName().endsWith(".class")) {
						final String entryName = entry.getName();

						final byte[] classData = data;

						tasks.add(executor.submit(() -> transformClass(entryName, classData, context)));

					} else {
						writeEntry(outputJar, entry.getName(), data);
					}
				}

				for (Future<ClassResult> task : tasks) {
					try {
						ClassResult result = task.get();

						writeEntry(outputJar, result.name, result.bytecode);

					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();

						throw new IOException("Interrupted while processing JAR", e);

					} catch (ExecutionException e) {
						throw new IOException("Failed to transform class", e.getCause());
					}
				}
			}

		} finally {
			executor.shutdown();
		}
	}

	private Manifest createManifest(JarFile jar, Mapping mapping) throws IOException {
		Manifest manifest = jar.getManifest();

		if (manifest == null) {
			manifest = new Manifest();
		} else {
			manifest = copyManifest(manifest);
		}

		Attributes mainAttributes = manifest.getMainAttributes();

		if (mainAttributes.getValue(Attributes.Name.MANIFEST_VERSION) == null) {

			mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		}

		String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);

		if (mainClass != null) {
			String internalName = mainClass.replace('.', '/');

			String mappedName = mapping.getClassName(internalName);

			mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), mappedName.replace('/', '.'));
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

	private ClassResult transformClass(String entryName, byte[] bytecode, TransformationContext context) {
		String className = entryName.substring(0, entryName.length() - ".class".length());

		byte[] transformed = bytecode;

		for (ClassTransformer transformer : transformers) {

			transformed = transformer.transform(className, transformed, context);
		}

		String outputName = context.getMapping().getClassName(className) + ".class";

		return new ClassResult(outputName, transformed);
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

	private static byte[] readAll(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		byte[] buffer = new byte[8192];

		int count;

		while ((count = input.read(buffer)) != -1) {

			output.write(buffer, 0, count);
		}

		return output.toByteArray();
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