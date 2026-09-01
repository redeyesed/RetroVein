package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingClassAdapter;

public final class AsmRemappingTransformer implements ClassTransformer {

	@Override
	public byte[] transform(final String className, byte[] bytecode, TransformationContext context) {

		final Mapping mapping = context.getMapping();

		ClassReader reader = new ClassReader(bytecode);
		ClassWriter writer = new ClassWriter(reader, 0);

		/*
		 * First create the normal ASM remapper.
		 */
		final RemappingClassAdapter remapper = new RemappingClassAdapter(writer, new AsmRemapper(mapping));

		/*
		 * This visitor receives ORIGINAL method names.
		 *
		 * That is important because local variable mappings are stored using the
		 * original method name.
		 */
		ClassVisitor localVariableRemapper = new ClassVisitor(Opcodes.ASM5, remapper) {

			@Override
			public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature,
					String[] exceptions) {

				MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

				if (visitor == null) {
					return null;
				}

				return new MethodVisitor(Opcodes.ASM5, visitor) {

					@Override
					public void visitLocalVariable(String localName, String localDescriptor, String localSignature,
							Label start, Label end, int index) {

						if ("this".equals(localName)) {
							super.visitLocalVariable(localName, localDescriptor, localSignature, start, end, index);

							return;
						}

						String mappedName = mapping.getLocalVariableName(className, name, descriptor, index);

						if (mappedName == null) {
							mappedName = localName;
						}

						super.visitLocalVariable(mappedName, localDescriptor, localSignature, start, end, index);
					}
				};
			}
		};

		reader.accept(localVariableRemapper, ClassReader.EXPAND_FRAMES);

		return writer.toByteArray();
	}
}
