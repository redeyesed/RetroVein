package com.red.retrovein.mapping;

import java.util.Map;

public final class Mapping {
	private final Map<String, String> classes;
	private final Map<String, String> methods;
	private final Map<String, String> fields;
	private final Map<String, String> localVariables;

	public Mapping(Map<String, String> classes, Map<String, String> methods, Map<String, String> fields,
			Map<String, String> localVariables) {

		this.classes = classes;
		this.methods = methods;
		this.fields = fields;
		this.localVariables = localVariables;
	}

	public String getClassName(String originalName) {
		String mappedName = classes.get(originalName);

		return mappedName != null ? mappedName : originalName;
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

	public String getLocalVariableName(String owner, String methodName, String methodDescriptor, int index) {

		String key = owner + "." + methodName + methodDescriptor + "#" + index;

		String mappedName = localVariables.get(key);

		return mappedName != null ? mappedName : null;
	}
}
