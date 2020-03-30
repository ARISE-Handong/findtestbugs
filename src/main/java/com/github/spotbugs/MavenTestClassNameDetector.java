package com.github.spotbugs;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.regex.Pattern;

public class MavenTestClassNameDetector extends BytecodeScanningDetector {
    private static final int SEEN_NOTHING = 0;

    private static final int SEEN_ALOAD_0 = 1;

    private final BugReporter bugReporter;

    private int state;

    public MavenTestClassNameDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    boolean directChildOfTestCase;


    private boolean sawSuperCall;

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (!enabled()) {
            return;
        }

        JavaClass jClass = classContext.getJavaClass();
        XClass xClass = classContext.getXClass();
        try {
            ClassDescriptor desc = xClass.getClassDescriptor();
            String fullClassNames[] = desc.getClassName().split("/");
            String className = fullClassNames[fullClassNames.length-1];

            if ((jClass.getAccessFlags() & Const.ACC_ABSTRACT) == 0) {
                if (!isValidTestClassName(className)) {
                    if (isJunit3TestCase(xClass) || doesClassNameContainTest(className)) {
                        bugReporter.reportBug(new BugInstance(this, "TEST_CLASS_NAME_NOT_DEFAULT", LOW_PRIORITY).addClass(jClass));
                    }
                }
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
    }

    private boolean isValidTestClassName(String className) {
        String pattern1 = "Test.*";
        String pattern2 = ".*Test$";
        String pattern3 = ".*Tests$";
        String pattern4 = ".*TestCase$";

        if (className.matches(pattern1) ||
                className.matches(pattern2) ||
                className.matches(pattern3) ||
                className.matches(pattern4)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean doesClassNameContainTest(String className) {
        String pattern = ".*Test.*";

        if (className.matches(pattern)) {
            return true;
        } else {
            return false;
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

    @Override
    public void sawOpcode(int seen) {
        switch (state) {
            case SEEN_NOTHING:
                if (seen == Const.ALOAD_0) {
                    state = SEEN_ALOAD_0;
                }
                break;

            case SEEN_ALOAD_0:
                if ((seen == Const.INVOKESPECIAL)
                        && (getNameConstantOperand().equals(getMethodName()))
                        && (getSigConstantOperand().equals("()V"))) {
                    sawSuperCall = true;
                }
                state = SEEN_NOTHING;
                break;
            default:
                state = SEEN_NOTHING;
        }
    }
}
