package com.problemfighter.java.oc.data;

import com.problemfighter.java.oc.common.ProcessCustomCopy;
import com.problemfighter.java.oc.reflection.ReflectionProcessor;

import java.util.ArrayList;
import java.util.List;

public class ObjectCopierInfoDetails<S, D> {

    public Boolean isStrictMapping = true;

    // According to presents of DataMapping Annotation
    public Boolean amIDestination = true;
    public ProcessCustomCopy<Object, Object> processCustomCopy;
    public String mappingClassName;
    private ReflectionProcessor reflectionProcessor;

    public ObjectCopierInfoDetails() {
        reflectionProcessor = new ReflectionProcessor();
    }

    public Boolean callMeAsDst(Object source, Object destination) {
        if (reflectionProcessor.isMethodExist(processCustomCopy.getClass(), "meAsDst", destination.getClass(), source.getClass())) {
            reflectionProcessor.invokeMethod(processCustomCopy, "meAsDst", destination, source);
        } else if (reflectionProcessor.isMethodExist(processCustomCopy.getClass(), "meAsDst", source.getClass(), destination.getClass())) {
            reflectionProcessor.invokeMethod(processCustomCopy, "meAsDst", source, destination);
        } else {
            return false;
        }
        return true;
    }

    public Boolean callMeAsSst(Object source, Object destination) {
        if (reflectionProcessor.isMethodExist(processCustomCopy.getClass(), "meAsSrc", destination.getClass(), source.getClass())) {
            reflectionProcessor.invokeMethod(processCustomCopy, "meAsSrc", destination, source);
        } else if (reflectionProcessor.isMethodExist(processCustomCopy.getClass(), "meAsSrc", source.getClass(), destination.getClass())) {
            reflectionProcessor.invokeMethod(processCustomCopy, "meAsSrc", source, destination);
        } else {
            return false;
        }
        return true;
    }

    public void callGlobalCallBack(Object source, Object destination) {
        if (processCustomCopy != null) {
            Boolean isSuccess = false;
            if (amIDestination) {
                isSuccess = callMeAsDst(source, destination);
            } else {
                isSuccess = callMeAsSst(source, destination);
            }
            if (!isSuccess) {
                processCustomCopy.whyNotCalled("Method not found with the parameter " + source.getClass().getSimpleName() + " and " + destination.getClass().getSimpleName());
            }
        }
    }

    public List<CopySourceDstField> copySourceDstFields = new ArrayList<>();
}
