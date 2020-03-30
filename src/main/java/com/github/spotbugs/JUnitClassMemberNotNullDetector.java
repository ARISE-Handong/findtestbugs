package com.github.spotbugs;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JUnitClassMemberNotNullDetector extends BytecodeScanningDetector {

    private final BugReporter bugReporter;

    public JUnitClassMemberNotNullDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    List<String> fieldNameList = new ArrayList<>();

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (!enabled()) {
            return;
        }
        System.out.println("---------");
        System.out.println("JUnitClassMemberNotNullDetector visitClassContext");
        System.out.println("---------");

        JavaClass jClass = classContext.getJavaClass();
        XClass xClass = classContext.getXClass();

        try {
            if (!isJunit3TestCase(xClass)) {
                return;
            }
            System.out.println("Reached Hear");

            List<? extends XField> fields = xClass.getXFields();
            System.out.println(" fields = " + fields);

            for (XField f : fields) {
                fieldNameList.add(f.getFieldDescriptor().getName());
                System.out.println("field name = " + f.getFieldDescriptor().getName());
            }


            jClass.accept(this);

            if(isSetUpImplemented(xClass)) {
                if(isTearDownImplemented(xClass)) {
                    for(String filedName : fieldNameList) {
                        BugInstance bug = new BugInstance(
                                this,
                                "TEST_CLASS_MEMBER_NOT_NULL", NORMAL_PRIORITY)
                                .addClass(jClass);
                        bugReporter.reportBug(bug);
                    }

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

    int nextPcOfNull = -1;

    @Override
    public void sawOpcode(int seen) {
        printOpCode(seen);
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
                    System.out.println("targetMethod ="+ method.getName());
                }
            } catch (ClassNotFoundException e) {
                // TODO : report bug not printing printStackTrace
//                    e.printStackTrace();
            }
        } else if (seen == ACONST_NULL) {
            nextPcOfNull = getNextPC();
        } else if (nextPcOfNull == getPC() && seen == Const.PUTFIELD) {
            XField f = getXFieldOperand();
            if (f != null) {
                String fieldName = f.getFieldDescriptor().getName();
                if(fieldNameList.contains(fieldName)) {
                    fieldNameList.remove(fieldName);
                }
            }
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


