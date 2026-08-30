package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public final class AsmClassRenamer implements ClassTransformer {
	@Override
	public byte[] transform(String className, byte[] bytecode, TransformationContext context) {
		final Mapping mapping = context.getMapping();

		ClassReader reader = new ClassReader(bytecode);

		ClassWriter writer = new ClassWriter(reader, 0);

		Remapper remapper = new Remapper() {

			@Override
			public String map(String internalName) {
				return mapping.getClassName(internalName);
			}
		};

		RemappingClassAdapter visitor = new RemappingClassAdapter(writer, remapper);

		reader.accept(visitor, 0);

		return writer.toByteArray();
	}
}
