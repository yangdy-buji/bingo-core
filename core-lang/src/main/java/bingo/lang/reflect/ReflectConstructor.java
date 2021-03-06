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

import java.lang.reflect.Constructor;

import bingo.lang.exceptions.ReflectException;

/**
 * 反射类的构造器。
 * @param <T> 
 */
public class ReflectConstructor<T> extends ReflectMember {

	/**
	 * JDK中的 {@link Constructor}。
	 */
	private final Constructor<T> javaConstructor;
	
	/**
	 * 该构造器的参数列表。
	 */
	private ReflectParameter[]   parameters;
	
	/**
	 * 通过对应的反射类和JDK中的 {@link Constructor}实例初始化。
	 * @param reflectClass 所对应的反射类。
	 * @param javaConstructor JDK中的 {@link Constructor}实例。
	 */
	protected ReflectConstructor(ReflectClass<T> reflectClass, Constructor<T> javaConstructor){
		super(reflectClass,javaConstructor);

		this.javaConstructor = javaConstructor;
		
		this.initialize();
	}
	
	/**
	 * 获得此反射类的构造器的名称。
	 */
	public String getName() {
	    return javaConstructor.getName();
    }
	
	public ReflectParameter[] getParameters(){
		return parameters;
	}
	
	/**
	 * 获取此 {@link ReflectConstructor}所包裹的Java原生的 {@link Constructor}。
	 * @return 此 {@link ReflectConstructor}所包裹的Java原生的 {@link Constructor}。
	 */
	public Constructor<T> getJavaConstructor(){
		return this.javaConstructor;
	}
	
	/**
	 * 根据传入参数实例化 {@link ReflectConstructor}，
	 * 实际上调用了所包裹的原生的 {@link Constructor}的newInstance(Object...)方法。
	 * @param args 传入的用于实例化的参数。
	 * @return 实例化后的对象。
	 */
	public T newInstance(Object... args){
		try {
	        return javaConstructor.newInstance(args);
        } catch (Exception e) {
        	throw new ReflectException("error newInstance in constructor '{0}.{1}'", javaConstructor.getDeclaringClass().getName(), getName());
        }
	}
	
	/**
	 * 反射类的构造器的初始化模块。
	 */
	private void initialize(){
		this.setAccessiable();

		this.parameters = new ReflectParameter[javaConstructor.getParameterTypes().length];
		
		if(this.parameters.length > 0){
			String[] names = reflectClass.getMetadata().getParameterNames(javaConstructor);

			if(null == names){
				names = createUnknowParameterNames(parameters.length);
			}
			
			if(javaConstructor.getDeclaringClass().isEnum() && javaConstructor.getGenericParameterTypes().length != this.parameters.length){
				//enum constructor's parameter size not equals to generic parameter types.
				
				for(int i=0;i<parameters.length;i++){
					ReflectParameter p = new ReflectParameter();
					
					p.index       = i+1;
					p.name        = names[i];
					p.type        = javaConstructor.getParameterTypes()[i];
					p.annotations = javaConstructor.getParameterAnnotations()[i];
					
					if(i < 2){
						p.genericType = p.type;
					}else{
						p.genericType = javaConstructor.getGenericParameterTypes()[i-2];
					}
					
					parameters[i] = p;
				}
				
			}else{
				for(int i=0;i<parameters.length;i++){
					ReflectParameter p = new ReflectParameter();
					
					p.index       = i+1;
					p.name        = names[i];
					p.type        = javaConstructor.getParameterTypes()[i];
					p.genericType = javaConstructor.getGenericParameterTypes()[i];
					p.annotations = javaConstructor.getParameterAnnotations()[i];
					
					parameters[i] = p;
				}
			}
		}
	}
	
	/**
	 * 将构造器的可访问性设置为true。
	 */
	private void setAccessiable(){
		try {
	        this.javaConstructor.setAccessible(true);
        } catch (SecurityException e) {
        	;
        }
	}
	
	/**
	 * 重写。将返回所包裹的 {@link Constructor}的toString()的内容。
	 */
	@Override
    public String toString() {
		return javaConstructor.toString();
    }
	
	private static String[] createUnknowParameterNames(int length){
		String[] names = new String[length];
		
		for(int i=0;i<length;i++){
			names[i] = "arg" + (i+1);
		}
		
		return names;
	}
}