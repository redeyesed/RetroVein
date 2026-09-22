package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.naming.NameGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassMapper {
	private final NameGenerator nameGenerator;
	private final PackageMapper packageMapper;

	public ClassMapper() {
		this.nameGenerator = new NameGenerator();
		this.packageMapper = new PackageMapper();
	}

	public Map<String, String> build(List<ClassInfo> classInfos) {
		Map<String, String> mappings = new HashMap<String, String>();

		/*
		 * Создаёт отображение исходных имён классов на новые имена.
		 */
		String rootPackage = packageMapper.findRootPackage(classInfos);
		String mappedPackage = packageMapper.mapPackage(rootPackage);

		for (ClassInfo classInfo : classInfos) {
			String originalName = classInfo.getName();
			String mappedName;

			if (mappedPackage.isEmpty()) {
				mappedName = nameGenerator.nextClass();
			} else {
				mappedName = mappedPackage + "/" + nameGenerator.nextClass();
			}

			mappings.put(originalName, mappedName);
			RetroLogger.debug(LogCategory.Mapping, "Class mapping: {} -> {}", originalName, mappedName);
		}

		RetroLogger.debug(LogCategory.Mapping, "Generated {} class mappings in package {}", mappings.size(),
				mappedPackage.isEmpty() ? "<default>" : mappedPackage);
		return mappings;
	}
}
