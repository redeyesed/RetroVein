package com.red.retrovein.transform;

public interface ClassTransformer {
	byte[] transform(String className, byte[] bytecode, TransformationContext context);

}
