package com.red.retrovein;

import com.red.retrovein.io.JarProcessor;
import com.red.retrovein.transform.AsmClassRenamer;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RetroVein {
	private RetroVein() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Usage: retrovein <input.jar> <output.jar>");
			System.exit(1);
		}

		Path input = Paths.get(args[0]);
		Path output = Paths.get(args[1]);

		int threads = Runtime.getRuntime().availableProcessors();

		System.out.println("RetroVein 0.1.0");
		System.out.println("Input: " + input);
		System.out.println("Output: " + output);
		System.out.println("Threads: " + threads);

		long start = System.nanoTime();

		JarProcessor processor = new JarProcessor(new AsmClassRenamer(), threads);

		processor.process(input, output);

		long elapsed = System.nanoTime() - start;

		System.out.println("Completed in " + (elapsed / 1_000_000L) + " ms");
	}
}
