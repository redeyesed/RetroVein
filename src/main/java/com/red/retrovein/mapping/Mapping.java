package com.red.retrovein.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Mapping {
	private final Map<String, String> classes;

	public Mapping(Map<String, String> classes) {
		this.classes = Collections.unmodifiableMap(new HashMap<String, String>(classes));
	}

	public String getClassName(String originalName) {
		String mappedName = classes.get(originalName);

		return mappedName != null ? mappedName : originalName;
	}

	public Map<String, String> getClasses() {
		return classes;
	}
}
