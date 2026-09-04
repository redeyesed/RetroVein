package com.red.retrovein.mapping.file;

import com.red.retrovein.mapping.Mapping;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MappingWriter {
	private static final String HEADER = "# RetroVein Mapping";
	private static final int FORMAT_VERSION = 1;

	public void write(Mapping mapping, Path output) throws IOException {
		if (mapping == null) {
			throw new IllegalArgumentException("Mapping must not be null");
		}

		if (output == null) {
			throw new IllegalArgumentException("Output must not be null");
		}

		Path parent = output.getParent();

		if (parent != null) {
			Files.createDirectories(parent);
		}

		try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

			writeHeader(writer);
			writeSection(writer, "CLASS", mapping.getClasses());
			writeSection(writer, "FIELD", mapping.getFields());
			writeSection(writer, "METHOD", mapping.getMethods());
			writeSection(writer, "LOCAL", mapping.getLocalVariables());
		}
	}

	private void writeHeader(BufferedWriter writer) throws IOException {
		writer.write(HEADER);
		writer.newLine();

		writer.write("version=");
		writer.write(Integer.toString(FORMAT_VERSION));
		writer.newLine();

		writer.newLine();
	}

	private void writeSection(BufferedWriter writer, String type, Map<String, String> mappings) throws IOException {

		List<String> keys = new ArrayList<String>(mappings.keySet());
		Collections.sort(keys);

		for (String key : keys) {
			String mappedName = mappings.get(key);

			if (mappedName == null) {
				continue;
			}

			writer.write(type);
			writer.write(": ");
			writer.write(key);
			writer.write(" -> ");
			writer.write(mappedName);
			writer.newLine();
		}

		writer.newLine();
	}
}
