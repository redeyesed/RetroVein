package com.red.retrovein.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClassMetadata {
	private final String name;
	private String superName;
	private final List<String> interfaces;
	private final Set<String> methods;

	public ClassMetadata(String name) {
		this.name = name;
		this.interfaces = new ArrayList<String>();
		this.methods = new HashSet<String>();
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

	public void addMethod(String name, String descriptor) {
		methods.add(createMethodKey(name, descriptor));
	}

	public boolean hasMethod(String name, String descriptor) {
		return methods.contains(createMethodKey(name, descriptor));
	}

	private String createMethodKey(String name, String descriptor) {
		return name + descriptor;
	}
}
