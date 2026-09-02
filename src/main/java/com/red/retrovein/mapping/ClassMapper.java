package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.RetroLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassMapper {
	private final NameGenerator nameGenerator;

	public ClassMapper() {
		this.nameGenerator = new NameGenerator();
	}

	public Map<String, String> build(List<ClassInfo> classInfos) {
		Map<String, String> mappings = new HashMap<String, String>();

		for (ClassInfo classInfo : classInfos) {

			String originalName = classInfo.getName();

			String mappedName = nameGenerator.next();

			mappings.put(originalName, mappedName);

			RetroLogger.debug("Class mapping: {} -> {}", originalName, mappedName);
		}

		RetroLogger.debug("Generated {} class mappings", mappings.size());

		return mappings;
	}
}
