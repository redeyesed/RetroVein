package com.red.retrovein.mapping;

public final class NameGenerator {
	private int counter;

	public NameGenerator() {
		this.counter = 0;
	}

	public String next() {
		return encode(counter++);
	}

	private String encode(int value) {
		StringBuilder result = new StringBuilder();

		do {
			result.append((char) ('a' + (value % 26)));
			value = value / 26;
		} while (value > 0);

		return result.reverse().toString();
	}
}
