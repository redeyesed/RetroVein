package com.red.retrovein.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;

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

	/*
	 * Строит полный mapping для переданного набора классов. Сначала
	 * подготавливаются классы и metadata, после чего создаются все остальные.
	 */
	public Mapping build(List<ClassInfo> classInfos) {
		RetroLogger.debug(LogCategory.Mapping, "Preparing {} classes for mapping", classInfos.size());
		List<ClassInfo> sortedClasses = this.sortClasses(classInfos);
		Map<String, String> classes = this.buildClassMappings(sortedClasses);
		Map<String, ClassMetadata> metadata = this.buildMetadata(sortedClasses);
		Map<String, String> fields = this.buildFieldMappings(sortedClasses);
		Map<String, String> methods = this.buildMethodMappings(sortedClasses, metadata);
		Map<String, String> localVariables = this.buildLocalVariableMappings(sortedClasses);
		Map<String, String> enums = this.buildEnumMappings(sortedClasses);

		this.logMappingSummary(classes, fields, methods, localVariables, enums);
		return new Mapping(classes, fields, methods, localVariables, enums);
	}

	private Map<String, String> buildClassMappings(List<ClassInfo> classes) {
		RetroLogger.debug(LogCategory.Mapping, "Generating class mappings");
		Map<String, String> mappings = classMapper.build(classes);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} class mappings", mappings.size());
		return mappings;
	}

	private Map<String, ClassMetadata> buildMetadata(List<ClassInfo> classes) {
		RetroLogger.debug(LogCategory.Mapping, "Generating class metadata");
		Map<String, ClassMetadata> metadata = methodMapper.buildMetadata(classes);
		RetroLogger.debug(LogCategory.Mapping, "Generated metadata for {} classes", metadata.size());
		return metadata;
	}

	private Map<String, String> buildFieldMappings(List<ClassInfo> classes) {
		RetroLogger.debug(LogCategory.Mapping, "Generating field mappings");
		Map<String, String> mappings = fieldMapper.build(classes);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} field mappings", mappings.size());
		return mappings;
	}

	private Map<String, String> buildMethodMappings(List<ClassInfo> sortedClasses,
			Map<String, ClassMetadata> metadata) {
		RetroLogger.debug(LogCategory.Mapping, "Generating method mappings");
		List<ClassInfo> inheritanceOrder = sortByInheritance(sortedClasses, metadata);
		Map<String, String> mappings = methodMapper.build(inheritanceOrder, metadata);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} method mappings", mappings.size());
		return mappings;
	}

	private Map<String, String> buildLocalVariableMappings(List<ClassInfo> classes) {
		RetroLogger.debug(LogCategory.Mapping, "Generating local variable mappings");
		Map<String, String> mappings = localVariableMapper.build(classes);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} local variable mappings", mappings.size());
		return mappings;
	}

	private Map<String, String> buildEnumMappings(List<ClassInfo> classes) {
		RetroLogger.debug(LogCategory.Mapping, "Generating enum mappings");
		Map<String, String> mappings = enumMapper.build(classes);
		RetroLogger.debug(LogCategory.Mapping, "Generated {} enum mappings", mappings.size());
		return mappings;
	}

	/*
	 * Выводит сводную статистику по количеству mappings, созданных каждым
	 * компонентом.
	 */
	private void logMappingSummary(Map<String, String> classes, Map<String, String> fields, Map<String, String> methods,
			Map<String, String> localVariables, Map<String, String> enums) {
		RetroLogger.info(LogCategory.Mapping,
				"Generated mappings: {} classes, {} fields, {} methods, {} variables, {} enum constants",
				classes.size(), fields.size(), methods.size(), localVariables.size(), enums.size());
	}

	/*
	 * Создаёт копию входного списка и сортирует классы по имени, обеспечивая
	 * детерминированный порядок их обработки.
	 */
	private List<ClassInfo> sortClasses(List<ClassInfo> classInfos) {
		List<ClassInfo> sorted = new ArrayList<ClassInfo>(classInfos);
		Collections.sort(sorted, new Comparator<ClassInfo>() {
			@Override
			public int compare(ClassInfo first, ClassInfo second) {
				return first.getName().compareTo(second.getName());
			}
		});
		RetroLogger.debug(LogCategory.Mapping, "Classes sorted alphabetically");
		return sorted;
	}

	/**
	 * Сортирует классы так, чтобы родительские классы и интерфейсы обрабатывались
	 * раньше классов, которые от них наследуются. Это необходимо для корректного
	 * переиспользования имён переопределённых методов.
	 */
	private List<ClassInfo> sortByInheritance(List<ClassInfo> sortedClasses, Map<String, ClassMetadata> metadata) {
		Map<String, ClassInfo> classesByName = new HashMap<String, ClassInfo>();
		for (ClassInfo classInfo : sortedClasses) {
			classesByName.put(classInfo.getName(), classInfo);
		}
		List<ClassInfo> orderedClasses = new ArrayList<ClassInfo>();
		Set<String> visited = new HashSet<String>();
		Set<String> visiting = new HashSet<String>();
		for (ClassInfo classInfo : sortedClasses) {
			this.visitClass(classInfo.getName(), classesByName, metadata, visited, visiting, orderedClasses);
		}
		return orderedClasses;
	}

	/*
	 * Рекурсивно обходит иерархию класса и добавляет его после всех родительских
	 * классов и интерфейсов.
	 */
	private void visitClass(String className, Map<String, ClassInfo> classesByName, Map<String, ClassMetadata> metadata,
			Set<String> visited, Set<String> visiting, List<ClassInfo> orderedClasses) {
		if (visited.contains(className)) {
			return;
		}

		/*
		 * Защищает рекурсивный обход от циклов в графе наследования.
		 */
		if (!visiting.add(className)) {
			RetroLogger.warn(LogCategory.Mapping, "Inheritance cycle detected involving {}", className);
			return;
		}
		ClassMetadata classMetadata = metadata.get(className);

		if (classMetadata != null) {
			String superName = classMetadata.getSuperName();
			if (superName != null && classesByName.containsKey(superName)) {
				this.visitClass(superName, classesByName, metadata, visited, visiting, orderedClasses);
			}
			List<String> interfaces = new ArrayList<String>(classMetadata.getInterfaces());
			interfaces.sort(String::compareTo);
			for (String interfaceName : interfaces) {
				if (classesByName.containsKey(interfaceName)) {
					this.visitClass(interfaceName, classesByName, metadata, visited, visiting, orderedClasses);
				}
			}
		}
		visiting.remove(className);
		visited.add(className);
		ClassInfo classInfo = classesByName.get(className);
		if (classInfo != null) {
			orderedClasses.add(classInfo);
		}
	}
}
