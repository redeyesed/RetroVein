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
		if (classInfos == null || classInfos.isEmpty()) {
			return "";
		}
		String rootPackage = "";
		for (ClassInfo classInfo : classInfos) {
			if (classInfo == null) {
				continue;
			}
			String packageName = getPackage(classInfo.getName());
			if (packageName.isEmpty()) {
				continue;
			}
			if (rootPackage.isEmpty()) {
				rootPackage = packageName;
				continue;
			}
			rootPackage = findCommonPackage(rootPackage, packageName);
			if (rootPackage.isEmpty()) {
				break;
			}
		}
		RetroLogger.debug(LogCategory.Mapping, "Detected root package: {}",
				rootPackage.isEmpty() ? "<default>" : rootPackage);
		return rootPackage;
	}

	/*
	 * Преобразует имя пакета перед его использованием в mapping. На текущем этапе
	 * пакеты сохраняются без изменений.
	 */
	public String mapPackage(String packageName) {
		return packageName == null ? "" : packageName;
	}

	/*
	 * Возвращает родительский пакет для указанного имени. Если родительского пакета
	 * нет, возвращается пустая строка.
	 */
	public String getParentPackage(String packageName) {
		if (packageName == null || packageName.isEmpty()) {
			return "";
		}
		int lastSlash = packageName.lastIndexOf('/');
		if (lastSlash <= 0) {
			return "";
		}
		return packageName.substring(0, lastSlash);
	}

	/*
	 * Проверяет, является ли второй пакет дочерним по отношению к первому. Сами
	 * пакеты считаются разными, поэтому одинаковые имена возвращают false.
	 */
	public boolean isSubPackage(String parent, String child) {
		if (parent == null || parent.isEmpty() || child == null || child.isEmpty() || parent.equals(child)) {
			return false;
		}
		return child.startsWith(parent + "/");
	}

	/*
	 * Возвращает количество компонентов в имени пакета. Для корневого пакета
	 * возвращается нулевая глубина.
	 */
	public int getDepth(String packageName) {
		if (packageName == null || packageName.isEmpty()) {
			return 0;
		}
		int depth = 1;
		for (int i = 0; i < packageName.length(); i++) {
			if (packageName.charAt(i) == '/') {
				depth++;
			}
		}
		return depth;
	}

	/*
	 * Проверяет корректность имени пакета во внутреннем формате JVM. Каждый
	 * компонент должен содержать только допустимые символы идентификатора.
	 */
	public boolean isValidPackage(String packageName) {
		if (packageName == null || packageName.isEmpty()) {
			return false;
		}
		String[] parts = packageName.split("/");
		for (String part : parts) {
			if (part.isEmpty() || !isValidPackagePart(part)) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidPackagePart(String part) {
		if (!Character.isJavaIdentifierStart(part.charAt(0))) {
			return false;
		}
		for (int i = 1; i < part.length(); i++) {
			if (!Character.isJavaIdentifierPart(part.charAt(i))) {
				return false;
			}
		}
		return true;
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
