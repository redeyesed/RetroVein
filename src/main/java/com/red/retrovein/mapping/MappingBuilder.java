package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MappingBuilder {
	private final ClassMapper classMapper;
	private final FieldMapper fieldMapper;
	private final MethodMapper methodMapper;
	private final LocalVariableMapper localVariableMapper;
	private final EnumMapper enumMapper;

	public MappingBuilder() {
		this.classMapper = new ClassMapper();
		this.fieldMapper = new FieldMapper();
		this.methodMapper = new MethodMapper();
		this.localVariableMapper = new LocalVariableMapper();
		this.enumMapper = new EnumMapper();
	}

	public Mapping build(List<ClassInfo> classInfos) {
		RetroLogger.debug(LogCategory.Mapping, "Preparing {} classes for mapping", classInfos.size());
		List<ClassInfo> sortedClasses = sortClasses(classInfos);
		RetroLogger.debug(LogCategory.Mapping, "Classes sorted alphabetically");
		/*
		 * Classes.
		 */
		RetroLogger.debug(LogCategory.Mapping, "Generating class mappings");
		Map<String, String> classes = classMapper.build(sortedClasses);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} class mappings", classes.size());

		/*
		 * Metadata.
		 */
		RetroLogger.debug(LogCategory.Mapping, "Generating class metadata");
		Map<String, ClassMetadata> metadata = methodMapper.buildMetadata(sortedClasses);
		RetroLogger.debug(LogCategory.Mapping, "Generated metadata for {} classes", metadata.size());

		/*
		 * Fields.
		 */
		RetroLogger.debug(LogCategory.Mapping, "Generating field mappings");
		Map<String, String> fields = fieldMapper.build(sortedClasses);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} field mappings", fields.size());

		/*
		 * Methods.
		 */
		RetroLogger.debug(LogCategory.Mapping, "Generating method mappings");
		List<ClassInfo> methodClasses = sortByInheritance(sortedClasses, metadata);
		Map<String, String> methods = methodMapper.build(methodClasses, metadata);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} method mappings", methods.size());
		/*
		 * Local variables.
		 */
		RetroLogger.debug(LogCategory.Mapping, "Generating local variable mappings");
		Map<String, String> localVariables = localVariableMapper.build(sortedClasses);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} local variable mappings", localVariables.size());

		/*
		 * Enum constants.
		 */
		RetroLogger.debug(LogCategory.Mapping, "Generating enum mappings");
		Map<String, String> enums = enumMapper.build(sortedClasses);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} enum mappings", enums.size());

		RetroLogger.info(LogCategory.Mapping,
				"Generated mappings: {} classes, {} fields, {} methods, {} variables, {} enum constants",
				classes.size(), fields.size(), methods.size(), localVariables.size(), enums.size());

		return new Mapping(classes, methods, fields, localVariables, enums);
	}

	private List<ClassInfo> sortClasses(List<ClassInfo> classInfos) {

		List<ClassInfo> sorted = new ArrayList<ClassInfo>(classInfos);

		Collections.sort(sorted, new Comparator<ClassInfo>() {
			@Override
			public int compare(ClassInfo first, ClassInfo second) {

				return first.getName().compareTo(second.getName());
			}
		});

		return sorted;
	}

	/**
	 * Сортирует классы так, чтобы родительские классы и интерфейсы обрабатывались
	 * раньше классов, которые от них наследуются.
	 *
	 * Это необходимо для корректного переиспользования имён переопределённых
	 * методов.
	 */
	private List<ClassInfo> sortByInheritance(List<ClassInfo> sortedClasses, Map<String, ClassMetadata> metadata) {
		Map<String, ClassInfo> classesByName = new HashMap<String, ClassInfo>();

		for (ClassInfo classInfo : sortedClasses) {
			classesByName.put(classInfo.getName(), classInfo);
		}

		List<ClassInfo> result = new ArrayList<ClassInfo>();
		Set<String> visited = new HashSet<String>();
		Set<String> visiting = new HashSet<String>();

		for (ClassInfo classInfo : sortedClasses) {
			visitClass(classInfo.getName(), classesByName, metadata, visited, visiting, result);
		}

		return result;
	}

	/**
	 * Рекурсивно добавляет родительские классы и интерфейсы перед текущим классом.
	 */
	private void visitClass(String className, Map<String, ClassInfo> classesByName, Map<String, ClassMetadata> metadata,
			Set<String> visited, Set<String> visiting, List<ClassInfo> result) {
		if (visited.contains(className)) {
			return;
		}

		/*
		 * Корректная Java-иерархия не должна содержать циклов. Проверка защищает от
		 * бесконечной рекурсии на повреждённом bytecode.
		 */
		if (!visiting.add(className)) {
			RetroLogger.warn(LogCategory.Mapping, "Inheritance cycle detected involving {}", className);
			return;
		}

		ClassMetadata classMetadata = metadata.get(className);

		if (classMetadata != null) {
			String superName = classMetadata.getSuperName();

			/*
			 * Сначала обрабатываем родительский класс.
			 */
			if (superName != null && classesByName.containsKey(superName)) {
				visitClass(superName, classesByName, metadata, visited, visiting, result);
			}

			/*
			 * Интерфейсы обрабатываем в алфавитном порядке, чтобы результат не зависел от
			 * порядка их объявления.
			 */
			List<String> interfaces = new ArrayList<String>(classMetadata.getInterfaces());

			Collections.sort(interfaces);

			for (String interfaceName : interfaces) {
				if (classesByName.containsKey(interfaceName)) {
					visitClass(interfaceName, classesByName, metadata, visited, visiting, result);
				}
			}
		}

		visiting.remove(className);
		visited.add(className);

		ClassInfo classInfo = classesByName.get(className);

		if (classInfo != null) {
			result.add(classInfo);
		}
	}
}
