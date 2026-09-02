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

	public static boolean isEnabled(LogLevel level) {
		if (level == null) {
			return false;
		}

		return level.getPriority() >= RetroLogger.level.getPriority();
	}

	public static boolean isTraceEnabled() {
		return isEnabled(LogLevel.TRACE);
	}

	public static boolean isDebugEnabled() {
		return isEnabled(LogLevel.DEBUG);
	}

	public static void trace(String message) {
		trace(LogCategory.Main, message);
	}

	public static void trace(String message, Object... args) {
		trace(LogCategory.Main, message, args);
	}

	public static void trace(LogCategory category, String message) {
		log(LogLevel.TRACE, category, message, null);
	}

	public static void trace(LogCategory category, String message, Object... args) {
		log(LogLevel.TRACE, category, format(message, args), null);
	}

	public static void debug(String message) {
		debug(LogCategory.Main, message);
	}

	public static void debug(String message, Object... args) {
		debug(LogCategory.Main, message, args);
	}

	public static void debug(LogCategory category, String message) {
		log(LogLevel.DEBUG, category, message, null);
	}

	public static void debug(LogCategory category, String message, Object... args) {
		log(LogLevel.DEBUG, category, format(message, args), null);
	}

	public static void info(String message) {
		info(LogCategory.Main, message);
	}

	public static void info(String message, Object... args) {
		info(LogCategory.Main, message, args);
	}

	public static void info(LogCategory category, String message) {
		log(LogLevel.INFO, category, message, null);
	}

	public static void info(LogCategory category, String message, Object... args) {
		log(LogLevel.INFO, category, format(message, args), null);
	}

	public static void warn(String message) {
		warn(LogCategory.Main, message);
	}

	public static void warn(String message, Object... args) {
		warn(LogCategory.Main, message, args);
	}

	public static void warn(LogCategory category, String message) {
		log(LogLevel.WARN, category, message, null);
	}

	public static void warn(LogCategory category, String message, Object... args) {
		log(LogLevel.WARN, category, format(message, args), null);
	}

	public static void error(String message) {
		error(LogCategory.Main, message);
	}

	public static void error(String message, Object... args) {
		error(LogCategory.Main, message, args);
	}

	public static void error(LogCategory category, String message) {
		log(LogLevel.ERROR, category, message, null);
	}

	public static void error(LogCategory category, String message, Object... args) {
		log(LogLevel.ERROR, category, format(message, args), null);
	}

	public static void error(String message, Throwable throwable) {
		error(LogCategory.Main, message, throwable);
	}

	public static void error(LogCategory category, String message, Throwable throwable) {

		log(LogLevel.ERROR, category, message, throwable);
	}

	public static void log(LogLevel logLevel, String message) {
		log(logLevel, LogCategory.Main, message, null);
	}

	public static void log(LogLevel logLevel, LogCategory category, String message) {

		log(logLevel, category, message, null);
	}

	private static void log(LogLevel logLevel, LogCategory category, String message, Throwable throwable) {

		if (!isEnabled(logLevel)) {
			return;
		}

		if (category == null) {
			category = LogCategory.Main;
		}

		if (message == null) {
			message = "null";
		}

		String time = LocalDateTime.now().format(TIME_FORMAT);

		String output = time + " [" + logLevel.name() + "]" + " [" + category.name() + "] " + message;

		PrintStream stream = logLevel == LogLevel.ERROR ? System.err : System.out;

		stream.println(output);

		if (throwable != null) {
			throwable.printStackTrace(stream);
		}
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
