package com.red.retrovein.transform;

import com.red.retrovein.logging.RetroLogger;
import com.red.retrovein.mapping.Mapping;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class ReflectionRemapper {
	private final Mapping mapping;

	public ReflectionRemapper(Mapping mapping) {
		this.mapping = mapping;
	}

	/*
	 * Проходим по всем инструкциям метода и ищем вызовы reflection API, для которых
	 * можно заранее определить используемое имя класса.
	 */
	public void remapReflection(MethodNode method) {
		for (AbstractInsnNode instruction = method.instructions
				.getFirst(); instruction != null; instruction = instruction.getNext()) {
			if (!(instruction instanceof MethodInsnNode)) {
				continue;
			}
			MethodInsnNode methodCall = (MethodInsnNode) instruction;
			if (isClassForName(methodCall)) {
				remapClassName(methodCall);
				continue;
			}
			if (isClassLoaderLoadClass(methodCall)) {
				remapClassName(methodCall);
			}
		}
	}

	/*
	 * Проверяем вызов Class.forName(String), который используется для
	 * получения класса по его полному имени, указанному в строке.
	 */
	private boolean isClassForName(MethodInsnNode instruction) {
		return instruction.getOpcode() == Opcodes.INVOKESTATIC
				&& "java/lang/Class".equals(instruction.owner)
				&& "forName".equals(instruction.name)
				&& "(Ljava/lang/String;)Ljava/lang/Class;".equals(instruction.desc);
	}

	/*
	 * Проверяем вызов ClassLoader.loadClass(String), выполняющий загрузку
	 * класса по имени через экземпляр загрузчика классов.
	 */
	private boolean isClassLoaderLoadClass(MethodInsnNode instruction) {
		return instruction.getOpcode() == Opcodes.INVOKEVIRTUAL
				&& "java/lang/ClassLoader".equals(instruction.owner)
				&& "loadClass".equals(instruction.name)
				&& "(Ljava/lang/String;)Ljava/lang/Class;".equals(instruction.desc);
	}

	/*
	 * Находим строковую константу, переданную в reflection-вызов, и
	 * заменяем имя класса только в том случае, если для него существует mapping.
	 */
	private void remapClassName(MethodInsnNode instruction) {
		AbstractInsnNode previous = getPreviousInstruction(instruction);
		if (!(previous instanceof LdcInsnNode)
				|| !(((LdcInsnNode) previous).cst instanceof String)) {
			return;
		}
		LdcInsnNode constant = (LdcInsnNode) previous;
		String originalName = (String) constant.cst;
		String mappedName = mapping.getClassName(originalName.replace('.', '/'));
		if (mappedName == null || originalName.equals(mappedName.replace('/', '.'))) {
			return;
		}
		String result = mappedName.replace('/', '.');
		RetroLogger.debug("Reflection class transformed: {} -> {}", originalName, result);
		constant.cst = result;
	}

	/*
	 * Ищем предыдущую значимую инструкцию, пропуская служебные элементы ASM,
	 * которые не влияют на значение, переданное в reflection-вызов.
	 */
	private AbstractInsnNode getPreviousInstruction(AbstractInsnNode instruction) {
		AbstractInsnNode current = instruction.getPrevious();
		while (current != null) {
			int type = current.getType();
			if (type != AbstractInsnNode.LABEL
					&& type != AbstractInsnNode.LINE
					&& type != AbstractInsnNode.FRAME) {
				return current;
			}
			current = current.getPrevious();
		}
		return null;
	}
}
