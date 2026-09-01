package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FieldMapper {
	private final NameGenerator nameGenerator;

	public FieldMapper() {
		this.nameGenerator = new NameGenerator();
	}

	public Map<String, String> build(List<ClassInfo> classInfos) {
		Map<String, String> mappings = new HashMap<String, String>();

		for (ClassInfo classInfo : classInfos) {
			collect(classInfo, mappings);
		}

		return mappings;
	}

	private void collect(final ClassInfo classInfo, final Map<String, String> mappings) {
		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				String key = classInfo.getName() + "." + name + ":" + descriptor;

				mappings.put(key, nameGenerator.next());

				return null;
			}

		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}
}
