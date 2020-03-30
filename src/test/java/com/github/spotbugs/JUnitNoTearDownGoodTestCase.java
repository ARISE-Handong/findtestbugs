package com.github.spotbugs;

import junit.framework.TestCase;

public class JUnitNoTearDownGoodTestCase extends TestCase {
    public void setUp() throws Exception{
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSomething() throws Exception {
        // This is for JUnit no test case
    }
}
