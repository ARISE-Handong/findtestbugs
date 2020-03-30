package com.github.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class JUnitPathSeparatorDetector extends BytecodeScanningDetector {
    private ClassContext classContext;

    private final BugReporter bugReporter;
//    private final BugAccumulator bugAccumulator;

    public JUnitPathSeparatorDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;
        classContext.getJavaClass().accept(this);

        System.out.println("---------");
        System.out.println("JUnitPathSeparatorDetector visitClassContext");
        System.out.println("---------");


        JavaClass jClass = classContext.getJavaClass();
        XClass xClass = classContext.getXClass();
        try {
            if (!isJunit3TestCase(xClass)) {
                return;
            }
            seenSlash = false;
            seenBackSlash = false;

            seenSlashPC = 0;
            seenBackSlashPC = 0;

            ClassDescriptor desc = xClass.getClassDescriptor();

            jClass.accept(this);

            if(seenSlash ^ seenBackSlash) { // only one of the slash is used in testcase
                int pc = seenSlash ? seenSlashPC : seenBackSlashPC;

                BugInstance bug = new BugInstance(this, "TEST_PATH_SEPARATOR", NORMAL_PRIORITY);
                bug.addClass(jClass);
                bugReporter.reportBug(bug);
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
    }


    private boolean seenSlash;
    private int seenSlashPC;
    private boolean seenBackSlash;
    private int seenBackSlashPC;

    @Override
    public void sawOpcode(int seen) {
        printOpCode(seen);
        if(!getMethodName().startsWith("test")) {
            return;
        }
        if(seen == Const.LDC) {
            Constant c = getConstantRefOperand();
            if (c != null && c instanceof ConstantString) {
                String operandString = getStringConstantOperand();
                //String methodName = getNameConstantOperand();
//                System.out.println("method Name = " + getNameConstantOperand());
                if (operandString.contains("/")) {
                    seenSlashPC = seen;
                    seenSlash = true;
                } else if (operandString.contains("\\")) {
                    seenBackSlashPC = seen;
                    seenBackSlash = true;
                }
            }
        }

    }

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