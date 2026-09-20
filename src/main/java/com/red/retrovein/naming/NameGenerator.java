package com.red.retrovein.naming;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class NameGenerator {
	private final Random random = new Random();

	/*
	 * Отдельные наборы предотвращают повторение случайных имён каждого типа.
	 */
	private final Set<String> methodNames = new HashSet<String>();
	private final Set<String> fieldNames = new HashSet<String>();
	private final Set<String> enumNames = new HashSet<String>();

	private int classCounter;
	private int parameterCounter;
	private int variableCounter;

	private static String generateName(int value) {
		StringBuilder name = new StringBuilder();
		do {
			name.append((char) ('a' + value % 26));
			value = value / 26 - 1;
		} while (value >= 0);
		return name.reverse().toString();
	}

	private String generateRandomMethodName() {
		String name;
		do {
			name = "_" + generateName(this.random.nextInt(26 * 26));
		} while (!this.methodNames.add(name));
		return name;
	}

	private String generateRandomUpperName(Set<String> usedNames) {
		String name;
		do {
			char first = (char) ('A' + this.random.nextInt(26));
			char second = (char) ('A' + this.random.nextInt(26));
			name = "_" + first + second;
		} while (!usedNames.add(name));
		return name;
	}

	public String nextClass() {
		return generateName(this.classCounter++);
	}

	public String nextMethod() {
		return generateRandomMethodName();
	}

	public String nextField() {
		return generateRandomUpperName(this.fieldNames);
	}

	public String nextEnum() {
		return generateRandomUpperName(this.enumNames);
	}

	public String nextParameter() {
		return "par" + this.parameterCounter++;
	}

	public String nextVariable() {
		return "var" + this.variableCounter++;
	}
}
