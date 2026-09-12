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
	private final ExternalClassResolver externalResolver;

	public MethodMapper() {
		this.externalResolver = new ExternalClassResolver();
	}

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

	private void collectMethods(final ClassInfo classInfo, final Map<String, ClassMetadata> metadata,
			final Map<String, String> methods) {
		final NameGenerator nameGenerator = new NameGenerator();

		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
					String[] exceptions) {
				if ("<init>".equals(name) || "<clinit>".equals(name)) {

					return null;
				}

				if ("main".equals(name) && "([Ljava/lang/String;)V".equals(descriptor)) {

					RetroLogger.debug("Keeping JVM entry point: {}.main{}", classInfo.getName(), descriptor);

					return null;
				}

				boolean publicMethod = (access & Opcodes.ACC_PUBLIC) != 0;

				boolean protectedMethod = (access & Opcodes.ACC_PROTECTED) != 0;

				boolean privateMethod = (access & Opcodes.ACC_PRIVATE) != 0;

				/*
				 * Private methods cannot override anything. They are always safe to rename.
				 */
				if (privateMethod) {
					createMethodMapping(classInfo.getName(), name, descriptor, methods, nameGenerator);

					return null;
				}

				/*
				 * Look for an existing method in the module or in an external
				 * superclass/interface.
				 */
				MethodResolution resolution = findOverriddenMethod(classInfo.getName(), name, descriptor, metadata,
						methods);

				if (resolution.isFound()) {
					/*
					 * The method belongs to an existing contract, so its name must remain
					 * compatible.
					 */
					if (resolution.getMappedName() != null) {
						String key = createMethodKey(classInfo.getName(), name, descriptor);

						methods.put(key, resolution.getMappedName());

						RetroLogger.debug("Keeping override mapping: {} -> {}", key, resolution.getMappedName());
					}

					return null;
				}

				/*
				 * If we couldn't inspect an externally visible method, fail safely and keep its
				 * original name.
				 */
				if ((publicMethod || protectedMethod) && resolution.isExternalUnknown()) {

					RetroLogger.debug("Keeping unresolved external method: {}.{}{}", classInfo.getName(), name,
							descriptor);

					return null;
				}

				/*
				 * The method has no known external contract. It can be renamed.
				 */
				createMethodMapping(classInfo.getName(), name, descriptor, methods, nameGenerator);

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

	private MethodResolution findOverriddenMethod(String owner, String name, String descriptor,
			Map<String, ClassMetadata> metadata, Map<String, String> methods) {
		ClassMetadata current = metadata.get(owner);

		if (current == null) {
			return MethodResolution.notFound();
		}

		/*
		 * First inspect superclass hierarchy.
		 */
		String superName = current.getSuperName();

		Set<String> visited = new HashSet<String>();

		while (superName != null) {

			if (!visited.add(superName)) {
				break;
			}

			String key = createMethodKey(superName, name, descriptor);

			/*
			 * Method belongs to a module class and already has a mapping. Reuse it.
			 */
			if (methods.containsKey(key)) {
				return MethodResolution.found(methods.get(key));
			}

			ClassMetadata superMetadata = metadata.get(superName);

			/*
			 * Module superclass.
			 */
			if (superMetadata != null) {

				if (superMetadata.hasMethod(name, descriptor)) {
					String mappedName = methods.get(key);

					return MethodResolution.found(mappedName);
				}

				superName = superMetadata.getSuperName();

				continue;
			}

			/*
			 * External superclass.
			 */
			ClassMetadata externalMetadata = externalResolver.resolve(superName);

			if (externalMetadata == null) {
				return MethodResolution.externalUnknown();
			}

			if (externalMetadata.hasMethod(name, descriptor)) {
				return MethodResolution.found(null);
			}

			superName = externalMetadata.getSuperName();
		}

		/*
		 * Then inspect interfaces.
		 */
		MethodResolution interfaceResult = findInterfaceMethod(current, name, descriptor, metadata, methods,
				new HashSet<String>());

		if (interfaceResult.isFound() || interfaceResult.isExternalUnknown()) {

			return interfaceResult;
		}

		return MethodResolution.notFound();
	}

	private MethodResolution findInterfaceMethod(ClassMetadata current, String name, String descriptor,
			Map<String, ClassMetadata> metadata, Map<String, String> methods, Set<String> visited) {
		for (String interfaceName : current.getInterfaces()) {

			if (!visited.add(interfaceName)) {
				continue;
			}

			String key = createMethodKey(interfaceName, name, descriptor);

			if (methods.containsKey(key)) {
				return MethodResolution.found(methods.get(key));
			}

			ClassMetadata interfaceMetadata = metadata.get(interfaceName);

			if (interfaceMetadata == null) {
				interfaceMetadata = externalResolver.resolve(interfaceName);
			}

			if (interfaceMetadata == null) {
				return MethodResolution.externalUnknown();
			}

			if (interfaceMetadata.hasMethod(name, descriptor)) {
				return MethodResolution.found(methods.get(key));
			}

			MethodResolution result = findInterfaceMethod(interfaceMetadata, name, descriptor, metadata, methods,
					visited);

			if (result.isFound() || result.isExternalUnknown()) {

				return result;
			}
		}

		return MethodResolution.notFound();
	}

	private String createMethodKey(String owner, String name, String descriptor) {
		return owner + "." + name + descriptor;
	}

	private static final class MethodResolution {

		private final boolean found;
		private final boolean externalUnknown;
		private final String mappedName;

		private MethodResolution(boolean found, boolean externalUnknown, String mappedName) {
			this.found = found;
			this.externalUnknown = externalUnknown;
			this.mappedName = mappedName;
		}

		static MethodResolution found(String mappedName) {
			return new MethodResolution(true, false, mappedName);
		}

		static MethodResolution notFound() {
			return new MethodResolution(false, false, null);
		}

		static MethodResolution externalUnknown() {
			return new MethodResolution(false, true, null);
		}

		boolean isFound() {
			return found;
		}

		boolean isExternalUnknown() {
			return externalUnknown;
		}

		String getMappedName() {
			return mappedName;
		}
	}
}
