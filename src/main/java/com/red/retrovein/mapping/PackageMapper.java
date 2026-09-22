package com.red.retrovein.mapping;

import java.util.List;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;

public final class PackageMapper {

	/*
	 * Извлекает имя пакета из полного внутреннего имени класса. Если класс
	 * находится в корневом пакете, возвращается пустая строка.
	 */
	public String getPackage(String className) {
		if (className == null || className.isEmpty()) {
			return "";
		}
		int lastSlash = className.lastIndexOf('/');
		if (lastSlash <= 0) {
			return "";
		}
		return className.substring(0, lastSlash);
	}

	/*
	 * Определяет общий корневой пакет для всех классов проекта. Полученный пакет
	 * используется ClassMapper при создании новых имён классов.
	 */
	public String findRootPackage(List<ClassInfo> classInfos) {
		String rootPackage = null;
		if (classInfos == null || classInfos.isEmpty()) {
			return "";
		}
		for (ClassInfo classInfo : classInfos) {
			if (classInfo == null) {
				continue;
			}
			String packageName = getPackage(classInfo.getName());

			if (packageName.isEmpty()) {
				continue;
			}
			if (rootPackage == null) {
				rootPackage = packageName;
				continue;
			}
			rootPackage = findCommonPackage(rootPackage, packageName);
			if (rootPackage.isEmpty()) {
				break;
			}
		}
		String result = rootPackage == null ? "" : rootPackage;
		RetroLogger.debug(LogCategory.Mapping, "Detected root package: {}", result.isEmpty() ? "<default>" : result);
		return result;
	}

	/*
	 * Преобразует имя пакета перед его использованием в mapping. На текущем этапе
	 * пакеты сохраняются без изменений.
	 */
	public String mapPackage(String packageName) {
		return packageName == null ? "" : packageName;
	}

	/*
	 * Находит общую часть двух имён пакетов. Если общих компонентов нет,
	 * возвращается пустая строка.
	 */
	private String findCommonPackage(String first, String second) {
		String[] firstParts = first.split("/");
		String[] secondParts = second.split("/");
		int length = Math.min(firstParts.length, secondParts.length);
		StringBuilder common = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (!firstParts[i].equals(secondParts[i])) {
				break;
			}
			if (common.length() > 0) {
				common.append('/');
			}
			common.append(firstParts[i]);
		}
		return common.toString();
	}
}
