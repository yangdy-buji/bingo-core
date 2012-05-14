/*
 * Copyright 2002-2006 the original author or authors.
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

package bingo.lang.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 * If this test case fails, uncomment diagnostics in
 * <code>assertProtocolAndFilenames</code> method.
 *
 * @author Oliver Hutchison
 * @author Juergen Hoeller
 * @author Chris Beams
 */
@SuppressWarnings("unchecked")
public class PathMatchingResourcePatternResolverTest {

	private static final String[] CLASSES_IN_RESOURCE =
			new String[] {"EncodedResource.class","PathMatchingResourcePatternResolver.class","ResourcePatternResolver.class"};

	private static final String[] TEST_CLASSES_IN_RESOURCE =
			new String[] {"PathMatchingResourcePatternResolverTest.class"};

	private static final String[] CLASSES_IN_JUNIT =
			new String[] {"After.class","AfterClass.class","Assert.class","Assume.class","Before.class","BeforeClass.class",
						   "ComparisonFailure.class","ComparisonFailure$ComparisonCompactor.class",
						   "Ignore.class","Rule.class","Test.class","Test$None.class"};

	private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	@Test
	public void testInvalidPrefixWithPatternElementInIt() throws IOException {
		try {
			resolver.getResources("xx**:**/*.xy");
			fail("Should have thrown FileNotFoundException");
		}
		catch (FileNotFoundException ex) {
			// expected
		}
	}

	@Test
	public void testSingleResourceOnFileSystem() throws IOException {
		Resource[] resources =
				resolver.getResources("bingo/lang/resource/PathMatchingResourcePatternResolverTest.class");
		assertEquals(1, resources.length);
		assertProtocolAndFilename(resources[0], "file", "PathMatchingResourcePatternResolverTest.class");
	}

	@Test
	public void testSingleResourceInJar() throws IOException {
		Resource[] resources = resolver.getResources("java/net/URL.class");
		assertEquals(1, resources.length);
		assertProtocolAndFilename(resources[0], "jar", "URL.class");
	}

	@Ignore // passes under eclipse, fails under ant
	@Test
	public void testClasspathStarWithPatternOnFileSystem() throws IOException {
		Resource[] resources = resolver.getResources("classpath*:bingo/lang/res/*.class");
		// Have to exclude Clover-generated class files here,
		// as we might be running as part of a Clover test run.
		List noCloverResources = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i].getFilename().indexOf("$__CLOVER_") == -1) {
				noCloverResources.add(resources[i]);
			}
		}
		resources = (Resource[]) noCloverResources.toArray(new Resource[noCloverResources.size()]);
		assertProtocolAndFilenames(resources, "file", CLASSES_IN_RESOURCE, TEST_CLASSES_IN_RESOURCE);
	}

	@Test
	public void testClasspathWithPatternInJar() throws IOException {
		Resource[] resources = resolver.getResources("classpath:org/junit/*.class");
		assertProtocolAndFilenames(resources, "jar", CLASSES_IN_JUNIT);
	}

	@Test
	public void testClasspathStartWithPatternInJar() throws IOException {
		Resource[] resources = resolver.getResources("classpath*:org/junit/*.class");
		assertProtocolAndFilenames(resources, "jar", CLASSES_IN_JUNIT);
	}

	private void assertProtocolAndFilename(Resource resource, String urlProtocol, String fileName) throws IOException {
		assertProtocolAndFilenames(new Resource[] {resource}, urlProtocol, new String[] {fileName});
	}

	private void assertProtocolAndFilenames(
			Resource[] resources, String urlProtocol, String[] fileNames1, String[] fileNames2) throws IOException {
		List fileNames = new ArrayList(Arrays.asList(fileNames1));
		fileNames.addAll(Arrays.asList(fileNames2));
		assertProtocolAndFilenames(resources, urlProtocol, (String[]) fileNames.toArray(new String[fileNames.size()]));
	}

	private void assertProtocolAndFilenames(Resource[] resources, String urlProtocol, String[] fileNames)
			throws IOException {

		// Uncomment the following if you encounter problems with matching against the file system
		// It shows file locations.
//		String[] actualNames = new String[resources.length];
//		for (int i = 0; i < resources.length; i++) {
//			actualNames[i] = resources[i].getFilename();
//		}
//		List sortedActualNames = new LinkedList(Arrays.asList(actualNames));
//		List expectedNames = new LinkedList(Arrays.asList(fileNames));
//		Collections.sort(sortedActualNames);
//		Collections.sort(expectedNames);
//		
//		System.out.println("-----------");
//		System.out.println("Expected: " + StringUtils.collectionToCommaDelimitedString(expectedNames));
//		System.out.println("Actual: " + StringUtils.collectionToCommaDelimitedString(sortedActualNames));
//		for (int i = 0; i < resources.length; i++) {
//			System.out.println(resources[i]);
//		}
			
		assertEquals("Correct number of files found", fileNames.length, resources.length);
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			assertEquals(urlProtocol, resource.getURL().getProtocol());
			assertFilenameIn(resource, fileNames);
		}
	}

	private void assertFilenameIn(Resource resource, String[] fileNames) {
		for (int i = 0; i < fileNames.length; i++) {
			if (resource.getFilename().endsWith(fileNames[i])) {
				return;
			}
		}
		fail("resource [" + resource + "] does not have a filename that matches and of the names in 'fileNames'");
	}

}
