package com.red.retrovein.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public final class ConfigLoader {
	private ConfigLoader() {
		throw new AssertionError("No instances");
	}

	/**
	 * Загружает конфигурацию. Если файл отсутствует, создаётся конфигурация со
	 * значениями по умолчанию.
	 */
	public static RetroConfig loadConfig() throws IOException {
		Path path = Paths.get("Retro.properties");
		if (Files.notExists(path)) {
			RetroConfig config = RetroConfig.createDefault();
			saveConfig(path, config);
			return config;
		}
		return parseConfig(readProperties(path));
	}

	public static void saveConfig(Path path, RetroConfig config) throws IOException {
		Objects.requireNonNull(path, "Configuration path cannot be null.");
		Objects.requireNonNull(config, "Configuration cannot be null.");
		Path file = path.toAbsolutePath().normalize();
		Path parent = file.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			writeConfig(writer, config);
		}
	}

	private static Properties readProperties(Path path) throws IOException {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}

	private static RetroConfig parseConfig(Properties properties) throws IOException {
		RetroConfig defaults = RetroConfig.createDefault();
		return new RetroConfig(getBoolean(properties, "mappingClasses", defaults.isMappingClasses()),
				getBoolean(properties, "mappingFields", defaults.isMappingFields()),
				getBoolean(properties, "mappingMethods", defaults.isMappingMethods()),
				getBoolean(properties, "mappingLocalVariables", defaults.isMappingLocalVariables()),
				getBoolean(properties, "mappingEnums", defaults.isMappingEnums()),
				getBoolean(properties, "packageRemap", defaults.isPackageRemap()),
				getBoolean(properties, "transformerAsmRemapping", defaults.isAsmRemappingTransformer()),
				getCoreThreads(properties, defaults.getCoreThreads()));
	}

	private static boolean getBoolean(Properties properties, String key, boolean defaultValue) throws IOException {
		String value = properties.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		value = value.trim();
		if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
			throw new IOException();
		}
		return Boolean.parseBoolean(value);
	}

	private static int getCoreThreads(Properties properties, int defaultValue) throws IOException {
		String value = properties.getProperty("coreThreads");
		if (value == null) {
			return defaultValue;
		}
		try {
			int threads = Integer.parseInt(value.trim());
			if (threads <= 0) {
				throw new IOException();
			}
			return threads;
		} catch (NumberFormatException exception) {
			throw new IOException(exception);
		}
	}

	private static void writeConfig(BufferedWriter writer, RetroConfig config) throws IOException {
		writer.write("# Mapping");
		writer.newLine();
		writeProperty(writer, "mappingClasses", config.isMappingClasses());
		writeProperty(writer, "mappingFields", config.isMappingFields());
		writeProperty(writer, "mappingMethods", config.isMappingMethods());
		writeProperty(writer, "mappingLocalVariables", config.isMappingLocalVariables());
		writeProperty(writer, "mappingEnums", config.isMappingEnums());
		writeSection(writer, "Packages");
		writeProperty(writer, "packageRemap", config.isPackageRemap());
		writeSection(writer, "Transformers");
		writeProperty(writer, "transformerAsmRemapping", config.isAsmRemappingTransformer());
		writeSection(writer, "Processing");
		writeProperty(writer, "coreThreads", config.getCoreThreads());
	}

	private static void writeProperty(BufferedWriter writer, String key, Object value) throws IOException {
		writer.write(key + "=" + value);
		writer.newLine();
	}

	private static void writeSection(BufferedWriter writer, String name) throws IOException {
		writer.newLine();
		writer.write("# " + name);
		writer.newLine();
	}
}
