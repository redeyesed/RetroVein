package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.RetroLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MappingBuilder {
	private final ClassMapper classMapper;
	private final FieldMapper fieldMapper;
	private final MethodMapper methodMapper;
	private final LocalVariableMapper localVariableMapper;

	public MappingBuilder() {
		this.classMapper = new ClassMapper();
		this.fieldMapper = new FieldMapper();
		this.methodMapper = new MethodMapper();
		this.localVariableMapper = new LocalVariableMapper();
	}

	public Mapping build(List<ClassInfo> classInfos) {
		RetroLogger.info("Preparing {} classes for mapping", classInfos.size());

		List<ClassInfo> sortedClasses = sortClasses(classInfos);

		RetroLogger.debug("Classes sorted alphabetically");

		RetroLogger.info("Generating class mappings...");

		Map<String, String> classes = classMapper.build(sortedClasses);

		RetroLogger.info("Generating class metadata...");

		Map<String, ClassMetadata> metadata = methodMapper.buildMetadata(sortedClasses);

		RetroLogger.info("Generating field mappings...");

		Map<String, String> fields = fieldMapper.build(sortedClasses);

		RetroLogger.info("Generating method mappings...");

		Map<String, String> methods = methodMapper.build(sortedClasses, metadata);

		RetroLogger.info("Generating local variable mappings...");

		Map<String, String> localVariables = localVariableMapper.build(sortedClasses);

		RetroLogger.info("Mapping complete: " + "{} classes, " + "{} fields, " + "{} methods, " + "{} local variables",
				classes.size(), fields.size(), methods.size(), localVariables.size());

		return new Mapping(classes, methods, fields, localVariables);
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
}
