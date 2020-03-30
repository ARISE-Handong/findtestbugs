package com.github.spotbugs;

import junit.framework.TestCase;

public class JUnitMissingJoinDetectorGoodCase extends TestCase {
    public void testSomething() throws InterruptedException {
        SimpleThreadClass t = new SimpleThreadClass();
        t.start();
        t.join();
    }

    class SimpleThreadClass extends Thread {

    }
}
