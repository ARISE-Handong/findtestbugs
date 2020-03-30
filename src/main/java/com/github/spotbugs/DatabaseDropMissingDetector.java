package com.github.spotbugs;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;

public class DatabaseDropMissingDetector extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    public DatabaseDropMissingDetector(BugReporter bugReporter) { this.bugReporter = bugReporter; }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (!enabled()) {
            return;
        }
        System.out.println("---------");
        System.out.println("DatabaseDropMissingDetector visitClassContext");
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


    private boolean seenCreateQuery;
    private int createQueryPC;
    private boolean seenDropQuery;

    @Override
    public void visit(Code code) {
        String methodName = getMethodName();

        System.out.println(methodName + ": visited");

        //In JUnit3, Test Methods are starts with a word "test"
        if(!methodName.startsWith("test")) {
            return;
        }

        System.out.println("Test Case ");

        seenLDC = false;
        seenCreateQuery = false;
        seenDropQuery = false;

        super.visit(code);

        if(seenCreateQuery && !seenDropQuery) {
            BugInstance bug = new BugInstance(this, "TEST_DB_DROP_MISSING", NORMAL_PRIORITY)
                    .addClass(getClassName())
                    .addSourceLine(this, createQueryPC);
            bugReporter.reportBug(bug);
        }
    }

    private boolean seenLDC;
    private String operandString;

    @Override
    public void sawOpcode(int seen) {
//        printOpCode(seen);
        if(seenLDC) {
            if (seen == Const.INVOKEINTERFACE
                    && "java.sql.Statement".equals(getDottedClassConstantOperand())
                    && "executeUpdate".equals(getNameConstantOperand())) {
                if (operandString.toUpperCase().startsWith("CREATE")) {
                    seenCreateQuery = true;
                    createQueryPC = getPC();
                } else if (operandString.toUpperCase().startsWith("DROP")) {
                    seenDropQuery = true;
                }
            }
        }

        if(seen == Const.LDC) {
            seenLDC = true;
            Constant c = getConstantRefOperand();
            if (c != null && c instanceof ConstantString) {
                operandString = getStringConstantOperand();
//                System.out.println("operandString = " + operandString);
            }
        } else {
            seenLDC = false;
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