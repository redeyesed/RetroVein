package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;
import com.red.retrovein.logging.RetroLogger;

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
	public Map<String, ClassMetadata> buildMetadata(List<ClassInfo> classInfos) {
		Map<String, ClassMetadata> metadata = new HashMap<String, ClassMetadata>();

		for (ClassInfo classInfo : classInfos) {

			ClassMetadata classMetadata = readMetadata(classInfo);

			metadata.put(classInfo.getName(), classMetadata);

			RetroLogger.debug("Metadata: {} extends {} implements {}", classInfo.getName(),
					classMetadata.getSuperName(), classMetadata.getInterfaces());
		}

		RetroLogger.debug("Generated metadata for {} classes", metadata.size());

		return metadata;
	}

	public Map<String, String> build(List<ClassInfo> classInfos, Map<String, ClassMetadata> metadata) {
		Map<String, String> methods = new HashMap<String, String>();

		for (ClassInfo classInfo : classInfos) {

			collectMethods(classInfo, metadata, methods);
		}

		RetroLogger.debug("Generated {} method mappings", methods.size());

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
		final NameGenerator nameGenerator = new NameGenerator();
		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				/*
				 * Constructors cannot be renamed.
				 */
				if ("<init>".equals(name) || "<clinit>".equals(name)) {

					RetroLogger.debug("Skipping constructor/static initializer: {}.{}{}", classInfo.getName(), name,
							descriptor);

					return null;
				}

				/*
				 * JVM entry point must retain the "main" method name.
				 */
				if ("main".equals(name) && "([Ljava/lang/String;)V".equals(descriptor)) {

					RetroLogger.debug("Skipping JVM entry point: {}.main{}", classInfo.getName(), descriptor);

					return null;
				}

				/*
				 * Private methods do not participate in overriding.
				 */
				if ((access & Opcodes.ACC_PRIVATE) != 0) {

					createMethodMapping(classInfo.getName(), name, descriptor, methods, nameGenerator);

					return null;
				}

				String overriddenKey = findOverriddenMethod(classInfo.getName(), name, descriptor, metadata, methods);

				if (overriddenKey != null) {

					String key = createMethodKey(classInfo.getName(), name, descriptor);

					String mappedName = methods.get(overriddenKey);

					methods.put(key, mappedName);

					RetroLogger.debug("Method override mapping: {} -> {} (from {})", key, mappedName, overriddenKey);

				} else {

					createMethodMapping(classInfo.getName(), name, descriptor, methods, nameGenerator);
				}

				return null;
			}

		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private void createMethodMapping(String owner, String name, String descriptor, Map<String, String> methods,
			NameGenerator nameGenerator) {
		String key = createMethodKey(owner, name, descriptor);

		String mappedName = nameGenerator.nextMethod();

		methods.put(key, mappedName);

		RetroLogger.debug("Method mapping: {} -> {}", key, mappedName);
	}

	private String findOverriddenMethod(String owner, String name, String descriptor,
			Map<String, ClassMetadata> metadata, Map<String, String> methods) {
		ClassMetadata current = metadata.get(owner);

		if (current == null) {

			RetroLogger.debug("No metadata found for {}", owner);

			return null;
		}

		/*
		 * Check superclass chain.
		 */
		String superName = current.getSuperName();

		while (superName != null) {

			String key = createMethodKey(superName, name, descriptor);

			if (methods.containsKey(key)) {

				RetroLogger.debug("Found overridden method in superclass: {}", key);

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

		String result = findInterfaceMethod(current, name, descriptor, metadata, methods, visited);

		if (result != null) {

			RetroLogger.debug("Found overridden method in interface: {}", result);
		}

		return result;
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
