package com.red.retrovein.mapping.file;

import com.red.retrovein.mapping.Mapping;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MappingWriter {
	private static final String HEADER = "# RetroVein Mapping";
	private static final int FORMAT_VERSION = 2;

	/**
	 * Записывает таблицу соответствий в указанный файл.
	 */
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
			writeClasses(writer, mapping);
		}
	}

	/**
	 * Записывает заголовок файла.
	 */
	private void writeHeader(BufferedWriter writer) throws IOException {
		writer.write(HEADER);
		writer.newLine();
		writer.write("version=");
		writer.write(Integer.toString(FORMAT_VERSION));
		writer.newLine();
		writer.newLine();
	}

	/**
	 * Записывает классы в порядке их преобразованных имён. Порядок определяется
	 * именами, которые были назначены генератором.
	 */
	private void writeClasses(BufferedWriter writer, Mapping mapping) throws IOException {
		List<ClassEntry> classes = new ArrayList<ClassEntry>();

		for (Map.Entry<String, String> entry : mapping.getClasses().entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}

			classes.add(new ClassEntry(entry.getKey(), entry.getValue()));
		}

		Collections.sort(classes, new Comparator<ClassEntry>() {
			@Override
			public int compare(ClassEntry first, ClassEntry second) {
				return compareMappedNames(first.mappedName, second.mappedName);
			}
		});

		for (ClassEntry classEntry : classes) {
			writeClass(writer, classEntry.originalName, classEntry.mappedName, mapping);
		}
	}

	/**
	 * Записывает класс и связанные с ним поля, enum-константы и методы.
	 */
	private void writeClass(BufferedWriter writer, String originalClass, String mappedClass, Mapping mapping)
			throws IOException {
		writer.write(originalClass);
		writer.write(" -> ");
		writer.write(mappedClass);
		writer.write(":");
		writer.newLine();

		this.writeFields(writer, originalClass, mapping);
		this.writeEnums(writer, originalClass, mapping);
		this.writeMethods(writer, originalClass, mapping);

		writer.newLine();
	}

	/**
	 * Записывает поля в порядке назначенных имён.
	 */
	private void writeFields(BufferedWriter writer, String owner, Mapping mapping) throws IOException {
		List<FieldEntry> fields = collectFields(owner, mapping.getFields());

		Collections.sort(fields, new Comparator<FieldEntry>() {
			@Override
			public int compare(FieldEntry first, FieldEntry second) {
				int result = first.name.compareToIgnoreCase(second.name);
				if (result != 0) {
					return result;
				}
				return first.descriptor.compareTo(second.descriptor);
			}
		});


		for (FieldEntry field : fields) {
			writeField(writer, field);
		}
	}

	/**
	 * Записывает enum-константы в порядке назначенных имён.
	 */
	private void writeEnums(BufferedWriter writer, String owner, Mapping mapping) throws IOException {
		List<FieldEntry> enums = collectFields(owner, mapping.getEnums());

		Collections.sort(enums, new Comparator<FieldEntry>() {
			@Override
			public int compare(FieldEntry first, FieldEntry second) {
				int result = first.name.compareToIgnoreCase(second.name);
				if (result != 0) {
					return result;
				}
				return first.descriptor.compareTo(second.descriptor);
			}
		});


		for (FieldEntry field : enums) {
			writeField(writer, field);
		}
	}

	private void writeField(BufferedWriter writer, FieldEntry field) throws IOException {
		writer.write("    ");
		writer.write(field.descriptor);
		writer.write(" ");
		writer.write(field.name);
		writer.write(" -> ");
		writer.write(field.mappedName);
		writer.newLine();
	}

	/**
	 * Записывает методы в порядке назначенных имён. После имени метода записываются
	 * его параметры и локальные переменные, если соответствующая информация
	 * присутствует в таблице mapping.
	 */
	private void writeMethods(BufferedWriter writer, String owner, Mapping mapping) throws IOException {
		List<MethodEntry> methods = collectMethods(owner, mapping.getMethods());

		Collections.sort(methods, new Comparator<MethodEntry>() {
			@Override
			public int compare(MethodEntry first, MethodEntry second) {
				int result = first.name.compareToIgnoreCase(second.name);
				if (result != 0) {
					return result;
				}
				return first.descriptor.compareTo(second.descriptor);
			}
		});


		for (MethodEntry method : methods) {
			writer.write("    ");
			writer.write(method.name);
			writer.write(method.descriptor);
			writer.write(" -> ");
			writer.write(method.mappedName);

			String locals = buildLocalVariables(owner, method.name, method.descriptor, mapping.getLocalVariables());

			if (!locals.isEmpty()) {
				writer.write(" ");
				writer.write(locals);
			}

			writer.newLine();
		}
	}

	/**
	 * Формирует список параметров и локальных переменных метода. Параметры и
	 * обычные локальные переменные разделяются точкой с запятой: (par1, par2; var1,
	 * var2).
	 */
	private String buildLocalVariables(String owner, String methodName, String descriptor,
			Map<String, String> localVariables) {
		List<LocalEntry> parameters = new ArrayList<LocalEntry>();
		List<LocalEntry> variables = new ArrayList<LocalEntry>();

		String prefix = owner + "." + methodName + descriptor + "#";

		for (Map.Entry<String, String> entry : localVariables.entrySet()) {
			if (!entry.getKey().startsWith(prefix)) {
				continue;
			}

			String indexString = entry.getKey().substring(prefix.length());

			try {
				int index = Integer.parseInt(indexString);
				String mappedName = entry.getValue();

				if (mappedName == null) {
					continue;
				}

				if (mappedName.startsWith("par")) {
					parameters.add(new LocalEntry(index, mappedName));
				} else if (mappedName.startsWith("var")) {
					variables.add(new LocalEntry(index, mappedName));
				}
			} catch (NumberFormatException ignored) {
				// Некорректная запись не должна прерывать запись всего mapping-файла.
			}
		}

		Collections.sort(parameters, LOCAL_COMPARATOR);
		Collections.sort(variables, LOCAL_COMPARATOR);

		if (parameters.isEmpty() && variables.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		result.append("(");

		writeLocalList(result, parameters);

		if (!variables.isEmpty()) {
			if (!parameters.isEmpty()) {
				result.append("; ");
			}
			writeLocalList(result, variables);
		}

		result.append(")");

		return result.toString();
	}

	/*
	 * Записывает список локальных переменных в сокращённом виде. Последовательные
	 * переменные с одинаковым префиксом объединяются в диапазон через "..".
	 */
	private void writeLocalList(StringBuilder result, List<LocalEntry> locals) {
		if (locals.isEmpty()) {
			return;
		}
		int start = 0;
		while (start < locals.size()) {
			int end = start;
			String firstName = locals.get(start).mappedName;
			String prefix = getLocalPrefix(firstName);
			int previousNumber = getLocalNumber(firstName);
			while (end + 1 < locals.size()) {
				String nextName = locals.get(end + 1).mappedName;
				if (!prefix.equals(getLocalPrefix(nextName))) {
					break;
				}
				int nextNumber = getLocalNumber(nextName);
				if (nextNumber != previousNumber + 1) {
					break;
				}
				end++;
				previousNumber = nextNumber;
			}
			if (start > 0) {
				result.append(", ");
			}
			String lastName = locals.get(end).mappedName;
			if (start != end && end - start >= 1) {
				result.append(firstName);
				result.append("..");
				result.append(lastName);
			} else {
				result.append(firstName);
			}
			start = end + 1;
		}
	}

	private static String getLocalPrefix(String name) {
		if (name == null || name.length() < 4) {
			return "";
		}
		return name.substring(0, 3);
	}

	private List<FieldEntry> collectFields(String owner, Map<String, String> mappings) {
		List<FieldEntry> fields = new ArrayList<FieldEntry>();

		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			FieldEntry field = parseField(entry.getKey(), entry.getValue());

			if (field != null && owner.equals(field.owner)) {
				fields.add(field);
			}
		}
		return fields;
	}

	private List<MethodEntry> collectMethods(String owner, Map<String, String> mappings) {
		List<MethodEntry> methods = new ArrayList<MethodEntry>();

		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			MethodEntry method = parseMethod(entry.getKey(), entry.getValue());

			if (method != null && owner.equals(method.owner)) {
				methods.add(method);
			}
		}
		return methods;
	}

	/**
	 * Разбирает внутреннее представление поля на владельца, имя поля и дескриптор
	 * типа.
	 */
	private FieldEntry parseField(String key, String mappedName) {
		int separator = key.lastIndexOf(':');

		if (separator <= 0 || separator == key.length() - 1) {
			return null;
		}

		int ownerSeparator = key.lastIndexOf('.', separator);

		if (ownerSeparator <= 0 || ownerSeparator == separator - 1) {
			return null;
		}

		String owner = key.substring(0, ownerSeparator);
		String name = key.substring(ownerSeparator + 1, separator);
		String descriptor = key.substring(separator + 1);

		return new FieldEntry(owner, name, descriptor, mappedName);
	}

	/**
	 * Разбирает внутреннее представление метода на владельца, имя метода и
	 * дескриптор.
	 */
	private MethodEntry parseMethod(String key, String mappedName) {
		int descriptorStart = key.indexOf('(');

		if (descriptorStart <= 0) {
			return null;
		}

		int ownerSeparator = key.lastIndexOf('.', descriptorStart);

		if (ownerSeparator <= 0 || ownerSeparator == descriptorStart - 1) {
			return null;
		}

		String owner = key.substring(0, ownerSeparator);
		String name = key.substring(ownerSeparator + 1, descriptorStart);
		String descriptor = key.substring(descriptorStart);

		return new MethodEntry(owner, name, descriptor, mappedName);
	}

	/**
	 * Сравнивает преобразованные имена с учётом последовательности, которую
	 * использует генератор имён.
	 */
	private int compareMappedNames(String first, String second) {
		String firstName = getSimpleMappedName(first);
		String secondName = getSimpleMappedName(second);

		if (firstName.length() != secondName.length()) {
			return Integer.compare(firstName.length(), secondName.length());
		}
		return firstName.compareTo(secondName);
	}

	private String getSimpleMappedName(String mappedName) {
		int separator = mappedName.lastIndexOf('/');
		return separator >= 0 ? mappedName.substring(separator + 1) : mappedName;
	}

	/**
	 * Сравнивает локальные переменные по их порядковому номеру. Например: (var1,
	 * var2, var3..) всегда будут записаны именно в таком порядке независимо от
	 * индекса локальной переменной.
	 */
	private static int compareLocalNames(String first, String second) {
		return Integer.compare(getLocalNumber(first), getLocalNumber(second));
	}

	private static int getLocalNumber(String name) {
		try {
			return Integer.parseInt(name.substring(3));
		} catch (NumberFormatException ignored) {
			return Integer.MAX_VALUE;
		}
	}

	private static final Comparator<LocalEntry> LOCAL_COMPARATOR = new Comparator<LocalEntry>() {
		@Override
		public int compare(LocalEntry first, LocalEntry second) {
			return compareLocalNames(first.mappedName, second.mappedName);
		}
	};

	private static final class ClassEntry {
		private final String originalName;
		private final String mappedName;

		private ClassEntry(String originalName, String mappedName) {
			this.originalName = originalName;
			this.mappedName = mappedName;
		}
	}

	private static final class FieldEntry {
		private final String owner;
		private final String name;
		private final String descriptor;
		private final String mappedName;

		private FieldEntry(String owner, String name, String descriptor, String mappedName) {
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.mappedName = mappedName;
		}
	}

	private static final class MethodEntry {
		private final String owner;
		private final String name;
		private final String descriptor;
		private final String mappedName;

		private MethodEntry(String owner, String name, String descriptor, String mappedName) {
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.mappedName = mappedName;
		}
	}

	private static final class LocalEntry {
		private final int index;
		private final String mappedName;

		private LocalEntry(int index, String mappedName) {
			this.index = index;
			this.mappedName = mappedName;
		}
	}
}
