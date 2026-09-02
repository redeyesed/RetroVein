package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.RetroLogger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LocalVariableMapper {
	public Map<String, String> build(List<ClassInfo> classInfos) {
		Map<String, String> mappings = new HashMap<String, String>();

		for (ClassInfo classInfo : classInfos) {

			collect(classInfo, mappings);
		}

		RetroLogger.info("Generated {} local variable mappings", mappings.size());

		return mappings;
	}

	private void collect(final ClassInfo classInfo, final Map<String, String> mappings) {
		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature,
					String[] exceptions) {
				final NameGenerator localNameGenerator = new NameGenerator();
				final Map<Integer, String> names = new HashMap<Integer, String>();

				return new MethodVisitor(Opcodes.ASM5) {

					@Override
					public void visitLocalVariable(String localName, String localDescriptor, String localSignature,
							Label start, Label end, int index) {
						if ("this".equals(localName)) {
							return;
						}

						String mappedName = names.get(index);

						if (mappedName == null) {

							mappedName = localNameGenerator.next();

							names.put(index, mappedName);

							RetroLogger.debug("Local variable mapping: {}.{}{} #{} -> {}", classInfo.getName(), name,
									descriptor, index, mappedName);
						}

						String key = classInfo.getName() + "." + name + descriptor + "#" + index;

						if (!mappings.containsKey(key)) {

							mappings.put(key, mappedName);
						}
					}
				};
			}

		}, ClassReader.SKIP_FRAMES);
	}
}
