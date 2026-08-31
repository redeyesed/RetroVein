package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MappingBuilder {
	private final NameGenerator nameGenerator;

	public MappingBuilder() {
		this.nameGenerator = new NameGenerator();
	}

	public Mapping build(List<ClassInfo> classInfos) {
		Map<String, String> classes = new HashMap<String, String>();
		Map<String, String> methods = new HashMap<String, String>();
		Map<String, String> fields = new HashMap<String, String>();

		List<ClassInfo> sortedClasses = new ArrayList<ClassInfo>(classInfos);

		Collections.sort(sortedClasses, new java.util.Comparator<ClassInfo>() {
			@Override
			public int compare(ClassInfo first, ClassInfo second) {
				return first.getName().compareTo(second.getName());
			}
		});

		// Class mapping.
		for (ClassInfo classInfo : sortedClasses) {
			classes.put(classInfo.getName(), nameGenerator.next());
		}

		// Method and field mapping.
		for (ClassInfo classInfo : sortedClasses) {
			collectMembers(classInfo, methods, fields);
		}

		return new Mapping(classes, methods, fields);
	}

	private void collectMembers(final ClassInfo classInfo, final Map<String, String> methods,
			final Map<String, String> fields) {

		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {

				String key = classInfo.getName() + "." + name + ":" + descriptor;

				fields.put(key, nameGenerator.next());

				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {

				if ("<init>".equals(name) || "<clinit>".equals(name)) {
					return null;
				}

				String key = classInfo.getName() + "." + name + descriptor;

				methods.put(key, nameGenerator.next());

				return null;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}
}
