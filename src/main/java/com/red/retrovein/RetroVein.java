package com.red.retrovein;

import com.red.retrovein.io.JarProcessor;
import com.red.retrovein.logging.LogLevel;
import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.transform.AsmRemappingTransformer;
import com.red.retrovein.transform.ClassTransformer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public final class RetroVein {
	private static final String VERSION = "0.1.0";

	private RetroVein() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 3) {
			printUsage();
			System.exit(1);
			return;
		}

		boolean debug = args.length == 3 && "--debug".equalsIgnoreCase(args[2]);

		if (debug) {
			RetroLogger.setLevel(LogLevel.DEBUG);
		}

		Path input = Paths.get(args[0]);
		Path output = Paths.get(args[1]);

		int threads = Runtime.getRuntime().availableProcessors();

		RetroLogger.section("RetroVein");

		RetroLogger.info("Version: {}", VERSION);
		RetroLogger.info("Input: {}", input);
		RetroLogger.info("Output: {}", output);
		RetroLogger.info("Threads: {}", threads);
		RetroLogger.info("Log level: {}", RetroLogger.getLevel());

		long start = System.nanoTime();

		try {
			RetroLogger.section("Initialization");

			List<ClassTransformer> transformers = Collections
					.<ClassTransformer>singletonList(new AsmRemappingTransformer());

			RetroLogger.info("Loaded {} transformer(s)", transformers.size());

			JarProcessor processor = new JarProcessor(transformers, threads);

			RetroLogger.info("Starting obfuscation...");

			processor.process(input, output);

			long elapsed = (System.nanoTime() - start) / 1_000_000L;

			RetroLogger.section("Completed");

			RetroLogger.info("Output: {}", output);

			RetroLogger.info("Completed in {} ms", elapsed);

		} catch (Exception exception) {

			RetroLogger.error("Obfuscation failed", exception);

			System.exit(1);
		}
	}

	private static void printUsage() {
		System.out.println("Usage: RetroVein " + "<input.jar> " + "<output.jar> " + "[--debug]");
	}
}
