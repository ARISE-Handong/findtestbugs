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

import java.util.*;

public class JUnitMissingFailDetector extends BytecodeScanningDetector {
    private ClassContext classContext;

    private final BugReporter bugReporter;
//    private final BugAccumulator bugAccumulator;

    public JUnitMissingFailDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;
        classContext.getJavaClass().accept(this);

        System.out.println("---------");
        System.out.println("JUnitMissingFailDetector visitClassContext");
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


    private CodeException[] codeExceptions;

    private ArrayList<PcAndException> pcAndExceptionList;
    private ArrayList<Integer> lastMethodIsFailPCList;

    @Override
    public void visit(Code code) {
        String methodName = getMethodName();
//        System.out.println("visit code:" + methodName );

        //In JUnit3, Test Methods are starts with a word "test"
        if(!methodName.startsWith("test")) {
            return;
        }

        // 각각의 method 마다 exception table update
        codeExceptions = code.getExceptionTable();
//        HashMap<Integer, String> catchExceptionMap = new HashMap<Integer, String>();
        lastMethodIsFailPCList = new ArrayList<Integer>();

        if(codeExceptions != null && codeExceptions.length > 0) {
            System.out.println(methodName + " has "+ codeExceptions.length +" code Exceptions");
            pcAndExceptionList = new ArrayList<PcAndException>();
        }

        // method 별로 실행됨 visit(Code code)
        super.visit(code);

        for(Integer pc : lastMethodIsFailPCList) {
            System.out.println("Last Method is fail pc : " + pc);
        }


        ArrayList<PcAndException> reportPcAndExceptionList = new ArrayList<PcAndException>();
        for (CodeException aExp : codeExceptions) {
            int startPC = aExp.getStartPC();
            int endPC = aExp.getEndPC();

            int lastExceptionThrowingMethodPC = startPC - 1;

            // if no fail in try block, failPC remains startPC - 1
            int failPC = startPC - 1;

            for(PcAndException pcAndException : pcAndExceptionList) {
                int exceptionPC = pcAndException.getPC();
                if( exceptionPC >= startPC && exceptionPC <= endPC) {
                    lastExceptionThrowingMethodPC = exceptionPC;
                }
            }

            System.out.println("lastMethodisfailpclist = "+ lastMethodIsFailPCList);
            for(Integer aFailPC : lastMethodIsFailPCList) {
                System.out.println("aFailPC = " + aFailPC);
                if (aFailPC >= startPC && aFailPC <= endPC) {
                    failPC = aFailPC;
                }
            }
            System.out.println("failPC = "+failPC);
            System.out.println("lastExceptionThrowingMethodPC = " + lastExceptionThrowingMethodPC);
            // if last Exception throwing method is called after last fail PC
            if(failPC < lastExceptionThrowingMethodPC) {
                BugInstance bug = new BugInstance(this, "TEST_MISSING_FAIL", NORMAL_PRIORITY)
                        .addClass(getClassName())
                        .addSourceLine(this, lastExceptionThrowingMethodPC);
                bugReporter.reportBug(bug);
            }
        }
    }

    private class PcAndException {
        int pc;
        List<String> exceptionNameList;

        private PcAndException(int pc, List<String> exceptionNameList) {
            this.pc = pc;
            this.exceptionNameList = exceptionNameList;
        }

        int getPC() {
            return this.pc;
        }

        List<String> getExceptionNameList() {
            return this.exceptionNameList;
        }
    }

    @Override
    public void sawOpcode(int seen) {
        printOpCode(seen);
        CodeException tryBlock = getSurroundingTryBlock(getPC());
//        int catchSize = getSizeOfSurroundingTryBlock(getPC());
//        System.out.println("pc = "+ getPC() + " catchSize = " + catchSize);
        if(tryBlock != null) {
            System.out.println("startpc = " + tryBlock.getStartPC() + " endPC = " + tryBlock.getEndPC());
            // try block 안에 있는 methodcall 인 경우
            if(isMethodCall()) {
                String className = getMethodDescriptorOperand().getClassDescriptor().toDottedClassName();
                String methodName =  getMethodDescriptorOperand().getName();
                String methodSig = getMethodSig();
                JavaClass targetClass = null;

                try {
                    targetClass = Repository.lookupClass(className);
                    JavaClassAndMethod targetMethod = Hierarchy.findMethod(targetClass, methodName, methodSig);

                    if (targetMethod != null) {
                        Method method = targetMethod.getMethod();
//                        System.out.println("targetMethod ="+ method.getName());
                        ExceptionTable methodExceptionTable = method.getExceptionTable();

                        // Exception 을 throw 하는 경우 list 에 넣는다.
                        if(methodExceptionTable != null) {
                            String[] methodExceptionNames = methodExceptionTable.getExceptionNames();
                            ArrayList<String> methodExceptionList = new ArrayList<String>();
                            Collections.addAll(methodExceptionList,methodExceptionNames);

                            pcAndExceptionList.add(new PcAndException(getPC(), methodExceptionList));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // TODO : report bug not printing printStackTrace
//                    e.printStackTrace();
                }
                if((seen == Const.INVOKESTATIC) && "fail".equals(methodName)) {
                    int nextPC = getNextPC();
                    for(CodeException codeException : codeExceptions) {
                        if (codeException.getEndPC() == nextPC) {
                            // fail method 의 pc 를 추가

                            lastMethodIsFailPCList.add(getPC());
                        }
                    }
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