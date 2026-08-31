package com.red.retrovein.mapping;

import java.util.Map;

public final class Mapping {
	private final Map<String, String> classes;
	private final Map<String, String> methods;
	private final Map<String, String> fields;

	public Mapping(Map<String, String> classes, Map<String, String> methods, Map<String, String> fields) {
		this.classes = classes;
		this.methods = methods;
		this.fields = fields;
	}

	public String getClassName(String className) {
		String mappedName = classes.get(className);

		return mappedName != null ? mappedName : className;
	}

	public String getMethodName(String owner, String name, String descriptor) {
		String key = owner + "." + name + descriptor;

		String mappedName = methods.get(key);

		return mappedName != null ? mappedName : name;
	}

	public String getFieldName(String owner, String name, String descriptor) {
		String key = owner + "." + name + ":" + descriptor;

		String mappedName = fields.get(key);

		return mappedName != null ? mappedName : name;
	}

	public Map<String, String> getClasses() {
		return classes;
	}

	public Map<String, String> getMethods() {
		return methods;
	}

	public Map<String, String> getFields() {
		return fields;
	}
}