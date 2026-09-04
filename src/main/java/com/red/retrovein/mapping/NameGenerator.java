package com.red.retrovein.mapping;

public class NameGenerator {
	private int classCounter = 1;
	private int variableCounter;
	private int parameterCounter;
	private int fieldCounter;
	private int methodCounter;

	public String next() {
		return generate(NameType.CLASS, classCounter++);
	}

	public String nextVariable() {
		return generate(NameType.VARIABLE, ++variableCounter);
	}

	public String nextParameter() {
		return generate(NameType.PARAMETER, ++parameterCounter);
	}

	public String nextField() {
		return generate(NameType.FIELD, ++fieldCounter);
	}

	public String nextMethod() {
		return generate(NameType.METHOD, ++methodCounter);
	}

	private String generate(NameType type, int counter) {
		String value = type.isEncoded() ? encode(counter) : String.valueOf(counter);

		return type.getPrefix() + value;
	}

	public static String encode(int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("Value must be positive");
		}

		StringBuilder result = new StringBuilder();

		while (value > 0) {
			value--;

			int digit = value % 26;
			result.append((char) ('a' + digit));

			value /= 26;
		}

		return result.reverse().toString();
	}
}
