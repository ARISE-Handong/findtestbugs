package com.github.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.classfile.JavaClass;

import java.util.List;

public class JUnitNoTearDownDetector extends BytecodeScanningDetector {

    private final BugReporter bugReporter;

    public JUnitNoTearDownDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (!enabled()) {
            return;
        }
        System.out.println("---------");
        System.out.println("JUnitNoTearDownDetector visitClassContext");
        System.out.println("---------");

        JavaClass jClass = classContext.getJavaClass();
        XClass xClass = classContext.getXClass();
        try {
            if (!isJunit3TestCase(xClass)) {
                return;
            }

            if(isSetUpImplemented(xClass)) {
                if(!isTearDownImplemented(xClass)) {
                    System.out.println("reached here");
                    BugInstance bug = new BugInstance(
                            this,
                            "TEST_NO_TEAR_DOWN", NORMAL_PRIORITY)
                            .addClass(jClass);
                    bugReporter.reportBug(bug);
                    System.out.println("bug reported");
                }
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
    }

    /**
     * Check whether or not this detector should be enabled. The detector is
     * disabled if the TestCase class cannot be found (meaning we don't have
     * junit.jar on the aux classpath).
     *
     * @return true if it should be enabled, false if not
     */
    private boolean enabled() {
        return true;
    }

    private boolean isJunit3TestCase(XClass jClass) throws ClassNotFoundException {
        ClassDescriptor sDesc = jClass.getSuperclassDescriptor();
        if (sDesc == null) {
            return false;
        }
        String sName = sDesc.getClassName();
        if (sName.equals("junit/framework/TestCase")) {
            return true;
        }
        if (sName.equals("java/lang/Object")) {
            return false;
        }

        try {
            XClass sClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, sDesc);
            if (sClass == null) {
                return false;
            }
            return isJunit3TestCase(sClass);
        } catch (CheckedAnalysisException e) {
            return false;
        }
    }

    // Check whether setUp() is implemented
    private boolean isSetUpImplemented(XClass xClass) {

        List<? extends XMethod> methods = xClass.getXMethods();

        for( XMethod m : methods) {
            if(m.getMethodDescriptor().getName().equals("setUp")) {
                return true;
            }
        }

        return false;
    }

    // Check whether tearDown() is implemented (check super classes too)
    private boolean isTearDownImplemented(XClass xClass) {
        List<? extends XMethod> methods = xClass.getXMethods();

        System.out.println("xCLasss");

        for( XMethod m : methods) {
            if(m.getMethodDescriptor().getName().equals("tearDown")) {
                return true;
            }
        }

        try {
            XClass sClass = xClass.getSuperclassDescriptor().getXClass();
            if( sClass == null) {
                return false;
            }

            if (sClass.getClassDescriptor().getClassName().equals("java/lang/Object")) {
                return false;
            }

            return isTearDownImplemented(sClass);
        } catch (CheckedAnalysisException e) {
            return false;
        }
    }
}


