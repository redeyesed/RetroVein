package com.red.retrovein.logging;

public enum LogLevel {
	TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);

	private final int priority;

	LogLevel(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
}