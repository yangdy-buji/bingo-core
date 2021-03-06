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
package bingo.lang;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import bingo.lang.exceptions.ObjectNotFoundException;
import bingo.lang.exceptions.ReflectException;
import bingo.lang.exceptions.UncheckedIOException;
import bingo.lang.logging.Log;
import bingo.lang.logging.LogFactory;
import bingo.lang.resource.Resource;
import bingo.lang.resource.Resources;

//from apache commons-lang3

/**
 * <code>null</code> safe {@link Class} utility.
 */
public class Classes {
	
	private static final Log log = LogFactory.get(Classes.class);
	
	/**
	 * Maps a primitive class name to its corresponding abbreviation used in array class names.
	 */
	private static final Map<String, String>	abbreviationMap	= new HashMap<String, String>();

	/**
	 * Maps an abbreviation used in array class names to corresponding primitive class name.
	 */
	private static final Map<String, String>	reverseAbbreviationMap	= new HashMap<String, String>();

	/**
	 * <p>
	 * The package separator character: <code>'&#x2e;' == {@value}</code>.
	 * </p>
	 */
	public static final char PACKAGE_SEPARATOR_CHAR = '.';

	/**
	 * <p>
	 * The package separator String: {@code "&#x2e;"}.
	 * </p>
	 */
	public static final String PACKAGE_SEPARATOR = String.valueOf(PACKAGE_SEPARATOR_CHAR);

	/**
	 * <p>
	 * The inner class separator character: <code>'$' == {@value}</code>.
	 * </p>
	 */
	public static final char	INNER_CLASS_SEPARATOR_CHAR	= '$';

	/**
	 * <p>
	 * The inner class separator String: {@code "$"}.
	 * </p>
	 */
	public static final String INNER_CLASS_SEPARATOR = String.valueOf(INNER_CLASS_SEPARATOR_CHAR);
	
	/** The ".class" file suffix */
	public static final String CLASS_FILE_SUFFIX = ".class";	

	/**
	 * Add primitive type abbreviation to maps of abbreviations.
	 * 
	 * @param primitive Canonical name of primitive type
	 * @param abbreviation Corresponding abbreviation of primitive type
	 */
	private static void addAbbreviation(String primitive, String abbreviation) {
		abbreviationMap.put(primitive, abbreviation);
		reverseAbbreviationMap.put(abbreviation, primitive);
	}

	/**
	 * Feed abbreviation maps
	 */
	static {
		addAbbreviation("int", "I");
		addAbbreviation("boolean", "Z");
		addAbbreviation("float", "F");
		addAbbreviation("long", "J");
		addAbbreviation("short", "S");
		addAbbreviation("byte", "B");
		addAbbreviation("double", "D");
		addAbbreviation("char", "C");
	}

	protected Classes() {

	}
	
	//new instance
	//----------------------------------------------------------------------
	public static Object newInstance(String className) throws ObjectNotFoundException,ReflectException {
		return Reflects.newInstance(forName(className));
	}
	
	public static Object newInstance(Class<?> loaderClass, String className) throws ObjectNotFoundException,ReflectException {
		return Reflects.newInstance(forName(loaderClass,className));
	}

	public static Object newInstance(Class<?> clazz) throws ReflectException {
		return Reflects.newInstance(clazz);
	}
	
	//primitives
    /**
     * <p>Converts the specified primitive Class object to its corresponding
     * wrapper Class object.</p>
     *
     * @param cls  the class to convert, may be null
     * @return the wrapper class for {@code cls} or {@code cls} if
     * {@code cls} is not a primitive. {@code null} if null input.
     */
    public static Class<?> primitiveToWrapper(Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = Primitives.wrap(cls);
        }
        return convertedClass;
    }	
    
    /**
     * <p>Converts the specified wrapper class to its corresponding primitive
     * class.</p>
     *
     * @param cls the class to convert, may be <b>null</b>
     * @return the corresponding primitive type if {@code cls} is a
     * wrapper class, <b>itself</b> otherwise
     */
    public static Class<?> wrapperToPrimitive(Class<?> cls) {
        return Primitives.unwrap(cls);
    }
    
    /**
     * Returns whether the given {@code type} is a primitive or primitive wrapper ({@link Boolean}, {@link Byte}, {@link Character},
     * {@link Short}, {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
     * 
     * @param type
     *            The class to query or null.
     * @return true if the given {@code type} is a primitive or primitive wrapper ({@link Boolean}, {@link Byte}, {@link Character},
     *         {@link Short}, {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
     */
    public static boolean isPrimitiveOrWrapper(Class<?> type) {
        if (type == null) {
            return false;
        }
        return type.isPrimitive() || isPrimitiveWrapper(type);
    }

    /**
     * Returns whether the given {@code type} is a primitive wrapper ({@link Boolean}, {@link Byte}, {@link Character}, {@link Short},
     * {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
     * 
     * @param type
     *            The class to query or null.
     * @return true if the given {@code type} is a primitive wrapper ({@link Boolean}, {@link Byte}, {@link Character}, {@link Short},
     *         {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
     */
    public static boolean isPrimitiveWrapper(Class<?> type) {
        return Primitives.isWrapperType(type);
    }    
	
	//Class scan
	//-----------------------------------------------------------------------
	public static Set<Class<?>> scan(String basePackage,String classpathPattern) throws UncheckedIOException {
		Assert.notEmpty(basePackage,	 "basePackage must not be empty");
		Assert.notEmpty(classpathPattern,"classLocationPattern must not be empty");
		
		String basePath = basePackage.replace('.', '/');
		String scanPath = Resources.CLASSPATH_ALL_URL_PREFIX + basePath + "/" + classpathPattern + ".class";
		
		StopWatch sw = StopWatch.startNew();
		
		Resource[] resources = Resources.scanQuietly(scanPath);
		
		Set<Class<?>> classes = new HashSet<Class<?>>();
		
		try {
	        for(Resource resource : resources) {
	        	if(resource.isReadable()){
	        		classes.add(classForResource(resource,basePath));
	        	}
	        }
        } catch (IOException e) {
        	Exceptions.uncheck(e,"Error scanning package '{0}'",basePackage);
        }
        
        sw.stop();
        
        log.debug("scan {} classes in package '{}' used {}ms",classes.size(),basePackage,sw.getElapsedMilliseconds());
		
		return classes;
	}
	
	//Class Loader
	//-----------------------------------------------------------------------
	/**
	 * Return the default ClassLoader to use: typically the thread context ClassLoader, if available; the ClassLoader
	 * that loaded the Classes class will be used as fallback.
	 * <p>
	 * Call this method if you intend to use the thread context ClassLoader in a scenario where you absolutely need a
	 * non-null ClassLoader reference: for example, for class path resource loading (but not necessarily for
	 * <code>Class.forName</code>, which accepts a <code>null</code> ClassLoader reference as well).
	 * 
	 * @return the default ClassLoader (never <code>null</code>)
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public static ClassLoader getClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back to system class loader...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = Classes.class.getClassLoader();
		}
		return cl;
	}
	
	public static ClassLoader getClassLoader(Class<?> clazz) {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back to system class loader...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = clazz.getClassLoader();
		}
		return cl;
	}

	// class & package name 
	// ----------------------------------------------------------------------	
	/**
	 * Determine the name of the class file, relative to the containing
	 * package: e.g. "String.class"
	 * @param clazz the class
	 * @return the file name of the ".class" file
	 */
	public static String getFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}
	
	/**
	 * <p>
	 * Gets the class name minus the package name from a {@code Class}.
	 * </p>
	 * 
	 * <p>
	 * Consider using the Java 5 API {@link Class#getSimpleName()} instead. The one known difference is that this code
	 * will return {@code "Map.Entry"} while the {@code java.lang.Class} variant will simply return {@code "Entry"}.
	 * </p>
	 * 
	 * @param cls the class to get the short name for.
	 * 
	 * @return the class name without the package name or an empty string
	 */
	public static String getShortName(Class<?> cls) {
		if (cls == null) {
			return Strings.EMPTY;
		}
		return getShortName(cls.getName());
	}

	/**
	 * <p>
	 * Gets the class name minus the package name from a String.
	 * </p>
	 * 
	 * <p>
	 * The string passed in is assumed to be a class name - it is not checked.
	 * </p>
	 * 
	 * <p>
	 * Note that this method differs from Class.getSimpleName() in that this will return {@code "Map.Entry"} whilst the
	 * {@code java.lang.Class} variant will simply return {@code "Entry"}.
	 * </p>
	 * 
	 * @param className the className to get the short name for
	 * @return the class name of the class without the package name or an empty string
	 */
	public static String getShortName(String className) {
		if (className == null) {
			return Strings.EMPTY;
		}
		if (className.length() == 0) {
			return Strings.EMPTY;
		}

		StringBuilder arrayPrefix = new StringBuilder();

		// Handle array encoding
		if (className.startsWith("[")) {
			while (className.charAt(0) == '[') {
				className = className.substring(1);
				arrayPrefix.append("[]");
			}
			// Strip Object type encoding
			if (className.charAt(0) == 'L' && className.charAt(className.length() - 1) == ';') {
				className = className.substring(1, className.length() - 1);
			}
		}

		if (reverseAbbreviationMap.containsKey(className)) {
			className = reverseAbbreviationMap.get(className);
		}

		int lastDotIdx = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR);
		int innerIdx = className.indexOf(INNER_CLASS_SEPARATOR_CHAR, lastDotIdx == -1 ? 0 : lastDotIdx + 1);
		String out = className.substring(lastDotIdx + 1);
		if (innerIdx != -1) {
			out = out.replace(INNER_CLASS_SEPARATOR_CHAR, PACKAGE_SEPARATOR_CHAR);
		}
		return out + arrayPrefix;
	}
	
	/**
	 * Given an input class object, return a string which consists of the
	 * class's package name as a pathname, i.e., all dots ('.') are replaced by
	 * slashes ('/'). Neither a leading nor trailing slash is added. The result
	 * could be concatenated with a slash and the name of a resource and fed
	 * directly to <code>ClassLoader.getResource()</code>. For it to be fed to
	 * <code>Class.getResource</code> instead, a leading slash would also have
	 * to be prepended to the returned value.
	 * @param clazz the input class. A <code>null</code> value or the default
	 * (empty) package will result in an empty string ("") being returned.
	 * @return a path which represents the package name
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String getPackageAsResourcePath(Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf('.');
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace('.', '/');
	}

	// Class loading
	// ----------------------------------------------------------------------
	/**
	 * Returns the (initialized) class represented by {@code className} using the current thread's context class loader.
	 * This implementation supports the syntaxes "{@code java.util.Map.Entry[]}", "{@code java.util.Map$Entry[]}", "
	 * {@code [Ljava.util.Map.Entry;}", and "{@code [Ljava.util.Map$Entry;}".
	 * 
	 * @param className the class name
	 * @return the class represented by {@code className} using the current thread's context class loader
	 * 
	 * @throws ObjectNotFoundException if the class is not found
	 */
	public static Class<?> forName(String className) throws ObjectNotFoundException {
		return forName(className, true);
	}
	
	public static Class<?> forNameOrNull(String className) {
		try {
	        return forName(className, true);
        } catch (ObjectNotFoundException e) {
        	return null;
        }
	}
	
	/**
	 * Returns the class represented by {@code className} using the current thread's context class loader. This
	 * implementation supports the syntaxes "{@code java.util.Map.Entry[]}", "{@code java.util.Map$Entry[]}", "{@code
	 * [Ljava.util.Map.Entry;}", and "{@code [Ljava.util.Map$Entry;}".
	 * 
	 * @param className the class name
	 * @param initialize whether the class must be initialized
	 * @return the class represented by {@code className} using the current thread's context class loader
	 * @throws ObjectNotFoundException if the class is not found
	 */
	static Class<?> forName(String className, boolean initialize) throws ObjectNotFoundException {
		ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
		ClassLoader loader = contextCL == null ? Classes.class.getClassLoader() : contextCL;
		return forName(loader, className, initialize);
	}	
	
	/**
	 * Returns the (initialized) class represented by {@code className} using the {@code classLoader}. This
	 * implementation supports the syntaxes "{@code java.util.Map.Entry[]}", "{@code java.util.Map$Entry[]}", "{@code
	 * [Ljava.util.Map.Entry;}", and "{@code [Ljava.util.Map$Entry;}".
	 * 
	 * @param classLoader the class loader to use to load the class
	 * @param className the class name
	 * @return the class represented by {@code className} using the {@code classLoader}
	 * @throws ObjectNotFoundException if the class is not found
	 */
	public static Class<?> forName(ClassLoader classLoader, String className) throws ObjectNotFoundException {
		return forName(classLoader, className, true);
	}
	
	public static Class<?> forName(Class<?> loaderClass,String className) throws ObjectNotFoundException {
		return forName(getClassLoader(loaderClass),className);
	}
	
	public static Class<?> forNameOrNull(ClassLoader classLoader, String className) {
		try {
	        return forName(classLoader, className, true);
        } catch (ObjectNotFoundException e) {
        	return null;
        }
	}
	
	public static Class<?> forNameOrNull(Class<?> loaderClass, String className) {
		try {
	        return forName(loaderClass, className);
        } catch (ObjectNotFoundException e) {
        	return null;
        }
	}
	
    // Inner class
    // ----------------------------------------------------------------------
    /**
     * <p>Is the specified class an inner class or static nested class.</p>
     *
     * @param cls  the class to check, may be null
     * @return {@code true} if the class is an inner or static nested class,
     *  false if not or {@code null}
     */
    public static boolean isInner(Class<?> cls) {
        return cls != null && cls.getEnclosingClass() != null;
    }
	
	/**
	 * Returns the class represented by {@code className} using the {@code classLoader}. This implementation supports
	 * the syntaxes "{@code java.util.Map.Entry[]}", "{@code java.util.Map$Entry[]}", "{@code [Ljava.util.Map.Entry;}
	 * ", and "{@code [Ljava.util.Map$Entry;}".
	 * 
	 * @param classLoader the class loader to use to load the class
	 * @param className the class name
	 * @param initialize whether the class must be initialized
	 * @return the class represented by {@code className} using the {@code classLoader}
	 * @throws ClassNotFoundException if the class is not found
	 */
	static Class<?> forName(ClassLoader classLoader, String className, boolean initialize) throws ObjectNotFoundException {
		try {
			Class<?> clazz;
			if (abbreviationMap.containsKey(className)) {
				String clsName = "[" + abbreviationMap.get(className);
				clazz = Class.forName(clsName, initialize, classLoader).getComponentType();
			} else {
				clazz = Class.forName(toCanonicalName(className), initialize, classLoader);
			}
			return clazz;
		} catch (ClassNotFoundException ex) {
			// allow path separators (.) as inner class name separators
			int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR);

			if (lastDotIndex != -1) {
				try {
					return forName(classLoader, className.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR_CHAR
					        + className.substring(lastDotIndex + 1), initialize);
				} catch (ObjectNotFoundException ex2) { // NOPMD
					// ignore exception
				}
			}

			throw new ObjectNotFoundException("class '{0}' not found",className,ex);
		}
	}
	
	/**
	 * Checks if given class is a concrete one; that is, not an interface or abstract class.
	 */
	public static boolean isConcrete(Class<?> clazz) {
		if(null == clazz){
			return false;
		}
		
		return ! (Modifier.isInterface(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers())); 
		
	}
	
	/**
	 * Checks if given class is a simple type.
	 */
	public static boolean isSimple(Class<?> clazz) {
		return null != clazz &&
				    (clazz.isPrimitive() || 
					Primitives.isWrapperType(clazz) || 
					clazz.isEnum() ||
					Number.class.isAssignableFrom(clazz) ||
					CharSequence.class.isAssignableFrom(clazz) ||
					Date.class.isAssignableFrom(clazz) ||
					Class.class.isAssignableFrom(clazz));
	}
	
	public static boolean isEnumerable(Class<?> clazz) {
		return null != clazz && 
				(clazz.isArray() || 
			     Iterable.class.isAssignableFrom(clazz) ||
			     Iterator.class.isAssignableFrom(clazz) ||
			     Enumeration.class.isAssignableFrom(clazz) 
			     );
	}
	
    /**
     * <p>Checks if one {@code Class} can be assigned to a variable of
     * another {@code Class}.</p>
     *
     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
     * this method takes into account widenings of primitive classes and
     * {@code null}s.</p>
     *
     * <p>Primitive widenings allow an int to be assigned to a long, float or
     * double. This method returns the correct result for these cases.</p>
     *
     * <p>{@code Null} may be assigned to any reference type. This method
     * will return {@code true} if {@code null} is passed in and the
     * toClass is non-primitive.</p>
     *
     * <p>Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
     *
     * @param cls  the Class to check, may be null
     * @param toClass  the Class to try to assign into, returns false if null
     * @return {@code true} if assignment possible
     */
    public static boolean isAssignable(Class<?> cls, Class<?> toClass) {
        if (toClass == null) {
            return false;
        }
        // have to check for null, as isAssignableFrom doesn't
        if (cls == null) {
            return !toClass.isPrimitive();
        }
        //autoboxing:
        if (cls.isPrimitive() && !toClass.isPrimitive()) {
            cls = Primitives.wrap(cls);
            if (cls == null) {
                return false;
            }
        }
        if (toClass.isPrimitive() && !cls.isPrimitive()) {
            cls = Primitives.unwrap(cls);
            if (cls == null) {
                return false;
            }
        }
        if (cls.equals(toClass)) {
            return true;
        }
        if (cls.isPrimitive()) {
            if (toClass.isPrimitive() == false) {
                return false;
            }
            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                    || Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                    || Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                    || Integer.TYPE.equals(toClass)
                    || Long.TYPE.equals(toClass)
                    || Float.TYPE.equals(toClass)
                    || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }
        return toClass.isAssignableFrom(cls);
    }	
	
	public static boolean isString(Class<?> clazz){
		return null == clazz ? false : clazz.equals(String.class);
	}
	
	public static boolean isBoolean(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class);
	}
	
	public static boolean isDouble(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Double.TYPE) || clazz.equals(Double.class);
	}
	
	public static boolean isInteger(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Integer.TYPE) || clazz.equals(Integer.class);
	}
	
	public static boolean isLong(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Long.TYPE) || clazz.equals(Long.class);
	}
	
	public static boolean isShort(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Short.TYPE) || clazz.equals(Short.class);
	}
	
	public static boolean isFloat(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Float.TYPE) || clazz.equals(Float.class);
	}
	
	public static boolean isBigDecimal(Class<?> clazz){
		return null == clazz ? false : clazz.equals(BigDecimal.class);
	}
	
	public static boolean isBigInteger(Class<?> clazz){
		return null == clazz ? false : clazz.equals(BigInteger.class);
	}
	
	public static boolean isCharacter(Class<?> clazz){
		return null == clazz ? false : clazz.equals(Character.TYPE) || clazz.equals(Character.class);
	}

	// Private Methods
	// ----------------------------------------------------------------------
	/**
	 * Converts a class name to a JLS style class name.
	 * 
	 * @param className the class name
	 * @return the converted name
	 */
	private static String toCanonicalName(String className) {
		className = Strings.trim(className);

		if (className.endsWith("[]")) {
			StringBuilder classNameBuffer = new StringBuilder();
			while (className.endsWith("[]")) {
				className = className.substring(0, className.length() - 2);
				classNameBuffer.append("[");
			}
			String abbreviation = abbreviationMap.get(className);
			if (abbreviation != null) {
				classNameBuffer.append(abbreviation);
			} else {
				classNameBuffer.append("L").append(className).append(";");
			}
			className = classNameBuffer.toString();
		}
		return className;
	}
	
    private static Class<?> classForResource(Resource resource,String basePath) throws IOException,ObjectNotFoundException {
        String fullPath = resource.getURL().toString();
        
        int baseIndex = fullPath.lastIndexOf(basePath + "/");
        
        if(baseIndex <= 0){
            throw new IllegalStateException("invalid resource '" + fullPath + "', can not found base path '" + basePath);
        }
        
        String classFile = fullPath.substring(baseIndex);
        String className = classFile.substring(0,classFile.indexOf(".class")).replace('/', '.');
        
        return forName(className);
    }	
}
