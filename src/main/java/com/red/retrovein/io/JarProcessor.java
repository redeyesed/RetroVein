package com.red.retrovein.io;

import com.red.retrovein.transform.ClassTransformer;

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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class JarProcessor {
	private final ClassTransformer transformer;
	private final int threads;

	public JarProcessor(ClassTransformer transformer, int threads) {
		if (threads <= 0) {
			throw new IllegalArgumentException("threads must be greater than zero");
		}

		this.transformer = transformer;
		this.threads = threads;
	}

	public void process(Path input, Path output) throws IOException {
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		try (JarFile jar = new JarFile(input.toFile());
				OutputStream fileOutput = Files.newOutputStream(output);
				JarOutputStream outputJar = new JarOutputStream(fileOutput)) {

			List<Future<ClassResult>> tasks = new ArrayList<Future<ClassResult>>();

			java.util.Enumeration<JarEntry> entries = jar.entries();

			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();

				if (entry.isDirectory()) {
					continue;
				}

				byte[] data;

				try (InputStream inputStream = jar.getInputStream(entry)) {

					data = readAll(inputStream);
				}

				if (entry.getName().endsWith(".class")) {
					final String entryName = entry.getName();
					final byte[] classData = data;

					tasks.add(executor.submit(() -> transformClass(entryName, classData)));
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

		} finally {
			executor.shutdown();
		}
	}

	private ClassResult transformClass(String entryName, byte[] bytecode) {
		String className = entryName.substring(0, entryName.length() - ".class".length());

		byte[] transformed = transformer.transform(className, bytecode);

		return new ClassResult(entryName, transformed);
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
