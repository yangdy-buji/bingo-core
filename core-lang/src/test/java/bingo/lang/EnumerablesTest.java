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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import bingo.lang.enumerable.IteratedEnumerable;
import bingo.lang.testing.Perf;
import bingo.lang.testing.junit.Concurrent;

public class EnumerablesTest {

	@Test
	@Concurrent(2)
	public void testPerformanceComparesToEnumerable() {
		final List<Integer> list = IteratedEnumerable.of(1, 3, 2, 4, 100, 1, 2, 3, 5, 531).toList();

		final IteratedEnumerable<Integer> enumerable = IteratedEnumerable.of(list);
		
		final Predicate<Integer> where = new Predicate<Integer>() {
			public boolean apply(Integer object) {
				return true;
			}
		};

		Perf.create("EnumerablePredicate(Where)", 100000)
		
		    .add("Enumerables.where", new Runnable() {
				public void run() {
					Enumerables.where(list, where).toArray(new Integer[] {});
				}
			})
			
			.add("Enumerable.where", new Runnable() {
				public void run() {
					enumerable.where(where).toArray(new Integer[] {});
				}
			})
			
			.add("Handcode.foreach", new Runnable() {
				public void run() {
					List<Integer> newList = new ArrayList<Integer>();
					for (Integer i : list) {
						if (where.apply(i)) {
							newList.add(i);
						}
					}
					newList.toArray(new Integer[] {});
				}
			})
			
			.run();
	}
}