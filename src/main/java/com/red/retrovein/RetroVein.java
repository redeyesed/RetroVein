package com.red.retrovein;

import com.red.retrovein.config.ConfigLoader;
import com.red.retrovein.config.RetroConfig;
import com.red.retrovein.io.JarProcessor;
import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.LogLevel;
import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.transform.AsmRemappingTransformer;
import com.red.retrovein.transform.ClassTransformer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class RetroVein {
	private static final String VERSION = "0.1.0";
	private static final String API_VERSION = "1.0";

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

		RetroConfig config;
		try {
			config = ConfigLoader.loadConfig();
		} catch (Exception exception) {
			RetroLogger.error(LogCategory.Main, "Failed to load configuration", exception);
			System.exit(1);
			return;
		}

		Path input = Paths.get(args[0]);
		Path output = Paths.get(args[1]);

		validatePaths(input, output);

		RetroLogger.info("RetroVein {} (API version {})", VERSION, API_VERSION);
		RetroLogger.info(LogCategory.Main, "Input: {}", input);
		RetroLogger.info(LogCategory.Main, "Output: {}", output);
		RetroLogger.info(LogCategory.Main, "Threads: {}", config.getCoreThreads());
		RetroLogger.debug(LogCategory.Main, "Mapping classes: {}", config.isMappingClasses());
		RetroLogger.debug(LogCategory.Main, "Mapping fields: {}", config.isMappingFields());
		RetroLogger.debug(LogCategory.Main, "Mapping methods: {}", config.isMappingMethods());
		RetroLogger.debug(LogCategory.Main, "Mapping local variables: {}", config.isMappingLocalVariables());
		RetroLogger.debug(LogCategory.Main, "Mapping enums: {}", config.isMappingEnums());
		RetroLogger.debug(LogCategory.Main, "Package remapping: {}", config.isPackageRemap());
		RetroLogger.debug(LogCategory.Main, "ASM remapping transformer: {}", config.isAsmRemappingTransformer());

		long start = System.nanoTime();

		try {
			List<ClassTransformer> transformers = new ArrayList<ClassTransformer>();

			if (config.isAsmRemappingTransformer()) {
				transformers.add(new AsmRemappingTransformer());
			}

			RetroLogger.debug(LogCategory.Main, "Loaded {} transformer(s)", transformers.size());

			JarProcessor processor = new JarProcessor(transformers, config.getCoreThreads(), config);

			RetroLogger.info(LogCategory.Main, "Starting obfuscation...");

			processor.process(input, output);

			long elapsed = (System.nanoTime() - start) / 1_000_000L;

			RetroLogger.info("Completed in ({}ms)", elapsed);

		} catch (Exception exception) {
			RetroLogger.error(LogCategory.Main, "Obfuscation failed", exception);

			System.exit(1);
		}
	}

	private static void validatePaths(Path inputJar, Path outputJar) {
		Path absoluteInput = inputJar.toAbsolutePath().normalize();
		Path absoluteOutput = outputJar.toAbsolutePath().normalize();
		if (absoluteInput.equals(absoluteOutput)) {
			throw new IllegalArgumentException("Input and output files must be different.");
		}
	}

	private static void printUsage() {
		System.out.println("Usage: RetroVein <input.jar> <output.jar> [--debug]");
	}
}
