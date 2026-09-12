package com.red.retrovein.mapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ExternalClassResolver {
	private final ClassLoader classLoader;
	private final ExternalMetadataReader metadataReader = new ExternalMetadataReader();
	private final Map<String, ClassMetadata> cache = new HashMap<String, ClassMetadata>();

	public ExternalClassResolver() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public ExternalClassResolver(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public ClassMetadata resolve(String internalName) {
		if (internalName == null) {
			return null;
		}

		if (cache.containsKey(internalName)) {
			return cache.get(internalName);
		}

		byte[] bytecode = readClass(internalName);

		if (bytecode == null) {
			return null;
		}

		ClassMetadata metadata = metadataReader.read(internalName, bytecode);

		cache.put(internalName, metadata);

		return metadata;
	}

	private byte[] readClass(String internalName) {
		String resourceName = internalName + ".class";

		InputStream input = null;

		if (classLoader != null) {
			input = classLoader.getResourceAsStream(resourceName);
		}

		if (input == null) {
			input = ClassLoader.getSystemResourceAsStream(resourceName);
		}

		if (input == null) {
			return null;
		}

		try {
			return readAll(input);
		} catch (IOException ignored) {
			return null;
		} finally {
			try {
				input.close();
			} catch (IOException ignored) {
			}
		}
	}

	private byte[] readAll(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		byte[] buffer = new byte[4096];
		int read;

		while ((read = input.read(buffer)) != -1) {
			output.write(buffer, 0, read);
		}

		return output.toByteArray();
	}
}
