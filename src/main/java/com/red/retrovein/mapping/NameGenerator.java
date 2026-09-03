package com.red.retrovein.mapping;

public class NameGenerator {
	private int counter = 1;

	public String next() {
		return encode(counter++);
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
