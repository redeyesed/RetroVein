package com.red.retrovein.reflection;

public final class ReflectionReference {
	public enum Type {
		CLASS, METHOD, FIELD
	}

	public enum Confidence {
		CERTAIN, UNKNOWN
	}

	private final String ownerClass;
	private final String ownerMethod;
	private final String ownerDescriptor;

	private final Type type;
	private final String value;

	private final Confidence confidence;

	public ReflectionReference(String ownerClass, String ownerMethod, String ownerDescriptor, Type type, String value,
			Confidence confidence) {
		this.ownerClass = ownerClass;
		this.ownerMethod = ownerMethod;
		this.ownerDescriptor = ownerDescriptor;
		this.type = type;
		this.value = value;
		this.confidence = confidence;
	}

	public String getOwnerClass() {
		return ownerClass;
	}

	public String getOwnerMethod() {
		return ownerMethod;
	}

	public String getOwnerDescriptor() {
		return ownerDescriptor;
	}

	public Type getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public Confidence getConfidence() {
		return confidence;
	}

	@Override
	public String toString() {
		return "ReflectionReference{" + "type=" + type + ", value='" + value + '\'' + ", confidence=" + confidence
				+ ", ownerClass='" + ownerClass + '\'' + ", ownerMethod='" + ownerMethod + '\'' + ", ownerDescriptor='"
				+ ownerDescriptor + '\'' + '}';
	}
}
