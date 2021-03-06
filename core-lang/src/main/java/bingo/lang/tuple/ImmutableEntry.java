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

import java.util.Map.Entry;

import bingo.lang.exceptions.ReadonlyException;

public class ImmutableEntry<K,V> extends PairBase<K, V> implements Entry<K, V> {
	
	private static final long serialVersionUID = 6333097634226450971L;
	
	public static <K,V> ImmutableEntry<K,V> of(K key,V value){
		return new ImmutableEntry<K, V>(key, value);
	}
	
	private final K key;
	private final V value;
	
	public ImmutableEntry(K key,V value){
		this.key   = key;
		this.value = value;
	}

	public K getLeft() {
	    return key;
    }

	public V getRight() {
	    return value;
    }
	
    public K getKey() {
	    return key;
    }

    public V getValue() {
	    return value;
    }

	public V setValue(V value) {
	    throw new ReadonlyException();
    }
}