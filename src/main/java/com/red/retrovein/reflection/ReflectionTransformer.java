package com.red.retrovein.reflection;

import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public final class ReflectionTransformer {
	public byte[] transform(String className, byte[] bytecode, Mapping mapping) {
		ReflectionAnalyzer analyzer = new ReflectionAnalyzer();

		List<ReflectionReference> references = analyzer.analyze(className, bytecode);

		if (references.isEmpty()) {
			return bytecode;
		}

		RetroLogger.debug(LogCategory.Transform, "Reflection references found in {}: {}", className, references.size());

		ClassReader reader = new ClassReader(bytecode);
		ClassWriter writer = new ClassWriter(reader, 0);

		/*
		 * Первый проход нужен для определения того, какие строковые константы относятся
		 * к reflection.
		 */
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {

			@Override
			public MethodVisitor visitMethod(int access, final String methodName, final String methodDescriptor,
					String signature, String[] exceptions) {
				MethodVisitor methodVisitor = super.visitMethod(access, methodName, methodDescriptor, signature,
						exceptions);

				if (methodVisitor == null) {
					return null;
				}

				return new MethodVisitor(Opcodes.ASM5, methodVisitor) {

					@Override
					public void visitLdcInsn(Object value) {
						if (!(value instanceof String)) {
							super.visitLdcInsn(value);
							return;
						}

						String originalValue = (String) value;
						String transformedValue = transformString(className, methodName, methodDescriptor,
								originalValue, references, mapping);

						if (!originalValue.equals(transformedValue)) {

							RetroLogger.debug(LogCategory.Transform,
									"Reflection string transformed: {} -> {} in {}.{}{}", originalValue,
									transformedValue, className, methodName, methodDescriptor);
						}

						super.visitLdcInsn(transformedValue);
					}
				};
			}
		};

		reader.accept(visitor, ClassReader.EXPAND_FRAMES);

		return writer.toByteArray();
	}

	private String transformString(String className, String methodName, String methodDescriptor, String value,
			List<ReflectionReference> references, Mapping mapping) {
		for (ReflectionReference reference : references) {

			if (!className.equals(reference.getOwnerClass())) {
				continue;
			}

			if (!methodName.equals(reference.getOwnerMethod())) {
				continue;
			}

			if (!methodDescriptor.equals(reference.getOwnerDescriptor())) {
				continue;
			}

			if (reference.getConfidence() != ReflectionReference.Confidence.CERTAIN) {
				continue;
			}

			if (!value.equals(reference.getValue())) {
				continue;
			}

			switch (reference.getType()) {
			case CLASS:
				return transformClass(value, mapping);
			case METHOD:
				return transformMethod(className, value, mapping);
			case FIELD:
				return transformField(className, value, mapping);
			default:
				return value;
			}
		}

		return value;
	}

	private String transformClass(String value, Mapping mapping) {
		return mapping.getClassNameFromJavaName(value);
	}

	private String transformMethod(String ownerClass, String value, Mapping mapping) {
		String mappedOwner = mapping.getClassName(ownerClass);
		return mapping.findMappedMethodName(mappedOwner, value);
	}

	private String transformField(String ownerClass, String value, Mapping mapping) {
		String mappedOwner = mapping.getClassName(ownerClass);
		return mapping.findMappedFieldName(mappedOwner, value);
	}
}
