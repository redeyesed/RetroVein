package com.red.retrovein.mapping;

public class NameGenerator {
	private int counter;
	private int variableCounter;
	private int parameterCounter;
	private int fieldCounter;
	private int methodCounter;

	public NameGenerator() {
		this.counter = 1;
		this.variableCounter = 0;
		this.parameterCounter = 0;
		this.fieldCounter = 0;
		this.methodCounter = 0;
	}

	public String next() {
		return encode(counter++);
	}

	public String nextVariable() {
		return "var" + (++variableCounter);
	}

	public String nextParameter() {
		return "par" + (++parameterCounter);
	}

	public String nextField() {
		return "field" + (++fieldCounter);
	}

	public String nextMethod() {
		return "method" + (++methodCounter);
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
