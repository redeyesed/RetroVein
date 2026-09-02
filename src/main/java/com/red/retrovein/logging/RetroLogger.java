package com.red.retrovein.logging;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RetroLogger {
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static volatile LogLevel level = LogLevel.INFO;

	private RetroLogger() {
		throw new AssertionError("No instances");
	}

	public static void setLevel(LogLevel level) {
		if (level == null) {
			throw new IllegalArgumentException("level cannot be null");
		}

		RetroLogger.level = level;
	}

	public static LogLevel getLevel() {
		return level;
	}

	public static boolean isDebugEnabled() {
		return level == LogLevel.DEBUG;
	}

	public static void debug(String message) {
		log(LogLevel.DEBUG, message);
	}

	public static void debug(String message, Object... args) {
		log(LogLevel.DEBUG, format(message, args));
	}

	public static void info(String message) {
		log(LogLevel.INFO, message);
	}

	public static void info(String message, Object... args) {
		log(LogLevel.INFO, format(message, args));
	}

	public static void warn(String message) {
		log(LogLevel.WARN, message);
	}

	public static void warn(String message, Object... args) {
		log(LogLevel.WARN, format(message, args));
	}

	public static void error(String message) {
		log(LogLevel.ERROR, message);
	}

	public static void error(String message, Object... args) {
		log(LogLevel.ERROR, format(message, args));
	}

	public static void error(String message, Throwable throwable) {
		log(LogLevel.ERROR, message);

		if (throwable != null) {
			throwable.printStackTrace(System.err);
		}
	}

	public static void section(String title) {
		if (title == null) {
			title = "";
		}

		System.out.println();
		System.out.println(title + ":");
	}

	public static void log(LogLevel logLevel, String message) {
		if (logLevel == null) {
			return;
		}

		if (logLevel.getPriority() < level.getPriority()) {
			return;
		}

		String time = LocalDateTime.now().format(TIME_FORMAT);

		String output = time + " [" + logLevel.name() + "] " + message;

		PrintStream stream = logLevel == LogLevel.ERROR ? System.err : System.out;

		stream.println(output);
	}

	private static String format(String message, Object... args) {
		if (message == null) {
			return "null";
		}

		if (args == null || args.length == 0) {
			return message;
		}

		String result = message;

		for (Object arg : args) {
			int index = result.indexOf("{}");

			if (index == -1) {
				break;
			}

			result = result.substring(0, index) + String.valueOf(arg) + result.substring(index + 2);
		}

		return result;
	}
}
