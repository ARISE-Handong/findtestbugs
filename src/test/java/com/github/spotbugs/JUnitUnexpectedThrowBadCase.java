package com.github.spotbugs;

import junit.framework.TestCase;

public class JUnitUnexpectedThrowBadCase extends TestCase {
    public void testSomething() {
        try {
            methodThrowsException(); // The exception of this method could be thrown
            methodThrowsException2(); // Expected this method throws an exception
            fail("Expected fail is not thrown.");
        } catch (MyException e) {

        }

    }

    void methodThrowsException() throws MyException {
        throw new MyException("This is MyException");
    }

    void methodThrowsException2() throws MyException {
        throw new MyException("This is MyException");
    }

    class MyException extends Exception {
        public MyException(String message) {
            super(message);
        }
    }
}