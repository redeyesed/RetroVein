package com.red.retrovein.io;

public final class ClassInfo {
	private final String name;
	private final byte[] bytecode;

	public ClassInfo(String name, byte[] bytecode) {
		this.name = name;
		this.bytecode = bytecode;
	}

	public String getName() {
		return name;
	}

	public byte[] getBytecode() {
		return bytecode;
	}
}
