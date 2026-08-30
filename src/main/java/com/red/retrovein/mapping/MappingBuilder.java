package com.red.retrovein.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MappingBuilder {
	private final NameGenerator nameGenerator;

	public MappingBuilder() {
		this.nameGenerator = new NameGenerator();
	}

	public Mapping build(List<String> classNames) {
		Map<String, String> classes = new HashMap<String, String>();

		List<String> sortedClassNames = new ArrayList<String>(classNames);

		Collections.sort(sortedClassNames);

		for (String className : classNames) {
			classes.put(className, nameGenerator.next());
		}

		return new Mapping(classes);
	}
}
