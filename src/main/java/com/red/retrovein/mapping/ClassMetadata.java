package com.red.retrovein.mapping;

import java.util.ArrayList;
import java.util.List;

public final class ClassMetadata {
	private final String name;
	private String superName;
	private final List<String> interfaces;

	public ClassMetadata(String name) {
		this.name = name;
		this.interfaces = new ArrayList<String>();
	}

	public String getName() {
		return name;
	}

	public String getSuperName() {
		return superName;
	}

	public void setSuperName(String superName) {
		this.superName = superName;
	}

	public List<String> getInterfaces() {
		return interfaces;
	}

	public void addInterface(String interfaceName) {
		interfaces.add(interfaceName);
	}
}
