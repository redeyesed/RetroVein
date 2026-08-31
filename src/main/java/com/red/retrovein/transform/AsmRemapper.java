package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.commons.Remapper;

public final class AsmRemapper extends Remapper {
	private final Mapping mapping;

	public AsmRemapper(Mapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public String map(String internalName) {
		return mapping.getClassName(internalName);
	}

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
}