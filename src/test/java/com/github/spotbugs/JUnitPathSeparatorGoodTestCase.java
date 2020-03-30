package com.github.spotbugs;

import junit.framework.TestCase;

import java.io.File;

public class JUnitPathSeparatorGoodTestCase extends TestCase {
    public void testSomething() {
        String fileName = getFileName();
        File file = new File(fileName);
        assertTrue(file.getPath().endsWith("test" + File.separator + "testFile.out"));
    }

    private String getFileName() {
        return "test" + File.separatorChar + "testFile.out";
    }
}
