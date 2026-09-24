package com.red.retrovein.config;

public final class RetroConfig {
	private final boolean mappingClasses;
	private final boolean mappingFields;
	private final boolean mappingMethods;
	private final boolean mappingLocalVariables;
	private final boolean mappingEnums;
	private final boolean packageRemap;
	private final boolean asmRemappingTransformer;
	private final int coreThreads;

	public RetroConfig(boolean mappingClasses, boolean mappingFields, boolean mappingMethods,
			boolean mappingLocalVariables, boolean mappingEnums, boolean packageRemap, boolean asmRemappingTransformer,
			int coreThreads) {
		this.mappingClasses = mappingClasses;
		this.mappingFields = mappingFields;
		this.mappingMethods = mappingMethods;
		this.mappingLocalVariables = mappingLocalVariables;
		this.mappingEnums = mappingEnums;
		this.packageRemap = packageRemap;
		this.asmRemappingTransformer = asmRemappingTransformer;
		this.coreThreads = coreThreads;
	}

	/**
	 * Создаёт конфигурацию со стандартными значениями. По умолчанию все функции
	 * маппинга и трансформации включены, для обработки используется 4 потока.
	 */
	public static RetroConfig createDefault() {
		return new RetroConfig(true, true, true, true, true, true, true, 4);
	}

	public boolean isMappingClasses() {
		return mappingClasses;
	}

	public boolean isMappingFields() {
		return mappingFields;
	}

	public boolean isMappingMethods() {
		return mappingMethods;
	}

	public boolean isMappingLocalVariables() {
		return mappingLocalVariables;
	}

	public boolean isMappingEnums() {
		return mappingEnums;
	}

	public boolean isPackageRemap() {
		return packageRemap;
	}

	public boolean isAsmRemappingTransformer() {
		return asmRemappingTransformer;
	}

	public int getCoreThreads() {
		return coreThreads;
	}
}
