package com.red.retrovein.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;

public final class EnumMapper {
	public Map<String, String> build(List<ClassInfo> classInfos) {
		Map<String, String> mappings = new HashMap<>();

		for (ClassInfo classInfo : classInfos) {
			collect(classInfo, mappings);
		}

		RetroLogger.debug(LogCategory.Mapping, "Generated {} enum mappings", mappings.size());

		return mappings;
	}

	private void collect(final ClassInfo classInfo, final Map<String, String> mappings) {
		ClassReader reader = new ClassReader(classInfo.getBytecode());

		if (!isEnum(reader)) {
			return;
		}

		final NameGenerator nameGenerator = new NameGenerator();

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				/*
				 * Константы перечисления представлены статическими полями с флагом ACC_ENUM.
				 */
				if (((access & Opcodes.ACC_ENUM) == 0) || ((access & Opcodes.ACC_STATIC) == 0)) {
					return null;
				}

				String key = createKey(classInfo.getName(), name, descriptor);
				String mappedName = nameGenerator.nextEnum();

				mappings.put(key, mappedName);

				RetroLogger.debug(LogCategory.Mapping, "Enum mapping: {}.{}:{} -> {}", classInfo.getName(), name,
						descriptor, mappedName);

				return null;
			}

		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private boolean isEnum(ClassReader reader) {
		return (reader.getAccess() & Opcodes.ACC_ENUM) != 0;
	}

	public static String createKey(String owner, String name, String descriptor) {
		return owner + "." + name + ":" + descriptor;
	}
}
