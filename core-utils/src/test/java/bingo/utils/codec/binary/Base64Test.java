/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bingo.utils.codec.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import bingo.lang.Strings;

/**
 * Test cases for Base64 class.
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
 * @author Apache Software Foundation
 * @version $Id: Base64Test.java 1161342 2011-08-25 00:34:24Z ggregory $
 */
public class Base64Test {
	
	static final byte[] CHUNK_SEPARATOR = {'\r', '\n'};

    private final Random _random = new Random();

    /**
     * @return Returns the _random.
     */
    public Random getRandom() {
        return this._random;
    }

    /**
     * Test the isStringBase64 method.
     */
    @Test
    public void testIsStringBase64() {
        String nullString = null;
        String emptyString = "";
        String validString = "abc===defg\n\r123456\r789\r\rABC\n\nDEF==GHI\r\nJKL==============";
        String invalidString = validString + ((char)0); // append null character
        
        try {
            Base64.isBase64(nullString);
        } catch (NullPointerException npe) {
            assertNotNull("Base64.isStringBase64() should be null-safe.", npe);
        }
        
        assertTrue("Base64.isStringBase64(empty-string) is true", Base64.isBase64(emptyString));
        assertTrue("Base64.isStringBase64(valid-string) is true", Base64.isBase64(validString));        
        assertFalse("Base64.isStringBase64(invalid-string) is false", Base64.isBase64(invalidString));        
    }
    
    /**
     * Test the Base64 implementation
     */
    @Test
    public void testBase64() {
        String content = "Hello World";
        String encodedContent;
        byte[] encodedBytes = Base64.encode(Strings.getBytesUtf8(content));
        encodedContent = Strings.newStringUtf8(encodedBytes);
        assertTrue("encoding hello world", encodedContent.equals("SGVsbG8gV29ybGQ="));

        encodedBytes = Base64.encode(Strings.getBytesUtf8(content),false);
        encodedContent = Strings.newStringUtf8(encodedBytes);
        assertTrue("encoding hello world", encodedContent.equals("SGVsbG8gV29ybGQ="));

        // bogus characters to decode (to skip actually) {e-acute*6}
        byte[] decode = Base64.decode("SGVsbG{\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9}8gV29ybGQ=");
        String decodeString = Strings.newStringUtf8(decode);
        assertTrue("decode hello world", decodeString.equals("Hello World"));        
    }

    /**
     * CODEC-68: isBase64 throws ArrayIndexOutOfBoundsException on some non-BASE64 bytes
     */
    @Test
    public void testCodec68() {
        byte[] x = new byte[]{'n', 'A', '=', '=', (byte) 0x9c};
        Base64.encode(x);
    }

    /**
     * Tests conditional true branch for "marker0" test.
     */
    @Test
    public void testDecodePadMarkerIndex2() throws UnsupportedEncodingException {
        assertEquals("A", new String(Base64.decode("QQ==".getBytes("UTF-8"))));
    }

    /**
     * Tests conditional branches for "marker1" test.
     */
    @Test
    public void testDecodePadMarkerIndex3() throws UnsupportedEncodingException {
        assertEquals("AA", new String(Base64.decode("QUE=".getBytes("UTF-8"))));
        assertEquals("AAA", new String(Base64.decode("QUFB".getBytes("UTF-8"))));
    }

    @Test
    public void testDecodePadOnly() throws UnsupportedEncodingException {
        assertTrue(Base64.decode("====".getBytes("UTF-8")).length == 0);
        assertEquals("", new String(Base64.decode("====".getBytes("UTF-8"))));
        // Test truncated padding
        assertTrue(Base64.decode("===".getBytes("UTF-8")).length == 0);
        assertTrue(Base64.decode("==".getBytes("UTF-8")).length == 0);
        assertTrue(Base64.decode("=".getBytes("UTF-8")).length == 0);
        assertTrue(Base64.decode("".getBytes("UTF-8")).length == 0);
    }

    @Test
    public void testDecodePadOnlyChunked() throws UnsupportedEncodingException {
        assertTrue(Base64.decode("====\n".getBytes("UTF-8")).length == 0);
        assertEquals("", new String(Base64.decode("====\n".getBytes("UTF-8"))));
        // Test truncated padding
        assertTrue(Base64.decode("===\n".getBytes("UTF-8")).length == 0);
        assertTrue(Base64.decode("==\n".getBytes("UTF-8")).length == 0);
        assertTrue(Base64.decode("=\n".getBytes("UTF-8")).length == 0);
        assertTrue(Base64.decode("\n".getBytes("UTF-8")).length == 0);
    }

    @Test
    public void testDecodeWithWhitespace() throws Exception {

        String orig = "I am a late night coder.";

        byte[] encodedArray = Base64.encode(orig.getBytes("UTF-8"));
        StringBuffer intermediate = new StringBuffer(new String(encodedArray));

        intermediate.insert(2, ' ');
        intermediate.insert(5, '\t');
        intermediate.insert(10, '\r');
        intermediate.insert(15, '\n');

        byte[] encodedWithWS = intermediate.toString().getBytes("UTF-8");
        byte[] decodedWithWS = Base64.decode(encodedWithWS);

        String dest = new String(decodedWithWS);

        assertTrue("Dest string doesn't equal the original", dest.equals(orig));
    }

    /**
     * Test encode and decode of empty byte array.
     */
    @Test
    public void testEmptyBase64() {
        byte[] empty = new byte[0];
        byte[] result = Base64.encode(empty);
        assertEquals("empty base64 encode", 0, result.length);
        assertEquals("empty base64 encode", 0, Base64.encode(null).length);
    }

    // encode/decode a large random array
    @Test
    public void testEncodeDecodeRandom() {
        for (int i = 1; i < 5; i++) {
            byte[] data = new byte[this.getRandom().nextInt(10000) + 1];
            this.getRandom().nextBytes(data);
            byte[] enc = Base64.encode(data);
            assertTrue(Base64.isBase64(enc));
            byte[] data2 = Base64.decode(enc);
            assertTrue(Arrays.equals(data, data2));
        }
    }

    // encode/decode random arrays from size 0 to size 11
    @Test
    public void testEncodeDecodeSmall() {
        for (int i = 0; i < 12; i++) {
            byte[] data = new byte[i];
            this.getRandom().nextBytes(data);
            byte[] enc = Base64.encode(data);
            assertTrue("\"" + (new String(enc)) + "\" is Base64 data.", Base64.isBase64(enc));
            byte[] data2 = Base64.decode(enc);
            assertTrue(toString(data) + " equals " + toString(data2), Arrays.equals(data, data2));
        }
    }

    @Test
    public void testIgnoringNonBase64InDecode() throws Exception {
        assertEquals("The quick brown fox jumped over the lazy dogs.", new String(Base64
                .decode("VGhlIH@$#$@%F1aWN@#@#@@rIGJyb3duIGZve\n\r\t%#%#%#%CBqd##$#$W1wZWQgb3ZlciB0aGUgbGF6eSBkb2dzLg==".getBytes("UTF-8"))));
    }

    @Test
    public void testIsArrayByteBase64() {
        assertFalse(Base64.isBase64(new byte[]{Byte.MIN_VALUE}));
        assertFalse(Base64.isBase64(new byte[]{-125}));
        assertFalse(Base64.isBase64(new byte[]{-10}));
        assertFalse(Base64.isBase64(new byte[]{0}));
        assertFalse(Base64.isBase64(new byte[]{64, Byte.MAX_VALUE}));
        assertFalse(Base64.isBase64(new byte[]{Byte.MAX_VALUE}));
        assertTrue(Base64.isBase64(new byte[]{'A'}));
        assertFalse(Base64.isBase64(new byte[]{'A', Byte.MIN_VALUE}));
        assertTrue(Base64.isBase64(new byte[]{'A', 'Z', 'a'}));
        assertTrue(Base64.isBase64(new byte[]{'/', '=', '+'}));
        assertFalse(Base64.isBase64(new byte[]{'$'}));
    }

    @Test
    public void testKnownDecodings() throws UnsupportedEncodingException {
        assertEquals("The quick brown fox jumped over the lazy dogs.", new String(Base64
                .decode("VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wZWQgb3ZlciB0aGUgbGF6eSBkb2dzLg==".getBytes("UTF-8"))));
        assertEquals("It was the best of times, it was the worst of times.", new String(Base64
                .decode("SXQgd2FzIHRoZSBiZXN0IG9mIHRpbWVzLCBpdCB3YXMgdGhlIHdvcnN0IG9mIHRpbWVzLg==".getBytes("UTF-8"))));
        assertEquals("http://jakarta.apache.org/commmons", new String(Base64
                .decode("aHR0cDovL2pha2FydGEuYXBhY2hlLm9yZy9jb21tbW9ucw==".getBytes("UTF-8"))));
        assertEquals("AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz", new String(Base64
                .decode("QWFCYkNjRGRFZUZmR2dIaElpSmpLa0xsTW1Obk9vUHBRcVJyU3NUdFV1VnZXd1h4WXlaeg==".getBytes("UTF-8"))));
        assertEquals("{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }", new String(Base64.decode("eyAwLCAxLCAyLCAzLCA0LCA1LCA2LCA3LCA4LCA5IH0="
                .getBytes("UTF-8"))));
        assertEquals("xyzzy!", new String(Base64.decode("eHl6enkh".getBytes("UTF-8"))));
    }

    @Test
    public void testKnownEncodings() throws UnsupportedEncodingException {
        assertEquals("VGhlIHF1aWNrIGJyb3duIGZveCBqdW1wZWQgb3ZlciB0aGUgbGF6eSBkb2dzLg==", new String(Base64
                .encode("The quick brown fox jumped over the lazy dogs.".getBytes("UTF-8"))));
        assertEquals(
                "YmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJs\r\nYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFo\r\nIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBibGFoIGJsYWggYmxhaCBi\r\nbGFoIGJsYWg=",
                new String(
                        Base64.encode("blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah".getBytes("UTF-8"),true)));
        assertEquals("SXQgd2FzIHRoZSBiZXN0IG9mIHRpbWVzLCBpdCB3YXMgdGhlIHdvcnN0IG9mIHRpbWVzLg==", new String(Base64
                .encode("It was the best of times, it was the worst of times.".getBytes("UTF-8"))));
        assertEquals("aHR0cDovL2pha2FydGEuYXBhY2hlLm9yZy9jb21tbW9ucw==", new String(Base64
                .encode("http://jakarta.apache.org/commmons".getBytes("UTF-8"))));
        assertEquals("QWFCYkNjRGRFZUZmR2dIaElpSmpLa0xsTW1Obk9vUHBRcVJyU3NUdFV1VnZXd1h4WXlaeg==", new String(Base64
                .encode("AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz".getBytes("UTF-8"))));
        assertEquals("eyAwLCAxLCAyLCAzLCA0LCA1LCA2LCA3LCA4LCA5IH0=", new String(Base64.encode("{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }"
                .getBytes("UTF-8"))));
        assertEquals("eHl6enkh", new String(Base64.encode("xyzzy!".getBytes("UTF-8"))));
    }

    @Test
    public void testNonBase64Test() throws Exception {

        byte[] bArray = {'%'};

        assertFalse("Invalid Base64 array was incorrectly validated as " + "an array of Base64 encoded data", Base64
                .isBase64(bArray));

        try {
            byte[] result = Base64.decode(bArray);

            assertTrue("The result should be empty as the test encoded content did " + "not contain any valid base 64 characters",
                    result.length == 0);
        } catch (Exception e) {
            fail("Exception was thrown when trying to decode "
                + "invalid base64 encoded data - RFC 2045 requires that all "
                + "non base64 character be discarded, an exception should not"
                + " have been thrown");
        }
    }

    @Test
    public void testPairs() {
        assertEquals("AAA=", new String(Base64.encode(new byte[]{0, 0})));
        for (int i = -128; i <= 127; i++) {
            byte test[] = {(byte) i, (byte) i};
            assertTrue(Arrays.equals(test, Base64.decode(Base64.encode(test))));
        }
    }

    /**
     * Tests RFC 4648 section 10 test vectors.
     * <ul>
     * <li>BASE64("") = ""</li>
     * <li>BASE64("f") = "Zg=="</li>
     * <li>BASE64("fo") = "Zm8="</li>
     * <li>BASE64("foo") = "Zm9v"</li>
     * <li>BASE64("foob") = "Zm9vYg=="</li>
     * <li>BASE64("fooba") = "Zm9vYmE="</li>
     * <li>BASE64("foobar") = "Zm9vYmFy"</li>
     * </ul>
     * 
     * @see <a href="http://tools.ietf.org/html/rfc4648">http://tools.ietf.org/html/rfc4648</a>
     */
    @Test
    public void testRfc4648Section10Decode() {
        assertEquals("", Strings.newStringUsAscii(Base64.decode("")));
        assertEquals("f", Strings.newStringUsAscii(Base64.decode("Zg==")));
        assertEquals("fo", Strings.newStringUsAscii(Base64.decode("Zm8=")));
        assertEquals("foo", Strings.newStringUsAscii(Base64.decode("Zm9v")));
        assertEquals("foob", Strings.newStringUsAscii(Base64.decode("Zm9vYg==")));
        assertEquals("fooba", Strings.newStringUsAscii(Base64.decode("Zm9vYmE=")));
        assertEquals("foobar", Strings.newStringUsAscii(Base64.decode("Zm9vYmFy")));
    }
    
    /**
     * Tests RFC 4648 section 10 test vectors.
     * <ul>
     * <li>BASE64("") = ""</li>
     * <li>BASE64("f") = "Zg=="</li>
     * <li>BASE64("fo") = "Zm8="</li>
     * <li>BASE64("foo") = "Zm9v"</li>
     * <li>BASE64("foob") = "Zm9vYg=="</li>
     * <li>BASE64("fooba") = "Zm9vYmE="</li>
     * <li>BASE64("foobar") = "Zm9vYmFy"</li>
     * </ul>
     * 
     * @see <a href="http://tools.ietf.org/html/rfc4648">http://tools.ietf.org/html/rfc4648</a>
     */
    @Test
    public void testRfc4648Section10DecodeWithCrLf() {
        String CRLF = Strings.newStringUsAscii(CHUNK_SEPARATOR);
        assertEquals("", Strings.newStringUsAscii(Base64.decode("" + CRLF)));
        assertEquals("f", Strings.newStringUsAscii(Base64.decode("Zg==" + CRLF)));
        assertEquals("fo", Strings.newStringUsAscii(Base64.decode("Zm8=" + CRLF)));
        assertEquals("foo", Strings.newStringUsAscii(Base64.decode("Zm9v" + CRLF)));
        assertEquals("foob", Strings.newStringUsAscii(Base64.decode("Zm9vYg==" + CRLF)));
        assertEquals("fooba", Strings.newStringUsAscii(Base64.decode("Zm9vYmE=" + CRLF)));
        assertEquals("foobar", Strings.newStringUsAscii(Base64.decode("Zm9vYmFy" + CRLF)));
    }
    
    /**
     * Tests RFC 4648 section 10 test vectors.
     * <ul>
     * <li>BASE64("") = ""</li>
     * <li>BASE64("f") = "Zg=="</li>
     * <li>BASE64("fo") = "Zm8="</li>
     * <li>BASE64("foo") = "Zm9v"</li>
     * <li>BASE64("foob") = "Zm9vYg=="</li>
     * <li>BASE64("fooba") = "Zm9vYmE="</li>
     * <li>BASE64("foobar") = "Zm9vYmFy"</li>
     * </ul>
     * 
     * @see <a href="http://tools.ietf.org/html/rfc4648">http://tools.ietf.org/html/rfc4648</a>
     */
    @Test
    public void testRfc4648Section10Encode() {
        assertEquals("", Base64.encodeToString(Strings.getBytesUtf8("")));
        assertEquals("Zg==", Base64.encodeToString(Strings.getBytesUtf8("f")));
        assertEquals("Zm8=", Base64.encodeToString(Strings.getBytesUtf8("fo")));
        assertEquals("Zm9v", Base64.encodeToString(Strings.getBytesUtf8("foo")));
        assertEquals("Zm9vYg==", Base64.encodeToString(Strings.getBytesUtf8("foob")));
        assertEquals("Zm9vYmE=", Base64.encodeToString(Strings.getBytesUtf8("fooba")));
        assertEquals("Zm9vYmFy", Base64.encodeToString(Strings.getBytesUtf8("foobar")));
    }
    
    /**
     * Tests RFC 4648 section 10 test vectors.
     * <ul>
     * <li>BASE64("") = ""</li>
     * <li>BASE64("f") = "Zg=="</li>
     * <li>BASE64("fo") = "Zm8="</li>
     * <li>BASE64("foo") = "Zm9v"</li>
     * <li>BASE64("foob") = "Zm9vYg=="</li>
     * <li>BASE64("fooba") = "Zm9vYmE="</li>
     * <li>BASE64("foobar") = "Zm9vYmFy"</li>
     * </ul>
     * 
     * @see <a href="http://tools.ietf.org/html/rfc4648">http://tools.ietf.org/html/rfc4648</a>
     */
    @Test
    public void testRfc4648Section10DecodeEncode() {
        testDecodeEncode("");
        //testDecodeEncode("Zg==");
        //testDecodeEncode("Zm8=");
        //testDecodeEncode("Zm9v");
        //testDecodeEncode("Zm9vYg==");
        //testDecodeEncode("Zm9vYmE=");
        //testDecodeEncode("Zm9vYmFy");
    }
    
    private void testDecodeEncode(String encodedText) {
        String decodedText = Strings.newStringUsAscii(Base64.decode(encodedText));
        String encodedText2 = Base64.encodeToString(Strings.getBytesUtf8(decodedText));
        assertEquals(encodedText, encodedText2);
    }

    /**
     * Tests RFC 4648 section 10 test vectors.
     * <ul>
     * <li>BASE64("") = ""</li>
     * <li>BASE64("f") = "Zg=="</li>
     * <li>BASE64("fo") = "Zm8="</li>
     * <li>BASE64("foo") = "Zm9v"</li>
     * <li>BASE64("foob") = "Zm9vYg=="</li>
     * <li>BASE64("fooba") = "Zm9vYmE="</li>
     * <li>BASE64("foobar") = "Zm9vYmFy"</li>
     * </ul>
     * 
     * @see <a href="http://tools.ietf.org/html/rfc4648">http://tools.ietf.org/html/rfc4648</a>
     */
    @Test
    public void testRfc4648Section10EncodeDecode() {
        testEncodeDecode("");
        testEncodeDecode("f");
        testEncodeDecode("fo");
        testEncodeDecode("foo");
        testEncodeDecode("foob");
        testEncodeDecode("fooba");
        testEncodeDecode("foobar");
    }
    
    private void testEncodeDecode(String plainText) {
        String encodedText = Base64.encodeToString(Strings.getBytesUtf8(plainText));
        String decodedText = Strings.newStringUsAscii(Base64.decode(encodedText));
        assertEquals(plainText, decodedText);
    }
    
    @Test
    public void testSingletons() {
        assertEquals("AA==", new String(Base64.encode(new byte[]{(byte) 0})));
        assertEquals("AQ==", new String(Base64.encode(new byte[]{(byte) 1})));
        assertEquals("Ag==", new String(Base64.encode(new byte[]{(byte) 2})));
        assertEquals("Aw==", new String(Base64.encode(new byte[]{(byte) 3})));
        assertEquals("BA==", new String(Base64.encode(new byte[]{(byte) 4})));
        assertEquals("BQ==", new String(Base64.encode(new byte[]{(byte) 5})));
        assertEquals("Bg==", new String(Base64.encode(new byte[]{(byte) 6})));
        assertEquals("Bw==", new String(Base64.encode(new byte[]{(byte) 7})));
        assertEquals("CA==", new String(Base64.encode(new byte[]{(byte) 8})));
        assertEquals("CQ==", new String(Base64.encode(new byte[]{(byte) 9})));
        assertEquals("Cg==", new String(Base64.encode(new byte[]{(byte) 10})));
        assertEquals("Cw==", new String(Base64.encode(new byte[]{(byte) 11})));
        assertEquals("DA==", new String(Base64.encode(new byte[]{(byte) 12})));
        assertEquals("DQ==", new String(Base64.encode(new byte[]{(byte) 13})));
        assertEquals("Dg==", new String(Base64.encode(new byte[]{(byte) 14})));
        assertEquals("Dw==", new String(Base64.encode(new byte[]{(byte) 15})));
        assertEquals("EA==", new String(Base64.encode(new byte[]{(byte) 16})));
        assertEquals("EQ==", new String(Base64.encode(new byte[]{(byte) 17})));
        assertEquals("Eg==", new String(Base64.encode(new byte[]{(byte) 18})));
        assertEquals("Ew==", new String(Base64.encode(new byte[]{(byte) 19})));
        assertEquals("FA==", new String(Base64.encode(new byte[]{(byte) 20})));
        assertEquals("FQ==", new String(Base64.encode(new byte[]{(byte) 21})));
        assertEquals("Fg==", new String(Base64.encode(new byte[]{(byte) 22})));
        assertEquals("Fw==", new String(Base64.encode(new byte[]{(byte) 23})));
        assertEquals("GA==", new String(Base64.encode(new byte[]{(byte) 24})));
        assertEquals("GQ==", new String(Base64.encode(new byte[]{(byte) 25})));
        assertEquals("Gg==", new String(Base64.encode(new byte[]{(byte) 26})));
        assertEquals("Gw==", new String(Base64.encode(new byte[]{(byte) 27})));
        assertEquals("HA==", new String(Base64.encode(new byte[]{(byte) 28})));
        assertEquals("HQ==", new String(Base64.encode(new byte[]{(byte) 29})));
        assertEquals("Hg==", new String(Base64.encode(new byte[]{(byte) 30})));
        assertEquals("Hw==", new String(Base64.encode(new byte[]{(byte) 31})));
        assertEquals("IA==", new String(Base64.encode(new byte[]{(byte) 32})));
        assertEquals("IQ==", new String(Base64.encode(new byte[]{(byte) 33})));
        assertEquals("Ig==", new String(Base64.encode(new byte[]{(byte) 34})));
        assertEquals("Iw==", new String(Base64.encode(new byte[]{(byte) 35})));
        assertEquals("JA==", new String(Base64.encode(new byte[]{(byte) 36})));
        assertEquals("JQ==", new String(Base64.encode(new byte[]{(byte) 37})));
        assertEquals("Jg==", new String(Base64.encode(new byte[]{(byte) 38})));
        assertEquals("Jw==", new String(Base64.encode(new byte[]{(byte) 39})));
        assertEquals("KA==", new String(Base64.encode(new byte[]{(byte) 40})));
        assertEquals("KQ==", new String(Base64.encode(new byte[]{(byte) 41})));
        assertEquals("Kg==", new String(Base64.encode(new byte[]{(byte) 42})));
        assertEquals("Kw==", new String(Base64.encode(new byte[]{(byte) 43})));
        assertEquals("LA==", new String(Base64.encode(new byte[]{(byte) 44})));
        assertEquals("LQ==", new String(Base64.encode(new byte[]{(byte) 45})));
        assertEquals("Lg==", new String(Base64.encode(new byte[]{(byte) 46})));
        assertEquals("Lw==", new String(Base64.encode(new byte[]{(byte) 47})));
        assertEquals("MA==", new String(Base64.encode(new byte[]{(byte) 48})));
        assertEquals("MQ==", new String(Base64.encode(new byte[]{(byte) 49})));
        assertEquals("Mg==", new String(Base64.encode(new byte[]{(byte) 50})));
        assertEquals("Mw==", new String(Base64.encode(new byte[]{(byte) 51})));
        assertEquals("NA==", new String(Base64.encode(new byte[]{(byte) 52})));
        assertEquals("NQ==", new String(Base64.encode(new byte[]{(byte) 53})));
        assertEquals("Ng==", new String(Base64.encode(new byte[]{(byte) 54})));
        assertEquals("Nw==", new String(Base64.encode(new byte[]{(byte) 55})));
        assertEquals("OA==", new String(Base64.encode(new byte[]{(byte) 56})));
        assertEquals("OQ==", new String(Base64.encode(new byte[]{(byte) 57})));
        assertEquals("Og==", new String(Base64.encode(new byte[]{(byte) 58})));
        assertEquals("Ow==", new String(Base64.encode(new byte[]{(byte) 59})));
        assertEquals("PA==", new String(Base64.encode(new byte[]{(byte) 60})));
        assertEquals("PQ==", new String(Base64.encode(new byte[]{(byte) 61})));
        assertEquals("Pg==", new String(Base64.encode(new byte[]{(byte) 62})));
        assertEquals("Pw==", new String(Base64.encode(new byte[]{(byte) 63})));
        assertEquals("QA==", new String(Base64.encode(new byte[]{(byte) 64})));
        assertEquals("QQ==", new String(Base64.encode(new byte[]{(byte) 65})));
        assertEquals("Qg==", new String(Base64.encode(new byte[]{(byte) 66})));
        assertEquals("Qw==", new String(Base64.encode(new byte[]{(byte) 67})));
        assertEquals("RA==", new String(Base64.encode(new byte[]{(byte) 68})));
        assertEquals("RQ==", new String(Base64.encode(new byte[]{(byte) 69})));
        assertEquals("Rg==", new String(Base64.encode(new byte[]{(byte) 70})));
        assertEquals("Rw==", new String(Base64.encode(new byte[]{(byte) 71})));
        assertEquals("SA==", new String(Base64.encode(new byte[]{(byte) 72})));
        assertEquals("SQ==", new String(Base64.encode(new byte[]{(byte) 73})));
        assertEquals("Sg==", new String(Base64.encode(new byte[]{(byte) 74})));
        assertEquals("Sw==", new String(Base64.encode(new byte[]{(byte) 75})));
        assertEquals("TA==", new String(Base64.encode(new byte[]{(byte) 76})));
        assertEquals("TQ==", new String(Base64.encode(new byte[]{(byte) 77})));
        assertEquals("Tg==", new String(Base64.encode(new byte[]{(byte) 78})));
        assertEquals("Tw==", new String(Base64.encode(new byte[]{(byte) 79})));
        assertEquals("UA==", new String(Base64.encode(new byte[]{(byte) 80})));
        assertEquals("UQ==", new String(Base64.encode(new byte[]{(byte) 81})));
        assertEquals("Ug==", new String(Base64.encode(new byte[]{(byte) 82})));
        assertEquals("Uw==", new String(Base64.encode(new byte[]{(byte) 83})));
        assertEquals("VA==", new String(Base64.encode(new byte[]{(byte) 84})));
        assertEquals("VQ==", new String(Base64.encode(new byte[]{(byte) 85})));
        assertEquals("Vg==", new String(Base64.encode(new byte[]{(byte) 86})));
        assertEquals("Vw==", new String(Base64.encode(new byte[]{(byte) 87})));
        assertEquals("WA==", new String(Base64.encode(new byte[]{(byte) 88})));
        assertEquals("WQ==", new String(Base64.encode(new byte[]{(byte) 89})));
        assertEquals("Wg==", new String(Base64.encode(new byte[]{(byte) 90})));
        assertEquals("Ww==", new String(Base64.encode(new byte[]{(byte) 91})));
        assertEquals("XA==", new String(Base64.encode(new byte[]{(byte) 92})));
        assertEquals("XQ==", new String(Base64.encode(new byte[]{(byte) 93})));
        assertEquals("Xg==", new String(Base64.encode(new byte[]{(byte) 94})));
        assertEquals("Xw==", new String(Base64.encode(new byte[]{(byte) 95})));
        assertEquals("YA==", new String(Base64.encode(new byte[]{(byte) 96})));
        assertEquals("YQ==", new String(Base64.encode(new byte[]{(byte) 97})));
        assertEquals("Yg==", new String(Base64.encode(new byte[]{(byte) 98})));
        assertEquals("Yw==", new String(Base64.encode(new byte[]{(byte) 99})));
        assertEquals("ZA==", new String(Base64.encode(new byte[]{(byte) 100})));
        assertEquals("ZQ==", new String(Base64.encode(new byte[]{(byte) 101})));
        assertEquals("Zg==", new String(Base64.encode(new byte[]{(byte) 102})));
        assertEquals("Zw==", new String(Base64.encode(new byte[]{(byte) 103})));
        assertEquals("aA==", new String(Base64.encode(new byte[]{(byte) 104})));
        for (int i = -128; i <= 127; i++) {
            byte test[] = {(byte) i};
            assertTrue(Arrays.equals(test, Base64.decode(Base64.encode(test))));
        }
    }

    @Test
    public void testTriplets() {
        assertEquals("AAAA", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 0})));
        assertEquals("AAAB", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 1})));
        assertEquals("AAAC", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 2})));
        assertEquals("AAAD", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 3})));
        assertEquals("AAAE", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 4})));
        assertEquals("AAAF", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 5})));
        assertEquals("AAAG", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 6})));
        assertEquals("AAAH", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 7})));
        assertEquals("AAAI", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 8})));
        assertEquals("AAAJ", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 9})));
        assertEquals("AAAK", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 10})));
        assertEquals("AAAL", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 11})));
        assertEquals("AAAM", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 12})));
        assertEquals("AAAN", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 13})));
        assertEquals("AAAO", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 14})));
        assertEquals("AAAP", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 15})));
        assertEquals("AAAQ", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 16})));
        assertEquals("AAAR", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 17})));
        assertEquals("AAAS", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 18})));
        assertEquals("AAAT", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 19})));
        assertEquals("AAAU", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 20})));
        assertEquals("AAAV", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 21})));
        assertEquals("AAAW", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 22})));
        assertEquals("AAAX", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 23})));
        assertEquals("AAAY", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 24})));
        assertEquals("AAAZ", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 25})));
        assertEquals("AAAa", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 26})));
        assertEquals("AAAb", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 27})));
        assertEquals("AAAc", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 28})));
        assertEquals("AAAd", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 29})));
        assertEquals("AAAe", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 30})));
        assertEquals("AAAf", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 31})));
        assertEquals("AAAg", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 32})));
        assertEquals("AAAh", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 33})));
        assertEquals("AAAi", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 34})));
        assertEquals("AAAj", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 35})));
        assertEquals("AAAk", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 36})));
        assertEquals("AAAl", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 37})));
        assertEquals("AAAm", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 38})));
        assertEquals("AAAn", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 39})));
        assertEquals("AAAo", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 40})));
        assertEquals("AAAp", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 41})));
        assertEquals("AAAq", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 42})));
        assertEquals("AAAr", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 43})));
        assertEquals("AAAs", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 44})));
        assertEquals("AAAt", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 45})));
        assertEquals("AAAu", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 46})));
        assertEquals("AAAv", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 47})));
        assertEquals("AAAw", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 48})));
        assertEquals("AAAx", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 49})));
        assertEquals("AAAy", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 50})));
        assertEquals("AAAz", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 51})));
        assertEquals("AAA0", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 52})));
        assertEquals("AAA1", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 53})));
        assertEquals("AAA2", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 54})));
        assertEquals("AAA3", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 55})));
        assertEquals("AAA4", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 56})));
        assertEquals("AAA5", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 57})));
        assertEquals("AAA6", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 58})));
        assertEquals("AAA7", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 59})));
        assertEquals("AAA8", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 60})));
        assertEquals("AAA9", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 61})));
        assertEquals("AAA+", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 62})));
        assertEquals("AAA/", new String(Base64.encode(new byte[]{(byte) 0, (byte) 0, (byte) 63})));
    }

//    /**
//     * Base64 encoding of UUID's is a common use-case, especially in URL-SAFE mode. This test case ends up being the
//     * "URL-SAFE" JUnit's.
//     * 
//     * @throws DecoderException
//     *             if Hex.decode() fails - a serious problem since Hex comes from our own commons-codec!
//     */
//    @Test
//    public void testUUID() throws DecoderException {
//        // The 4 UUID's below contains mixtures of + and / to help us test the
//        // URL-SAFE encoding mode.
//        byte[][] ids = new byte[4][];
//
//        // ids[0] was chosen so that it encodes with at least one +.
//        ids[0] = Hex.decodeHex("94ed8d0319e4493399560fb67404d370".toCharArray());
//
//        // ids[1] was chosen so that it encodes with both / and +.
//        ids[1] = Hex.decodeHex("2bf7cc2701fe4397b49ebeed5acc7090".toCharArray());
//
//        // ids[2] was chosen so that it encodes with at least one /.
//        ids[2] = Hex.decodeHex("64be154b6ffa40258d1a01288e7c31ca".toCharArray());
//
//        // ids[3] was chosen so that it encodes with both / and +, with /
//        // right at the beginning.
//        ids[3] = Hex.decodeHex("ff7f8fc01cdb471a8c8b5a9306183fe8".toCharArray());
//
//        byte[][] standard = new byte[4][];
//        standard[0] = Strings.getBytesUtf8("lO2NAxnkSTOZVg+2dATTcA==");
//        standard[1] = Strings.getBytesUtf8("K/fMJwH+Q5e0nr7tWsxwkA==");
//        standard[2] = Strings.getBytesUtf8("ZL4VS2/6QCWNGgEojnwxyg==");
//        standard[3] = Strings.getBytesUtf8("/3+PwBzbRxqMi1qTBhg/6A==");
//
//        byte[][] urlSafe1 = new byte[4][];
//        // regular padding (two '==' signs).
//        urlSafe1[0] = Strings.getBytesUtf8("lO2NAxnkSTOZVg-2dATTcA==");
//        urlSafe1[1] = Strings.getBytesUtf8("K_fMJwH-Q5e0nr7tWsxwkA==");
//        urlSafe1[2] = Strings.getBytesUtf8("ZL4VS2_6QCWNGgEojnwxyg==");
//        urlSafe1[3] = Strings.getBytesUtf8("_3-PwBzbRxqMi1qTBhg_6A==");
//
//        byte[][] urlSafe2 = new byte[4][];
//        // single padding (only one '=' sign).
//        urlSafe2[0] = Strings.getBytesUtf8("lO2NAxnkSTOZVg-2dATTcA=");
//        urlSafe2[1] = Strings.getBytesUtf8("K_fMJwH-Q5e0nr7tWsxwkA=");
//        urlSafe2[2] = Strings.getBytesUtf8("ZL4VS2_6QCWNGgEojnwxyg=");
//        urlSafe2[3] = Strings.getBytesUtf8("_3-PwBzbRxqMi1qTBhg_6A=");
//
//        byte[][] urlSafe3 = new byte[4][];
//        // no padding (no '=' signs).
//        urlSafe3[0] = Strings.getBytesUtf8("lO2NAxnkSTOZVg-2dATTcA");
//        urlSafe3[1] = Strings.getBytesUtf8("K_fMJwH-Q5e0nr7tWsxwkA");
//        urlSafe3[2] = Strings.getBytesUtf8("ZL4VS2_6QCWNGgEojnwxyg");
//        urlSafe3[3] = Strings.getBytesUtf8("_3-PwBzbRxqMi1qTBhg_6A");
//
//        for (int i = 0; i < 4; i++) {
//            byte[] encodedStandard = Base64.encode(ids[i]);
//            byte[] decodedStandard = Base64.decode(standard[i]);
//            byte[] decodedUrlSafe1 = Base64.decode(urlSafe1[i]);
//            byte[] decodedUrlSafe2 = Base64.decode(urlSafe2[i]);
//            byte[] decodedUrlSafe3 = Base64.decode(urlSafe3[i]);
//
//            // Very important debugging output should anyone
//            // ever need to delve closely into this stuff.
//            if (false) {
//                System.out.println("reference: [" + Hex.encodeHexString(ids[i]) + "]");
//                System.out.println("standard:  [" +
//                        Hex.encodeHexString(decodedStandard) +
//                    "] From: [" +
//                    Strings.newStringUtf8(standard[i]) +
//                    "]");
//                System.out.println("safe1:     [" +
//                        Hex.encodeHexString(decodedUrlSafe1) +
//                    "] From: [" +
//                    Strings.newStringUtf8(urlSafe1[i]) +
//                    "]");
//                System.out.println("safe2:     [" +
//                        Hex.encodeHexString(decodedUrlSafe2) +
//                    "] From: [" +
//                    Strings.newStringUtf8(urlSafe2[i]) +
//                    "]");
//                System.out.println("safe3:     [" +
//                        Hex.encodeHexString(decodedUrlSafe3) +
//                    "] From: [" +
//                    Strings.newStringUtf8(urlSafe3[i]) +
//                    "]");
//            }
//
//            assertTrue("standard encode uuid", Arrays.equals(encodedStandard, standard[i]));
//            assertTrue("url-safe encode uuid", Arrays.equals(encodedUrlSafe, urlSafe3[i]));
//            assertTrue("standard decode uuid", Arrays.equals(decodedStandard, ids[i]));
//            assertTrue("url-safe1 decode uuid", Arrays.equals(decodedUrlSafe1, ids[i]));
//            assertTrue("url-safe2 decode uuid", Arrays.equals(decodedUrlSafe2, ids[i]));
//            assertTrue("url-safe3 decode uuid", Arrays.equals(decodedUrlSafe3, ids[i]));
//        }
//    }
//
//    @Test
//    public void testByteToStringVariations() throws DecoderException {
//        Base64 base64 = new Base64(0);
//        byte[] b1 = Strings.getBytesUtf8("Hello World");
//        byte[] b2 = new byte[0];
//        byte[] b3 = null;
//        byte[] b4 = Hex.decodeHex("2bf7cc2701fe4397b49ebeed5acc7090".toCharArray());  // for url-safe tests
//
//        assertEquals("byteToString Hello World", "SGVsbG8gV29ybGQ=", base64.encodeToString(b1));
//        assertEquals("byteToString static Hello World", "SGVsbG8gV29ybGQ=", Base64.encodeBase64String(b1));
//        assertEquals("byteToString \"\"", "", base64.encodeToString(b2));
//        assertEquals("byteToString static \"\"", "", Base64.encodeBase64String(b2));
//        assertEquals("byteToString UUID", "K/fMJwH+Q5e0nr7tWsxwkA==", base64.encodeToString(b4));
//        assertEquals("byteToString static UUID", "K/fMJwH+Q5e0nr7tWsxwkA==", Base64.encodeBase64String(b4));
//        assertEquals("byteToString static-url-safe UUID", "K_fMJwH-Q5e0nr7tWsxwkA", Base64.encodeBase64URLSafeString(b4));
//    }
//
//    @Test
//    public void testStringToByteVariations() throws DecoderException {
//        Base64 base64 = new Base64();
//        String s1 = "SGVsbG8gV29ybGQ=\r\n";
//        String s2 = "";
//        String s3 = null;
//        String s4a = "K/fMJwH+Q5e0nr7tWsxwkA==\r\n";
//        String s4b = "K_fMJwH-Q5e0nr7tWsxwkA";
//        byte[] b4 = Hex.decodeHex("2bf7cc2701fe4397b49ebeed5acc7090".toCharArray());  // for url-safe tests
//
//        assertEquals("StringToByte Hello World", "Hello World", Strings.newStringUtf8(base64.decode(s1)));
//        assertEquals("StringToByte Hello World", "Hello World", Strings.newStringUtf8((byte[])base64.decode((Object)s1)));
//        assertEquals("StringToByte static Hello World", "Hello World", Strings.newStringUtf8(Base64.decode(s1)));
//        assertEquals("StringToByte \"\"", "", Strings.newStringUtf8(base64.decode(s2)));
//        assertEquals("StringToByte static \"\"", "", Strings.newStringUtf8(Base64.decode(s2)));
//        assertEquals("StringToByte null", null, Strings.newStringUtf8(base64.decode(s3)));
//        assertEquals("StringToByte static null", null, Strings.newStringUtf8(Base64.decode(s3)));
//        assertTrue("StringToByte UUID", Arrays.equals(b4, base64.decode(s4b)));
//        assertTrue("StringToByte static UUID", Arrays.equals(b4, Base64.decode(s4a)));
//        assertTrue("StringToByte static-url-safe UUID", Arrays.equals(b4, Base64.decode(s4b)));
//    }

    private String toString(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(data[i]);
            if (i != data.length - 1) {
                buf.append(",");
            }
        }
        return buf.toString();
    }

}
