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

import bingo.lang.NamedEntry;
import bingo.lang.NamedValue;
import bingo.lang.exceptions.ReadonlyException;

public class ImmutableNamedEntry<V> extends PairBase<String, V> implements NamedEntry<V>, NamedValue<V> {
	
	private static final long serialVersionUID = -426792841413872323L;
	
	public static <V> ImmutableNamedEntry<V> of(String name,V value){
		return new ImmutableNamedEntry<V>(name, value);
	}
	
	protected final String name;
	protected final V      value;
	
	public ImmutableNamedEntry(String name,V value){
		this.name  = name;
		this.value = value;
	}
	
	public String getLeft() {
	    return name;
    }

	public V getRight() {
	    return value;
    }
	
    public String getKey() {
	    return name;
    }

	public V getValue() {
	    return value;
    }

	public String getName() {
	    return name;
    }
	
	public V setValue(V value) {
	    throw new ReadonlyException();
    }
}