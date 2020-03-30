package com.github.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;

public class JUnitMissingJoinDetector extends BytecodeScanningDetector {

    private final BugReporter bugReporter;

    private int state;

    public JUnitMissingJoinDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    boolean directChildOfTestCase;


    private boolean sawSuperCall;

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (!enabled()) {
            return;
        }
        System.out.println("---------");
        System.out.println("JUnitMissingJoinDetector visitClassContext");
        System.out.println("---------");

        JavaClass jClass = classContext.getJavaClass();
        XClass xClass = classContext.getXClass();
        try {
            if (!isJunit3TestCase(xClass)) {
                return;
            }

            ClassDescriptor desc = xClass.getClassDescriptor();
            String fullClassNames[] = desc.getClassName().split("/");
            String className = fullClassNames[fullClassNames.length-1];

            jClass.accept(this);
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
    }

    private boolean seenThreadStart;
    private boolean seenThreadJoin;
    private int threadStartPC;

    @Override
    public void visit(Code code) {
        String methodName = getMethodName();

        System.out.println(methodName + ": visited");

        //In JUnit3, Test Methods are starts with a word "test"
        if(!methodName.startsWith("test")) {
            return;
        }
        threadStartPC = -1;
        super.visit(code);

        if(seenThreadStart && !seenThreadJoin) {
            BugInstance bug = new BugInstance(this, "TEST_MISSING_JOIN", NORMAL_PRIORITY)
                    .addClass(getClassName())
                    .addSourceLine(this, threadStartPC);
            bugReporter.reportBug(bug);
        }

    }

    @Override
    public void sawOpcode(int seen) {
        if((seen == Const.INVOKEVIRTUAL) && "start".equals(getNameConstantOperand())) {
            // TODO 지금은 method name만 보고 start, join 만 확인하는데,
            // parent class 가 Thread인지 확인하는 방법이 필요함..
            seenThreadStart = true;
            threadStartPC = getPC();
        } else if ((seen == Const.INVOKEVIRTUAL) && "join".equals(getNameConstantOperand())) {
            seenThreadJoin = true;
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
}


