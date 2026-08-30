package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

public final class TransformationContext {
	private final Mapping mapping;

	public TransformationContext(Mapping mapping) {
		this.mapping = mapping;
	}

	public Mapping getMapping() {
		return mapping;
	}
}
