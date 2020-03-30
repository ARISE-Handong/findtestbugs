package com.github.spotbugs;

import junit.framework.TestCase;

public class JUnitUnexpectedThrowGoodCase extends TestCase {
    public void testSomething() {
        try {
            methodThrowsException();
            fail("MyException is not thrown.");
        } catch (MyException e) {
            System.out.println("MyException");
        }

        try {
            methodThrowsException2();
            fail("MyException is not thrown.");
        } catch (MyException e) {
            System.out.println("MyException2");
        }

    }

    void methodThrowsException() throws MyException {
        throw new MyException("This is my Exception");
    }

    void methodThrowsException2() throws MyException {
        throw new MyException("This is my Exception");
    }

    class MyException extends Exception {
        public MyException(String message) {
            super(message);
        }
    }
}
