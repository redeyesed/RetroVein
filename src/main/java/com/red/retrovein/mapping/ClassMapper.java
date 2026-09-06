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

		String rootPackage = findRootPackage(classInfos);

		for (ClassInfo classInfo : classInfos) {

			String originalName = classInfo.getName();

			String mappedName = rootPackage + "/" + nameGenerator.next();

			mappings.put(originalName, mappedName);

			RetroLogger.debug("Class mapping: {} -> {}", originalName, mappedName);
		}

		RetroLogger.debug("Generated {} class mappings in package {}", mappings.size(), rootPackage);

		return mappings;
	}

	/*
	 * Определяет общий корневой пакет всех классов.
	 */
	private String findRootPackage(List<ClassInfo> classInfos) {
		String rootPackage = null;

		for (ClassInfo classInfo : classInfos) {

			String className = classInfo.getName();

			int lastSlash = className.lastIndexOf('/');

			if (lastSlash <= 0) {
				continue;
			}

			String packageName = className.substring(0, lastSlash);

			if (rootPackage == null) {

				rootPackage = packageName;

				continue;
			}

			rootPackage = findCommonPackage(rootPackage, packageName);

			if (rootPackage.isEmpty()) {
				break;
			}
		}

		return rootPackage == null ? "" : rootPackage;
	}

	/*
	 * Находит общий пакет двух указанных пакетов.
	 */
	private String findCommonPackage(String first, String second) {
		String[] firstParts = first.split("/");
		String[] secondParts = second.split("/");

		int length = Math.min(firstParts.length, secondParts.length);

		int commonLength = 0;

		for (int i = 0; i < length; i++) {

			if (!firstParts[i].equals(secondParts[i])) {
				break;
			}

			commonLength++;
		}

		if (commonLength == 0) {
			return "";
		}

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < commonLength; i++) {

			if (i > 0) {
				result.append('/');
			}

			result.append(firstParts[i]);
		}

		return result.toString();
	}
}
