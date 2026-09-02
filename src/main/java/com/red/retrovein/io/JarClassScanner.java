package com.red.retrovein.io;

import com.red.retrovein.logging.RetroLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarClassScanner {
	public List<ClassInfo> scan(JarFile jar) throws IOException {
		RetroLogger.info("Scanning JAR for classes...");

		List<ClassInfo> classes = new ArrayList<ClassInfo>();

		Enumeration<JarEntry> entries = jar.entries();

		int totalEntries = 0;

		while (entries.hasMoreElements()) {

			JarEntry entry = entries.nextElement();

			totalEntries++;

			if (entry.isDirectory()) {
				continue;
			}

			if (!entry.getName().endsWith(".class")) {
				continue;
			}

			String className = entry.getName().substring(0, entry.getName().length() - ".class".length());

			try (InputStream inputStream = jar.getInputStream(entry)) {

				classes.add(new ClassInfo(className, readAll(inputStream)));
			}

			RetroLogger.debug("Scanned class: {}", className);
		}

		RetroLogger.info("Scanned {} JAR entries", totalEntries);

		RetroLogger.info("Found {} classes", classes.size());

		return classes;
	}

	private static byte[] readAll(InputStream inputStream) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		byte[] buffer = new byte[8192];

		int count;

		while ((count = inputStream.read(buffer)) != -1) {

			output.write(buffer, 0, count);
		}

		return output.toByteArray();
	}
}
