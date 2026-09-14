package com.red.retrovein.transform;

import com.red.retrovein.mapping.Mapping;

public interface ClassTransformer {
	byte[] transform(String className, byte[] bytecode, Mapping mapping);
}
