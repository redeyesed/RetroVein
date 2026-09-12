package com.red.retrovein.mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class ExternalMetadataReader {
	public ClassMetadata read(String internalName, byte[] bytecode) {
		final ClassMetadata metadata = new ClassMetadata(internalName);

		ClassReader reader = new ClassReader(bytecode);

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				metadata.setSuperName(superName);

				if (interfaces != null) {
					for (String interfaceName : interfaces) {
						metadata.addInterface(interfaceName);
					}
				}
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				if (!"<init>".equals(name) && !"<clinit>".equals(name)) {

					metadata.addMethod(name, descriptor);
				}

				return null;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		return metadata;
	}
}
