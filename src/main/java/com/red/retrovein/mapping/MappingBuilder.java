package com.red.retrovein.mapping;

import com.red.retrovein.io.ClassInfo;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MappingBuilder {
	private final NameGenerator nameGenerator;

	public MappingBuilder() {
		this.nameGenerator = new NameGenerator();
	}

	public Mapping build(List<ClassInfo> classInfos) {
		Map<String, String> classes = new HashMap<String, String>();

		Map<String, String> methods = new HashMap<String, String>();

		Map<String, String> fields = new HashMap<String, String>();

		List<ClassInfo> sortedClasses = new ArrayList<ClassInfo>(classInfos);

		Collections.sort(sortedClasses, new Comparator<ClassInfo>() {
			@Override
			public int compare(ClassInfo first, ClassInfo second) {

				return first.getName().compareTo(second.getName());
			}
		});

		/*
		 * Build class mapping first.
		 */
		for (ClassInfo classInfo : sortedClasses) {
			classes.put(classInfo.getName(), nameGenerator.next());
		}

		/*
		 * Build class hierarchy information.
		 */
		Map<String, ClassMetadata> metadata = new HashMap<String, ClassMetadata>();

		for (ClassInfo classInfo : sortedClasses) {
			metadata.put(classInfo.getName(), readMetadata(classInfo));
		}

		/*
		 * Build field mapping.
		 */
		for (ClassInfo classInfo : sortedClasses) {
			collectFields(classInfo, fields);
		}

		/*
		 * Build method mapping.
		 */
		for (ClassInfo classInfo : sortedClasses) {
			collectMethods(classInfo, metadata, methods);
		}
		
		System.out.println("METHOD MAPPING:");

		for (Map.Entry<String, String> entry : methods.entrySet()) {
		    System.out.println(
		            entry.getKey() + " -> " + entry.getValue()
		    );
		}

		System.out.println("FIELD MAPPING:");

		for (Map.Entry<String, String> entry : fields.entrySet()) {
		    System.out.println(
		            entry.getKey() + " -> " + entry.getValue()
		    );
		}


		return new Mapping(classes, methods, fields);
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

	private void collectFields(final ClassInfo classInfo, final Map<String, String> fields) {
		ClassReader reader = new ClassReader(classInfo.getBytecode());

		reader.accept(new ClassVisitor(Opcodes.ASM5) {

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				String key = classInfo.getName() + "." + name + ":" + descriptor;

				fields.put(key, nameGenerator.next());

				return null;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
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
				
				// The JVM entry point must retain the "main" method name.
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
					methods.put(classInfo.getName() + "." + name + descriptor, methods.get(overriddenKey));
				} else {
					createMethodMapping(classInfo.getName(), name, descriptor, methods);
				}

				return null;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	private void createMethodMapping(String owner, String name, String descriptor, Map<String, String> methods) {
		String key = owner + "." + name + descriptor;

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
			String key = superName + "." + name + descriptor;

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

			String key = interfaceName + "." + name + descriptor;

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

	private static final class ClassMetadata {
		private final String name;
		private String superName;
		private final List<String> interfaces;

		private ClassMetadata(String name) {
			this.name = name;
			this.interfaces = new ArrayList<String>();
		}

		private void setSuperName(String superName) {
			this.superName = superName;
		}

		private void addInterface(String interfaceName) {
			interfaces.add(interfaceName);
		}

		private String getSuperName() {
			return superName;
		}

		private List<String> getInterfaces() {
			return interfaces;
		}
	}
}