package com.red.retrovein.reflection;

public final class ReflectionCall {
	private final String owner;
	private final String name;
	private final String descriptor;

	private final int stringArgumentIndex;

	private final ReflectionReference.Type type;

	public ReflectionCall(String owner, String name, String descriptor, int stringArgumentIndex,
			ReflectionReference.Type type) {
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
		this.stringArgumentIndex = stringArgumentIndex;
		this.type = type;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public int getStringArgumentIndex() {
		return stringArgumentIndex;
	}

	public ReflectionReference.Type getType() {
		return type;
	}
}
