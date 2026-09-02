package com.red.retrovein.logging;

public final class ObfuscationStats {
	private int classesScanned;
	private int classesRenamed;
	private int classesSkipped;

	private int methodsScanned;
	private int methodsRenamed;
	private int methodsSkipped;

	private int fieldsScanned;
	private int fieldsRenamed;
	private int fieldsSkipped;

	private int localVariablesScanned;
	private int localVariablesRenamed;
	private int localVariablesSkipped;

	private int resourcesScanned;
	private int resourcesTransformed;

	private int classesTransformed;

	public void classScanned() {
		classesScanned++;
	}

	public void classRenamed() {
		classesRenamed++;
	}

	public void classSkipped() {
		classesSkipped++;
	}

	public void classTransformed() {
		classesTransformed++;
	}

	public void methodScanned() {
		methodsScanned++;
	}

	public void methodRenamed() {
		methodsRenamed++;
	}

	public void methodSkipped() {
		methodsSkipped++;
	}

	public void fieldScanned() {
		fieldsScanned++;
	}

	public void fieldRenamed() {
		fieldsRenamed++;
	}

	public void fieldSkipped() {
		fieldsSkipped++;
	}

	public void localVariableScanned() {
		localVariablesScanned++;
	}

	public void localVariableRenamed() {
		localVariablesRenamed++;
	}

	public void localVariableSkipped() {
		localVariablesSkipped++;
	}

	public void resourceScanned() {
		resourcesScanned++;
	}

	public void resourceTransformed() {
		resourcesTransformed++;
	}

	public int getClassesScanned() {
		return classesScanned;
	}

	public int getClassesRenamed() {
		return classesRenamed;
	}

	public int getClassesSkipped() {
		return classesSkipped;
	}

	public int getClassesTransformed() {
		return classesTransformed;
	}

	public int getMethodsScanned() {
		return methodsScanned;
	}

	public int getMethodsRenamed() {
		return methodsRenamed;
	}

	public int getMethodsSkipped() {
		return methodsSkipped;
	}

	public int getFieldsScanned() {
		return fieldsScanned;
	}

	public int getFieldsRenamed() {
		return fieldsRenamed;
	}

	public int getFieldsSkipped() {
		return fieldsSkipped;
	}

	public int getLocalVariablesScanned() {
		return localVariablesScanned;
	}

	public int getLocalVariablesRenamed() {
		return localVariablesRenamed;
	}

	public int getLocalVariablesSkipped() {
		return localVariablesSkipped;
	}

	public int getResourcesScanned() {
		return resourcesScanned;
	}

	public int getResourcesTransformed() {
		return resourcesTransformed;
	}

	public void printSummary() {
		RetroLogger.info(LogCategory.Main, "Obfuscation Summary");

		RetroLogger.info("Classes: {} scanned, {} renamed, {} skipped, {} transformed", classesScanned, classesRenamed,
				classesSkipped, classesTransformed);
		RetroLogger.info("Methods: {} scanned, {} renamed, {} skipped", methodsScanned, methodsRenamed, methodsSkipped);
		RetroLogger.info("Fields: {} scanned, {} renamed, {} skipped", fieldsScanned, fieldsRenamed, fieldsSkipped);
		RetroLogger.info("Local variables: {} scanned, {} renamed, {} skipped", localVariablesScanned,
				localVariablesRenamed, localVariablesSkipped);
		RetroLogger.info("Resources: {} scanned, {} transformed", resourcesScanned, resourcesTransformed);
	}
}
