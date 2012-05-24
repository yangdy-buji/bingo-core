/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bingo.lang.reflect;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bingo.lang.Classes;
import bingo.lang.asm.ClassReader;
import bingo.lang.asm.ClassVisitor;
import bingo.lang.asm.Label;
import bingo.lang.asm.MethodVisitor;
import bingo.lang.asm.Opcodes;
import bingo.lang.asm.Type;
import bingo.lang.logging.Log;
import bingo.lang.logging.LogFactory;

//from spring framework

/**
 * 反射类的元数据
 */
class ReflectMetadata {

	private static final Log log = LogFactory.get(ReflectMetadata.class);

	// marker object for classes that do not have any debug info
	private static final Map<Member, String[]> NO_DEBUG_INFO_MAP = Collections.emptyMap();

	private final Map<Class<?>, Map<Member, String[]>> parameterNamesCache = new ConcurrentHashMap<Class<?>, Map<Member, String[]>>();

	ReflectMetadata(Class<?> clazz) {

	}

	/**
	 * 根据方法获取参数名称。
	 * @param method 欲获取参数名称的方法。
	 * @return 参数名称数组。
	 */
	public String[] getParameterNames(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();
		Map<Member, String[]> map = this.parameterNamesCache.get(declaringClass);
		if (map == null) {
			// initialize cache
			map = inspectClass(declaringClass);
			this.parameterNamesCache.put(declaringClass, map);
		}
		if (map != NO_DEBUG_INFO_MAP) {
			return map.get(method);
		}
		return null;
	}

	/**
	 * 根据构造器获取参数名称。
	 * @param ctor 欲获取参数名称的构造器。
	 * @return 参数名称数组。
	 */
	@SuppressWarnings("unchecked")
	public String[] getParameterNames(Constructor ctor) {
		Class<?> declaringClass = ctor.getDeclaringClass();
		Map<Member, String[]> map = this.parameterNamesCache.get(declaringClass);
		if (map == null) {
			// initialize cache
			map = inspectClass(declaringClass);
			this.parameterNamesCache.put(declaringClass, map);
		}
		if (map != NO_DEBUG_INFO_MAP) {
			return map.get(ctor);
		}

		return null;
	}

	/**
	 * Inspects the target class. Exceptions will be logged and a maker map returned to indicate the lack of debug
	 * information.
	 */
	private Map<Member, String[]> inspectClass(Class<?> clazz) {
		InputStream is = clazz.getResourceAsStream(Classes.getFileName(clazz));
		if (is == null) {
			// We couldn't load the class file, which is not fatal as it
			// simply means this method of discovering parameter names won't work.
			if (log.isDebugEnabled()) {
				log.debug("Cannot find '.class' file for class [" + clazz + "] - unable to determine constructors/methods parameter names");
			}
			return NO_DEBUG_INFO_MAP;
		}
		try {
			ClassReader classReader = new ClassReader(is);
			Map<Member, String[]> map = new ConcurrentHashMap<Member, String[]>();
			classReader.accept(new ParameterNameDiscoveringVisitor(clazz, map), 0);
			return map;
		} catch (IOException ex) {
			if (log.isDebugEnabled()) {
				log.debug("Exception thrown while reading '.class' file for class [" + clazz
				        + "] - unable to determine constructors/methods parameter names", ex);
			}
		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				// ignore
			}
		}
		return NO_DEBUG_INFO_MAP;
	}

	/**
	 * Helper class that inspects all methods (constructor included) and then attempts to find the parameter names for
	 * that member.
	 */
	private static class ParameterNameDiscoveringVisitor extends ClassVisitor {

		private static final String STATIC_CLASS_INIT = "<clinit>";

		private final Class<?> clazz;
		private final Map<Member, String[]> memberMap;

		public ParameterNameDiscoveringVisitor(Class<?> clazz, Map<Member, String[]> memberMap) {
			super(Opcodes.ASM4);
			this.clazz = clazz;
			this.memberMap = memberMap;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			// exclude synthetic + bridged && static class initialization
			if (!isSyntheticOrBridged(access) && !STATIC_CLASS_INIT.equals(name)) {
				return new LocalVariableTableVisitor(clazz, memberMap, name, desc, isStatic(access));
			}
			return null;
		}

		private static boolean isSyntheticOrBridged(int access) {
			return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
		}

		private static boolean isStatic(int access) {
			return ((access & Opcodes.ACC_STATIC) > 0);
		}
	}

	private static class LocalVariableTableVisitor extends MethodVisitor {

		private static final String CONSTRUCTOR = "<init>";

		private final Class<?> clazz;
		private final Map<Member, String[]> memberMap;
		private final String name;
		private final Type[] args;
		private final boolean isStatic;

		private String[] parameterNames;
		private boolean hasLvtInfo = false;

		/*
		 * The nth entry contains the slot index of the LVT table entry holding the argument name for the nth parameter.
		 */
		private final int[] lvtSlotIndex;

		public LocalVariableTableVisitor(Class<?> clazz, Map<Member, String[]> map, String name, String desc, boolean isStatic) {
			super(Opcodes.ASM4);
			this.clazz = clazz;
			this.memberMap = map;
			this.name = name;
			// determine args
			args = Type.getArgumentTypes(desc);
			this.parameterNames = new String[args.length];
			this.isStatic = isStatic;
			this.lvtSlotIndex = computeLvtSlotIndices(isStatic, args);
		}
		
		@Override
		public void visitLocalVariable(String name, String description, String signature, Label start, Label end, int index) {
			this.hasLvtInfo = true;
			for (int i = 0; i < lvtSlotIndex.length; i++) {
				if (lvtSlotIndex[i] == index) {
					this.parameterNames[i] = name;
				}
			}
		}

		@Override
		public void visitEnd() {
			if (this.hasLvtInfo || (this.isStatic && this.parameterNames.length == 0)) {
				// visitLocalVariable will never be called for static no args methods
				// which doesn't use any local variables.
				// This means that hasLvtInfo could be false for that kind of methods
				// even if the class has local variable info.
				memberMap.put(resolveMember(), parameterNames);
			}
		}

		private Member resolveMember() {
			ClassLoader loader = clazz.getClassLoader();
			Class<?>[] classes = new Class<?>[args.length];

			// resolve args
			for (int i = 0; i < args.length; i++) {
				classes[i] = Classes.forName(loader, args[i].getClassName());
			}
			try {
				if (CONSTRUCTOR.equals(name)) {
					return clazz.getDeclaredConstructor(classes);
				}

				return clazz.getDeclaredMethod(name, classes);
			} catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Method [" + name + "] was discovered in the .class file but cannot be resolved in the class object",
				        ex);
			}
		}

		private static int[] computeLvtSlotIndices(boolean isStatic, Type[] paramTypes) {
			int[] lvtIndex = new int[paramTypes.length];
			int nextIndex = (isStatic ? 0 : 1);
			for (int i = 0; i < paramTypes.length; i++) {
				lvtIndex[i] = nextIndex;
				if (isWideType(paramTypes[i])) {
					nextIndex += 2;
				} else {
					nextIndex++;
				}
			}
			return lvtIndex;
		}

		private static boolean isWideType(Type aType) {
			// float is not a wide type
			return (aType == Type.LONG_TYPE || aType == Type.DOUBLE_TYPE);
		}
	}
}
