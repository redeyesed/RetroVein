package com.red.retrovein.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarClassScanner {
	public List<String> scan(JarFile jar) throws IOException {

		List<String> classes = new ArrayList<String>();

		Enumeration<JarEntry> entries = jar.entries();

		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			if (entry.isDirectory()) {
				continue;
			}

			if (!entry.getName().endsWith(".class")) {
				continue;
			}

			String className = entry.getName().substring(0, entry.getName().length() - ".class".length());

			classes.add(className);
		}

		return classes;
	}
}
