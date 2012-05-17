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
package bingo.lang.tuple;

import bingo.lang.NamedValue;

public class MutableNamedValue<V> extends Pair<String, V> implements NamedValue<V> {
	
	private static final long serialVersionUID = -8052820161777994131L;
	
	private final String name;
	private V 	value;
	
	public MutableNamedValue(String name) {
	    this.name = name;
    }	

	public MutableNamedValue(String name, V value) {
		this.name  = name;
		this.value = value;
    }
	
	public String getName() {
	    return name;
    }

	public V setValue(V value) {
		this.value = value;
	    return value;
    }
	
	@Override
    public String getKey() {
	    return name;
    }

	@Override
    public V getValue() {
	    return value;
    }

	@Override
    protected String getLeft() {
	    return name;
    }

	@Override
    protected V getRight() {
	    return value;
    }
}