package com.red.retrovein.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Mapping {
	private final Map<String, String> classes;
	private final Map<String, String> fields;
	private final Map<String, String> methods;
	private final Map<String, String> localVariables;
	private final Map<String, String> enums;

	/*
	 * Инициализирует все типы и оборачивает их в неизменяемые Map, исключая
	 * возможность модификации состояния извне.
	 */
	public Mapping(Map<String, String> classes, Map<String, String> fields, Map<String, String> methods,
			Map<String, String> localVariables, Map<String, String> enums) {
		this.classes = Collections.unmodifiableMap(new HashMap<String, String>(classes));
		this.fields = Collections.unmodifiableMap(new HashMap<String, String>(fields));
		this.methods = Collections.unmodifiableMap(new HashMap<String, String>(methods));
		this.localVariables = Collections.unmodifiableMap(new HashMap<String, String>(localVariables));
		this.enums = Collections.unmodifiableMap(new HashMap<String, String>(enums));
	}

	public String getClassName(String originalName) {
		String mappedName = this.classes.get(originalName);
		return mappedName != null ? mappedName : originalName;
	}

	public String getFieldName(String owner, String name, String descriptor) {
		String key = owner + "." + name + ":" + descriptor;
		String mappedName = this.fields.get(key);
		return mappedName != null ? mappedName : name;
	}

	public String getMethodName(String owner, String name, String descriptor) {
		String key = owner + "." + name + descriptor;
		String mappedName = this.methods.get(key);
		return mappedName != null ? mappedName : name;
	}

	public String getLocalVariableName(String owner, String methodName, String methodDescriptor, int index) {
		String key = owner + "." + methodName + methodDescriptor + "#" + index;
		return this.localVariables.get(key);
	}

	public String getEnumName(String owner, String name, String descriptor) {
		String key = EnumMapper.createKey(owner, name, descriptor);
		String mappedName = this.enums.get(key);
		return mappedName != null ? mappedName : name;
	}

	public Map<String, String> getClasses() {
		return this.classes;
	}

	public Map<String, String> getFields() {
		return this.fields;
	}

	public Map<String, String> getMethods() {
		return this.methods;
	}

	public Map<String, String> getLocalVariables() {
		return this.localVariables;
	}

	public Map<String, String> getEnums() {
		return this.enums;
	}
}
