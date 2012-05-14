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

import bingo.lang.beans.BeanClass;
import bingo.lang.beans.BeanProperty;
import bingo.lang.exceptions.NotFoundException;

public class Beans {

	protected Beans(){
		
	}
	
	public static <T> BeanClass<T> forType(Class<T> classType) {
		return BeanClass.get(classType);
	}
	
	public static BeanClass<?> forName(String className) throws NotFoundException {
		return BeanClass.get(Classes.forName(className));
	}
	
	public static BeanProperty[] getProperties(Class<?> beanType) {
		return BeanClass.get(beanType).getProperties();
	}
	
	public static <T> T newInstance(Class<T> beanType){
		return BeanClass.get(beanType).newInstance();
	}
	
	public static boolean set(Object bean,String property,Object value){
		return BeanClass.get(bean.getClass()).set(bean, property, value);
	}
	
	public static Object get(Object bean,String property){
		return BeanClass.get(bean.getClass()).get(bean, property);
	}
}
