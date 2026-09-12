package com.red.retrovein.transform;

import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public final class ForgeAnnotationRemapper {
	private static final String SIDED_PROXY = "Lcpw/mods/fml/common/SidedProxy;";

	public static AnnotationVisitor remap(String descriptor, AnnotationVisitor visitor, Mapping mapping) {
		if (visitor == null) {
			return null;
		}
		if (SIDED_PROXY.equals(descriptor)) {
			return new SidedProxyVisitor(visitor, mapping);
		}
		return visitor;
	}

	/*
	 * Обрабатываем имена прокси, указанные в @SidedProxy.
	 */
	private static final class SidedProxyVisitor extends AnnotationVisitor {
		private final Mapping mapping;

		private SidedProxyVisitor(AnnotationVisitor visitor, Mapping mapping) {
			super(Opcodes.ASM5, visitor);
			this.mapping = mapping;
		}

		@Override
		public void visit(String name, Object value) {
			if (value instanceof String && isProxyProperty(name)) {
				value = remapClassName((String) value);
			}
			super.visit(name, value);
		}

		/*
		 * Эти параметры содержат имена классов, которые нужно переименовать.
		 */
		private boolean isProxyProperty(String name) {
			return "clientSide".equals(name) || "serverSide".equals(name) || "bukkitSide".equals(name);
		}

		private String remapClassName(String className) {
			String internalName = className.replace('.', '/');
			String mappedName = mapping.getClassName(internalName);
			String result = mappedName.replace('/', '.');
			if (!className.equals(result)) {
				RetroLogger.debug("Forge @SidedProxy: {} -> {}", className, result);
			}
			return result;
		}
	}
}
