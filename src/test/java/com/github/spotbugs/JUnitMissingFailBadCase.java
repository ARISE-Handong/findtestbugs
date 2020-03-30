package com.github.spotbugs;

import junit.framework.TestCase;

public class JUnitMissingFailBadCase extends TestCase {
    public void testSomething() {
        try {
            methodThrowsException();
            methodThrowsException2();
//            fail("Expected fail");
        } catch (MyException e) {
            System.out.println("MyException");
        } catch (MyException2 e) {
            System.out.println("MyException2");
        }
    }

    void methodThrowsException() throws MyException {
        throw new MyException("This is my Exception");
    }

    void methodThrowsException2() throws MyException2 {
        throw new MyException2("This is my Exception");
    }

    class MyException extends Exception {
        public MyException(String message) {
            super(message);
        }
    }

    class MyException2 extends Exception {
        public MyException2(String message) {
            super(message);
        }
    }
}
