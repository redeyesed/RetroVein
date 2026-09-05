package com.red.retrovein.reflection;

import com.red.retrovein.logging.LogCategory;
import com.red.retrovein.logging.RetroLogger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReflectionAnalyzer {
	private final Map<String, ReflectionCall> calls = new HashMap<String, ReflectionCall>();

	public ReflectionAnalyzer() {
		registerCalls();
	}

	private void registerCalls() {
		/*
		 * Class.forName(String)
		 */
		register("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", 0,
				ReflectionReference.Type.CLASS);

		/*
		 * Class.forName(String, boolean, ClassLoader)
		 */
		register("java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", 0,
				ReflectionReference.Type.CLASS);

		/*
		 * Class.getMethod(String, Class[])
		 */
		register("java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", 0,
				ReflectionReference.Type.METHOD);

		/*
		 * Class.getDeclaredMethod(String, Class[])
		 */
		register("java/lang/Class", "getDeclaredMethod",
				"(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", 0, ReflectionReference.Type.METHOD);

		/*
		 * Class.getField(String)
		 */
		register("java/lang/Class", "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", 0,
				ReflectionReference.Type.FIELD);

		/*
		 * Class.getDeclaredField(String)
		 */
		register("java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", 0,
				ReflectionReference.Type.FIELD);

		/*
		 * ClassLoader.loadClass(String)
		 */
		register("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", 0,
				ReflectionReference.Type.CLASS);
	}

	private void register(String owner, String name, String descriptor, int stringArgumentIndex,
			ReflectionReference.Type type) {
		ReflectionCall call = new ReflectionCall(owner, name, descriptor, stringArgumentIndex, type);

		calls.put(key(owner, name, descriptor), call);
	}

	private ReflectionCall findCall(MethodInsnNode instruction) {
		return calls.get(key(instruction.owner, instruction.name, instruction.desc));
	}

	private String key(String owner, String name, String descriptor) {
		return owner + "#" + name + descriptor;
	}

	public List<ReflectionReference> analyze(String className, byte[] bytecode) {
		List<ReflectionReference> references = new ArrayList<ReflectionReference>();

		ClassReader reader = new ClassReader(bytecode);
		ClassNode classNode = new ClassNode();

		reader.accept(classNode, ClassReader.EXPAND_FRAMES);

		for (Object methodObject : classNode.methods) {

			MethodNode method = (MethodNode) methodObject;

			analyzeMethod(classNode, method, references);
		}

		return references;
	}

	private void analyzeMethod(ClassNode classNode, MethodNode method, List<ReflectionReference> references) {
		if (method.instructions.size() == 0) {
			return;
		}

		Frame[] frames;

		try {

			Analyzer analyzer = new Analyzer(new SourceInterpreter());

			frames = analyzer.analyze(classNode.name, method);

		} catch (AnalyzerException exception) {

			RetroLogger.debug(LogCategory.Transform, "Reflection analysis failed for {}.{}{}: {}", classNode.name,
					method.name, method.desc, exception.getMessage());

			return;
		}

		for (int i = 0; i < method.instructions.size(); i++) {

			AbstractInsnNode instruction = method.instructions.get(i);

			if (!(instruction instanceof MethodInsnNode)) {
				continue;
			}

			MethodInsnNode methodInstruction = (MethodInsnNode) instruction;

			ReflectionCall call = findCall(methodInstruction);

			if (call == null) {
				continue;
			}

			Frame frame = frames[i];

			if (frame == null) {
				continue;
			}

			ReflectionReference reference = resolveReference(classNode, method, methodInstruction, call, frame);

			references.add(reference);
		}
	}

	private ReflectionReference resolveReference(ClassNode classNode, MethodNode method, MethodInsnNode instruction,
			ReflectionCall call, Frame frame) {

		String value = null;

		ReflectionReference.Confidence confidence = ReflectionReference.Confidence.UNKNOWN;

		Type[] argumentTypes = Type.getArgumentTypes(instruction.desc);

		int stringArgumentIndex = call.getStringArgumentIndex();

		if (stringArgumentIndex < argumentTypes.length) {

			int stackOffset = getArgumentStackOffset(argumentTypes, stringArgumentIndex);
			int stackIndex = frame.getStackSize() - getTotalArgumentSlots(argumentTypes) + stackOffset;

			if (stackIndex >= 0 && stackIndex < frame.getStackSize()) {

				Object stackValue = frame.getStack(stackIndex);

				if (stackValue instanceof SourceValue) {

					SourceValue source = (SourceValue) stackValue;

					String constant = resolveConstantString(source);

					if (constant != null) {

						value = constant;

						confidence = ReflectionReference.Confidence.CERTAIN;
					}
				}
			}
		}

		return new ReflectionReference(classNode.name, method.name, method.desc, call.getType(), value, confidence);
	}

	private int getTotalArgumentSlots(Type[] argumentTypes) {
		int result = 0;

		for (int i = 0; i < argumentTypes.length; i++) {

			result += argumentTypes[i].getSize();
		}

		return result;
	}

	private int getArgumentStackOffset(Type[] argumentTypes, int argumentIndex) {
		int result = 0;

		for (int i = 0; i < argumentIndex; i++) {
			result += argumentTypes[i].getSize();
		}

		return result;
	}

	private String resolveConstantString(SourceValue source) {

		if (source == null || source.insns == null || source.insns.size() != 1) {
			return null;
		}

		for (Object instructionObject : source.insns) {

			AbstractInsnNode sourceInstruction = (AbstractInsnNode) instructionObject;

			if (!(sourceInstruction instanceof LdcInsnNode)) {
				return null;
			}

			Object constant = ((LdcInsnNode) sourceInstruction).cst;

			if (!(constant instanceof String)) {
				return null;
			}

			return (String) constant;
		}

		return null;
	}
}
