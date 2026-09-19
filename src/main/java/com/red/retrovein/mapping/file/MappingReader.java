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
	private static final int FORMAT_VERSION = 2;

	/**
	 * Читает таблицу соответствий из файла RVM.
	 */
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
		Map<String, String> enums = new HashMap<String, String>();

		String currentClass = null;

		boolean headerRead = false;
		boolean versionRead = false;

		try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}

				if (line.startsWith("#")) {
					if (HEADER.equals(line.trim())) {
						headerRead = true;
					}
					continue;
				}

				if (line.startsWith("version=")) {
					readVersion(line);
					versionRead = true;
					continue;
				}

				if (line.startsWith("\t")) {
					if (currentClass == null) {
						throw new IOException("Mapping entry found outside of a class: " + line);
					}

					readClassEntry(line.substring(1), currentClass, fields, methods, localVariables, enums);

					continue;
				}

				currentClass = readClass(line, classes);
			}
		}

		if (!headerRead) {
			throw new IOException("Invalid mapping file: missing header");
		}

		if (!versionRead) {
			throw new IOException("Invalid mapping file: missing version");
		}

		return new Mapping(classes, methods, fields, localVariables, enums);
	}

	private void readVersion(String line) throws IOException {
		String value = line.substring("version=".length()).trim();

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

	/**
	 * Читает объявление класса и возвращает его исходное имя, которое используется
	 * как владелец последующих записей.
	 */
	private String readClass(String line, Map<String, String> classes) throws IOException {
		String content = line.trim();

		if (!content.endsWith(":")) {
			throw new IOException("Invalid class mapping: " + line);
		}

		content = content.substring(0, content.length() - 1).trim();

		int separator = content.indexOf(" -> ");

		if (separator == -1) {
			throw new IOException("Invalid class mapping: " + line);
		}

		String original = content.substring(0, separator).trim();
		String mapped = content.substring(separator + " -> ".length()).trim();

		if (original.isEmpty()) {
			throw new IOException("Empty original class name: " + line);
		}

		if (mapped.isEmpty()) {
			throw new IOException("Empty mapped class name: " + line);
		}

		if (classes.containsKey(original)) {
			throw new IOException("Duplicate class mapping: " + original);
		}

		classes.put(original, mapped);

		return original;
	}

	/**
	 * Определяет тип записи внутри класса по её структуре. Поля содержат двоеточие
	 * между именем и дескриптором, а методы начинаются с имени и дескриптора
	 * метода.
	 */
	private void readClassEntry(String line, String owner, Map<String, String> fields, Map<String, String> methods,
			Map<String, String> localVariables, Map<String, String> enums) throws IOException {
		int separator = line.indexOf(" -> ");

		if (separator == -1) {
			throw new IOException("Invalid class entry: " + line);
		}

		String original = line.substring(0, separator).trim();
		String mappedPart = line.substring(separator + " -> ".length()).trim();

		if (original.isEmpty()) {
			throw new IOException("Empty original member name: " + line);
		}

		if (original.indexOf(':') >= 0) {
			readField(owner, original, mappedPart, fields, enums);
			return;
		}

		if (original.indexOf('(') >= 0) {
			readMethod(owner, original, mappedPart, methods, localVariables);
			return;
		}
		throw new IOException("Unknown class entry: " + line);
	}

	private void readField(String owner, String original, String mapped, Map<String, String> fields,
			Map<String, String> enums) throws IOException {
		int separator = original.indexOf(' ');

		if (separator <= 0 || separator == original.length() - 1) {
			throw new IOException("Invalid field mapping: " + original + " -> " + mapped);
		}

		String descriptor = original.substring(0, separator).trim();
		String name = original.substring(separator + 1).trim();

		if (descriptor.isEmpty() || name.isEmpty()) {
			throw new IOException("Invalid field mapping: " + original + " -> " + mapped);
		}

		if (mapped.isEmpty()) {
			throw new IOException("Empty mapped field name: " + original);
		}

		String key = owner + "." + name + ":" + descriptor;

		/*
		 * Enum-константы используют тот же формат ключа, что и обычные поля. Поэтому
		 * без дополнительного признака в файле их нельзя однозначно отличить друг от
		 * друга. Сначала сохраняем запись как обычное поле.
		 */
		if (fields.containsKey(key)) {
			throw new IOException("Duplicate field mapping: " + key);
		}
		fields.put(key, mapped);
	}

	private void readMethod(String owner, String original, String mappedPart, Map<String, String> methods,
			Map<String, String> localVariables) throws IOException {
		int separator = original.indexOf('(');

		if (separator <= 0) {
			throw new IOException("Invalid method mapping: " + original + " -> " + mappedPart);
		}

		String name = original.substring(0, separator);
		String descriptor = original.substring(separator);

		int localsStart = mappedPart.indexOf(" (");

		String mappedName;

		if (localsStart >= 0) {
			mappedName = mappedPart.substring(0, localsStart).trim();
		} else {
			mappedName = mappedPart.trim();
		}

		if (mappedName.isEmpty()) {
			throw new IOException("Empty mapped method name: " + original);
		}

		String key = owner + "." + name + descriptor;

		if (methods.containsKey(key)) {
			throw new IOException("Duplicate method mapping: " + key);
		}

		methods.put(key, mappedName);

		if (localsStart >= 0) {
			String locals = mappedPart.substring(localsStart).trim();
			readLocalVariables(owner, name, descriptor, locals, localVariables);
		}
	}

	/**
	 * Читает список параметров и локальных переменных метода. Параметры находятся
	 * до ; обычные локальные переменные после него. Индексы параметров
	 * восстанавливаются последовательно.
	 */
	private void readLocalVariables(String owner, String methodName, String descriptor, String locals,
			Map<String, String> localVariables) throws IOException {
		if (!locals.startsWith("(") || !locals.endsWith(")")) {
			throw new IOException("Invalid local variable mapping: " + locals);
		}

		String content = locals.substring(1, locals.length() - 1).trim();

		if (content.isEmpty()) {
			return;
		}

		String parametersPart = content;
		String variablesPart = "";

		int separator = content.indexOf(';');

		if (separator >= 0) {
			parametersPart = content.substring(0, separator).trim();
			variablesPart = content.substring(separator + 1).trim();
		}

		int parameterIndex = 1;

		if (!parametersPart.isEmpty()) {
			String[] parameters = parametersPart.split(",");

			for (String parameter : parameters) {
				String name = parameter.trim();

				if (name.isEmpty()) {
					continue;
				}

				this.putLocalVariable(owner, methodName, descriptor, parameterIndex, name, localVariables);

				parameterIndex++;
			}
		}

		int variableIndex = parameterIndex;

		if (!variablesPart.isEmpty()) {
			String[] variables = variablesPart.split(",");

			for (String variable : variables) {
				String name = variable.trim();

				if (name.isEmpty()) {
					continue;
				}

				this.putLocalVariable(owner, methodName, descriptor, variableIndex, name, localVariables);

				variableIndex++;
			}
		}
	}

	private void putLocalVariable(String owner, String methodName, String descriptor, int index, String mappedName,
			Map<String, String> localVariables) throws IOException {
		String key = owner + "." + methodName + descriptor + "#" + index;

		if (localVariables.containsKey(key)) {
			throw new IOException("Duplicate local variable mapping: " + key);
		}

		localVariables.put(key, mappedName);
	}
}
