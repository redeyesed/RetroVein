package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

public final class AsmRemappingTransformer implements ClassTransformer {
	@Override
	public byte[] transform(String className, byte[] bytecode, TransformationContext context) {
		Mapping mapping = context.getMapping();

		ClassReader reader = new ClassReader(bytecode);

		ClassWriter writer = new ClassWriter(reader, 0);

		RemappingClassAdapter remapper = new RemappingClassAdapter(writer, new AsmRemapper(mapping));

		reader.accept(remapper, 0);

		return writer.toByteArray();
	}
}