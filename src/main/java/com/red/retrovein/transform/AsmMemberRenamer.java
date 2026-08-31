package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public final class AsmMemberRenamer implements ClassTransformer {
	@Override
	public byte[] transform(final String className, byte[] bytecode, final TransformationContext context) {
		final Mapping mapping = context.getMapping();

		ClassReader reader = new ClassReader(bytecode);
		ClassWriter writer = new ClassWriter(reader, 0);

		RemappingClassAdapter remapper = new RemappingClassAdapter(writer, new Remapper() {

			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				if ("<init>".equals(name) || "<clinit>".equals(name)) {
					return name;
				}

				return mapping.getMethodName(owner, name, descriptor);
			}

			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				return mapping.getFieldName(owner, name, descriptor);
			}
		});

		reader.accept(remapper, 0);

		return writer.toByteArray();
	}

}