package com.red.retrovein.transform;

import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.MethodNode;

public final class AsmRemappingTransformer implements ClassTransformer {
	@Override
	public byte[] transform(final String className, byte[] bytecode, Mapping mapping) {
		RetroLogger.debug("ASM transform started: {} ({} bytes)", className, bytecode.length);

		ClassReader reader = new ClassReader(bytecode);
		ClassWriter writer = new ClassWriter(reader, 0);
		/*
		 * First create the normal ASM remapper.
		 */
		final RemappingClassAdapter remapper = new RemappingClassAdapter(writer, new AsmRemapper(mapping));
		final ReflectionRemapper reflectionRemapper = new ReflectionRemapper(mapping);

		/*
		 * This visitor receives ORIGINAL method names.
		 *
		 * That is important because local variable mappings are stored using the
		 * original method name.
		 */
		ClassVisitor localVariableRemapper = new ClassVisitor(Opcodes.ASM5, remapper) {

			/*
			 * Обрабатываем аннотации класса, содержащие ссылки на классы Forge.
			 */
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationVisitor visitor = super.visitAnnotation(descriptor, visible);
				return ForgeAnnotationRemapper.remap(descriptor, visitor, mapping);
			}

			/*
			 * Обрабатываем аннотации полей. В частности, это нужно для
			 * @SidedProxy, где имена прокси указываются прямо в аннотации.
			 */
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				final FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
				if (visitor == null) {
					return null;
				}
				return new FieldVisitor(Opcodes.ASM5, visitor) {
					@Override
					public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
						AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
						return ForgeAnnotationRemapper.remap(descriptor, annotationVisitor, mapping);
					}
				};
			}

			@Override
			public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature,
					String[] exceptions) {
				final MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

				if (visitor == null) {
					return null;
				}

				final MethodNode method = new MethodNode(Opcodes.ASM5, access, name, descriptor, signature, exceptions);

				return new MethodVisitor(Opcodes.ASM5, method) {

					@Override
					public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
						AnnotationVisitor annotationVisitor = method.visitAnnotation(annotationDescriptor, visible);

						return ForgeAnnotationRemapper.remap(annotationDescriptor, annotationVisitor, mapping);
					}

					@Override
					public void visitLocalVariable(String localName, String localDescriptor, String localSignature,
							Label start, Label end, int index) {

						if ("this".equals(localName)) {
							method.visitLocalVariable(localName, localDescriptor, localSignature, start, end, index);

							return;
						}

						String mappedName = mapping.getLocalVariableName(className, name, descriptor, index);

						if (mappedName == null) {
							mappedName = localName;
						}

						if (!localName.equals(mappedName)) {
							RetroLogger.debug("Local variable transformed: {} -> {} in {}.{}{} #{}", localName,
									mappedName, className, name, descriptor, index);
						}

						method.visitLocalVariable(mappedName, localDescriptor, localSignature, start, end, index);
					}

					@Override
					public void visitEnd() {
						/*
						 * Сначала обрабатываем reflection-вызовы внутри метода, пока он представлен в
						 * виде MethodNode.
						 */
						reflectionRemapper.remapReflection(method);

						/*
						 * После всех дополнительных преобразований передаём готовый метод в основной
						 * ASM visitor.
						 */
						method.accept(visitor);
					}
				};
			}
		};

		reader.accept(localVariableRemapper, ClassReader.EXPAND_FRAMES);

		byte[] result = writer.toByteArray();

		RetroLogger.debug("ASM transform complete: {} ({} -> {} bytes)", className, bytecode.length, result.length);

		return result;
	}
}
