package com.red.retrovein.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class AsmIdentityTransformer implements ClassTransformer {
    @Override
    public byte[] transform(String className, byte[] bytecode) {
        ClassReader reader = new ClassReader(bytecode);
        ClassWriter writer = new ClassWriter(reader, 0);

        reader.accept(writer, 0);

        return writer.toByteArray();
    }
}
