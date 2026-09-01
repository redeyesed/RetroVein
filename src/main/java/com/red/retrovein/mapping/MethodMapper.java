package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MethodMapper {
	private final NameGenerator nameGenerator;

	public MethodMapper() {
		this.nameGenerator = new NameGenerator();
	}

	public Map<String, ClassMetadata> buildMetadata(List<ClassInfo> classInfos) {
		Map<String, ClassMetadata> metadata = new HashMap<String, ClassMetadata>();

		for (ClassInfo classInfo : classInfos) {
			metadata.put(classInfo.getName(), readMetadata(classInfo));
		}

		return metadata;
	}

	public Map<String, String> build(List<ClassInfo> classInfos, Map<String, ClassMetadata> metadata) {
		Map<String, String> methods = new HashMap<String, String>();

		for (ClassInfo classInfo : classInfos) {
			collectMethods(classInfo, metadata, methods);
		}

		return methods;
	}

	private ClassMetadata readMetadata(ClassInfo classInfo) {
		final ClassMetadata metadata = new ClassMetadata(classInfo.getName());

		ClassReader reader = new ClassReader(classInfo.getBytecode());

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

		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		return metadata;
	}

	private void collectMethods(final ClassInfo classInfo, final Map<String, ClassMetadata> metadata,
			final Map<String, String> methods) {
		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {

				/*
				 * Constructors cannot be renamed.
				 */
				if ("<init>".equals(name) || "<clinit>".equals(name)) {

					return null;
				}

				/*
				 * The JVM entry point must retain the "main" method name.
				 */
				if ("main".equals(name) && "([Ljava/lang/String;)V".equals(descriptor)) {

					return null;
				}

				/*
				 * Private methods do not participate in overriding.
				 */
				if ((access & Opcodes.ACC_PRIVATE) != 0) {
					createMethodMapping(classInfo.getName(), name, descriptor, methods);

					return null;
				}

				String overriddenKey = findOverriddenMethod(classInfo.getName(), name, descriptor, metadata, methods);

				if (overriddenKey != null) {
					methods.put(createMethodKey(classInfo.getName(), name, descriptor), methods.get(overriddenKey));
				} else {
					createMethodMapping(classInfo.getName(), name, descriptor, methods);
				}

				return null;
			}

		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private void createMethodMapping(String owner, String name, String descriptor, Map<String, String> methods) {
		String key = createMethodKey(owner, name, descriptor);

		methods.put(key, nameGenerator.next());
	}

	private String findOverriddenMethod(String owner, String name, String descriptor,
			Map<String, ClassMetadata> metadata, Map<String, String> methods) {
		ClassMetadata current = metadata.get(owner);

		if (current == null) {
			return null;
		}

		/*
		 * Check superclass chain.
		 */
		String superName = current.getSuperName();

		while (superName != null) {

			String key = createMethodKey(superName, name, descriptor);

			if (methods.containsKey(key)) {
				return key;
			}

			ClassMetadata superMetadata = metadata.get(superName);

			if (superMetadata == null) {
				break;
			}

			superName = superMetadata.getSuperName();
		}

		/*
		 * Check implemented interfaces.
		 */
		Set<String> visited = new HashSet<String>();

		return findInterfaceMethod(current, name, descriptor, metadata, methods, visited);
	}

	private String findInterfaceMethod(ClassMetadata metadataEntry, String name, String descriptor,
			Map<String, ClassMetadata> metadata, Map<String, String> methods, Set<String> visited) {
		for (String interfaceName : metadataEntry.getInterfaces()) {

			if (!visited.add(interfaceName)) {
				continue;
			}

			String key = createMethodKey(interfaceName, name, descriptor);

			if (methods.containsKey(key)) {
				return key;
			}

			ClassMetadata interfaceMetadata = metadata.get(interfaceName);

			if (interfaceMetadata != null) {

				String result = findInterfaceMethod(interfaceMetadata, name, descriptor, metadata, methods, visited);

				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	private String createMethodKey(String owner, String name, String descriptor) {
		return owner + "." + name + descriptor;
	}
}
