package com.red.retrovein.mapping;

public enum NameType {
	CLASS("", true),
	METHOD("MD", false),
	FIELD("FD", false),
	VARIABLE("var", false),
	PARAMETER("par", false);

	private final String prefix;
	private final boolean encodeCounter;

	NameType(String prefix, boolean encoded) {
		this.prefix = prefix;
		this.encodeCounter = encoded;
	}

	public String getPrefix() {
		return prefix;
	}

	public boolean isEncoded() {
		return encodeCounter;
	}
}
