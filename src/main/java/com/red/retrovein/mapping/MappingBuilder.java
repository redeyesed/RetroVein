package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;

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
		List<ClassInfo> sortedClasses = sortClasses(classInfos);

		Map<String, String> classes = classMapper.build(sortedClasses);
		Map<String, ClassMetadata> metadata = methodMapper.buildMetadata(sortedClasses);
		Map<String, String> fields = fieldMapper.build(sortedClasses);
		Map<String, String> methods = methodMapper.build(sortedClasses, metadata);
		Map<String, String> localVariables = localVariableMapper.build(sortedClasses);

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
