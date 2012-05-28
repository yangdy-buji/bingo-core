/**
 * Copyright (C) 2010 Mycila <mathieu.carbou@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bingo.lang.testing.junit;

import java.util.concurrent.CountDownLatch;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import bingo.lang.logging.Log;
import bingo.lang.logging.LogFactory;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class Repeater implements TestRule {
	
	private static final Log log = LogFactory.get(Repeater.class);
	
	private static final int CPUS = Runtime.getRuntime().availableProcessors();
	
	private int      times      = CPUS;
	private boolean concurrent = true;;
	
	public Repeater(){
		
	}
	
	public Repeater(int threads){
		this.times = threads;
	}
	
	public Repeater(boolean concurrent,int threadsOrTimes){
		this.concurrent = concurrent;
		this.times      = threadsOrTimes;
	}
	
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Repeat annotation = description.getAnnotation(Repeat.class);
                
                if(null == annotation){
                	annotation = description.getTestClass().getAnnotation(Repeat.class);
                }
                
                if(null != annotation ){
                	
                	if(annotation.ignore()){
                		base.evaluate();
                		return;
                	}
                	
                	times      = Math.max(0, annotation.times());
                	concurrent = annotation.concurrent();
                }
                
                if (times == 1) {
                    base.evaluate();
                } else {
                	String testName = description.getTestClass().getSimpleName() + (description.isTest() ? "#" + description.getMethodName() : "");
                	
                    if (times > 1) {
                    	int counter = times;
                    	
                        if (!concurrent) {
                            while (counter-- > 0) {
                                base.evaluate();
                            }
                            log.debug("run test case '{}' {} times repeatly",testName,times);
                        } else {
                            ConcurrentJunitRunnerScheduler scheduler = new ConcurrentJunitRunnerScheduler(testName, times);
                            
                            final CountDownLatch go = new CountDownLatch(1);
                            
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    try {
                                        go.await();
                                        base.evaluate();
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    } catch (Throwable throwable) {
                                        throw ConcurrentJunitException.wrap(throwable);
                                    }
                                }
                            };
                            
                            while (counter-- > 0) {
                                scheduler.schedule(runnable);
                            }
                            
                            go.countDown();
                            
                            try {
                                scheduler.finished();
                            } catch (ConcurrentJunitException e) {
                                throw e.unwrap();
                            }
                            
                            log.debug("run test case '{}' concurrently with {} threads",testName,times);
                        }
                    }
                }
            }
        };
    }
}
