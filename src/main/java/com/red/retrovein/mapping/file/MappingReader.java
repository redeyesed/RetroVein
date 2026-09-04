package com.red.retrovein.mapping.file;

import com.red.retrovein.mapping.Mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class MappingReader {
	private static final String HEADER = "# RetroVein Mapping";
	private static final int FORMAT_VERSION = 1;

	public Mapping read(Path input) throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}

		if (!Files.exists(input)) {
			throw new IOException("Mapping file does not exist: " + input);
		}

		Map<String, String> classes = new HashMap<String, String>();
		Map<String, String> fields = new HashMap<String, String>();
		Map<String, String> methods = new HashMap<String, String>();
		Map<String, String> localVariables = new HashMap<String, String>();

		boolean headerRead = false;
		boolean versionRead = false;

		try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
			String line;

			while ((line = reader.readLine()) != null) {

				line = line.trim();

				if (line.isEmpty()) {
					continue;
				}

				if (line.startsWith("#")) {

					if (HEADER.equals(line)) {
						headerRead = true;
					}
					continue;
				}

				if (line.startsWith("version:")) {
					readVersion(line);
					versionRead = true;
					continue;
				}

				if (line.startsWith("CLASS:")) {
					readMapping(line, "CLASS", classes);
					continue;
				}

				if (line.startsWith("FIELD:")) {
					readMapping(line, "FIELD", fields);
					continue;
				}

				if (line.startsWith("METHOD:")) {
					readMapping(line, "METHOD", methods);
					continue;
				}

				if (line.startsWith("LOCAL:")) {
					readMapping(line, "LOCAL", localVariables);
					continue;
				}

				throw new IOException("Unknown mapping entry: " + line);
			}
		}

		if (!headerRead) {
			throw new IOException("Invalid mapping file: missing header");
		}

		if (!versionRead) {
			throw new IOException("Invalid mapping file: missing version");
		}

		return new Mapping(classes, methods, fields, localVariables);
	}

	private void readVersion(String line) throws IOException {
		String value = line.substring("version:".length()).trim();

		if (value.isEmpty()) {
			throw new IOException("Invalid mapping version");
		}

		final int version;

		try {
			version = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IOException("Invalid mapping version: " + value, e);
		}

		if (version != FORMAT_VERSION) {
			throw new IOException("Unsupported mapping version: " + version);
		}
	}

	private void readMapping(String line, String type, Map<String, String> mappings) throws IOException {
		String prefix = type + ":";
		String content = line.substring(prefix.length()).trim();

		int separator = content.indexOf(" -> ");

		if (separator == -1) {
			throw new IOException("Invalid " + type + " mapping: " + line);
		}

		String original = content.substring(0, separator).trim();
		String mapped = content.substring(separator + " -> ".length()).trim();

		if (original.isEmpty()) {
			throw new IOException("Empty original name in " + type + " mapping: " + line);
		}

		if (mapped.isEmpty()) {
			throw new IOException("Empty mapped name in " + type + " mapping: " + line);
		}

		if (mappings.containsKey(original)) {
			throw new IOException("Duplicate " + type + " mapping: " + original);
		}

		mappings.put(original, mapped);
	}
}
