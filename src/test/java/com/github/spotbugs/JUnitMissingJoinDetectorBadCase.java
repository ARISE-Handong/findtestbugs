package com.github.spotbugs;

import junit.framework.TestCase;

public class JUnitMissingJoinDetectorBadCase extends TestCase {
    public void testSomething() {
        SimpleThreadClass t = new SimpleThreadClass();
        t.start();
        // no test join
    }

    class SimpleThreadClass extends Thread {

    }
}