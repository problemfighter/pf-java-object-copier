package com.problemfighter.java.oc.copier;


import com.problemfighter.java.oc.annotation.DataMapping;
import com.problemfighter.java.oc.annotation.DataMappingInfo;
import com.problemfighter.java.oc.common.InitCustomProcessor;
import com.problemfighter.java.oc.common.OCConstant;
import com.problemfighter.java.oc.common.ObjectCopierException;
import com.problemfighter.java.oc.common.ProcessCustomCopy;
import com.problemfighter.java.oc.data.CopyReport;
import com.problemfighter.java.oc.data.CopyReportError;
import com.problemfighter.java.oc.data.CopySourceDstField;
import com.problemfighter.java.oc.data.ObjectCopierInfoDetails;
import com.problemfighter.java.oc.reflection.ReflectionProcessor;
import javax.validation.*;
import java.lang.reflect.Field;
import java.util.*;

public class ObjectCopier {

    private ReflectionProcessor reflectionProcessor;
    private LinkedHashMap<String, CopyReport> errorReports = new LinkedHashMap<>();
    public InitCustomProcessor initCustomProcessor = null;

    public ObjectCopier() {
        reflectionProcessor = new ReflectionProcessor();
    }


    private void addReport(String name, String errorType, String nestedKey) {
        if (name == null) {
            name = "Source or Destination";
        }
        if (nestedKey == null) {
            errorReports.put(name, new CopyReport(name, errorType));
        } else {
            if (errorReports.get(nestedKey) != null) {
                errorReports.get(nestedKey).addNestedReport(new CopyReport(name, errorType));
            }
        }
    }

    public LinkedHashMap<String, CopyReport> getErrorReports() {
        return this.errorReports;
    }

    public LinkedHashMap<String, String> validateObject(Object object) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        for (ConstraintViolation<Object> violation : violations) {
            for (Path.Node node : violation.getPropertyPath()) {
                errors.put(node.getName(), violation.getMessage());
            }
        }
        return errors;
    }

    private Boolean isValidateTypeOrReport(CopySourceDstField copySourceDstField, String nestedKey) {
        Boolean isValid = false;
        if (copySourceDstField.source == null) {
            addReport(copySourceDstField.sourceFieldName, CopyReportError.DST_PROPERTY_UNAVAILABLE.label, nestedKey);
        } else if (copySourceDstField.destination == null) {
            addReport(copySourceDstField.sourceFieldName, CopyReportError.DST_PROPERTY_UNAVAILABLE.label, nestedKey);
        } else if (copySourceDstField.source.getType() != copySourceDstField.destination.getType()) {
            addReport(copySourceDstField.source.getName(), CopyReportError.DATA_TYPE_MISMATCH.label, nestedKey);
        } else {
            isValid = true;
        }
        return isValid;
    }

    private Boolean isDataMapperAnnotationAvailable(Field field) {
        return field.isAnnotationPresent(DataMapping.class);
    }

    private Boolean isFieldCustomCall(Field field) {
        if (isDataMapperAnnotationAvailable(field)) {
            return field.getAnnotation(DataMapping.class).customProcess();
        }
        return false;
    }

    private String getSourceFieldName(Field field, Boolean isStrict) {
        if (isDataMapperAnnotationAvailable(field)) {
            return field.getAnnotation(DataMapping.class).source();
        }
        if (!isStrict) {
            return field.getName();
        }
        return null;
    }


    private Boolean isDataMapperAnnotationAvailable(List<Field> fields) {
        for (Field field : fields) {
            if (isDataMapperAnnotationAvailable(field)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isDataMappingInfoAnnotation(Class<?> klass) {
        if (klass.isAnnotationPresent(DataMappingInfo.class)) {
            return true;
        }
        return false;
    }

    private Boolean isStrictMapping(Class<?> klass) {
        if (isDataMappingInfoAnnotation(klass)) {
            return klass.getAnnotation(DataMappingInfo.class).isStrict();
        }
        return OCConstant.isStrictCopy;
    }


    private String copierDefaultName(Class<?> klass) {
        if (isDataMappingInfoAnnotation(klass)) {
            return klass.getAnnotation(DataMappingInfo.class).name();
        }
        return OCConstant.copierDefaultName;
    }

    private Class<?> customProcessor(Class<?> klass) {
        if (isDataMappingInfoAnnotation(klass)) {
            return klass.getAnnotation(DataMappingInfo.class).customProcessor();
        }
        return null;
    }

    private <S, D> ProcessCustomCopy<S, D> initCustomProcessor(Object object, S sourceObject, D destinationObject) {
        Class<?> callbackClass = customProcessor(object.getClass());
        if (callbackClass == null || !ProcessCustomCopy.class.isAssignableFrom(callbackClass)) {
            return null;
        }
        ProcessCustomCopy<S, D> customCopy = null;
        if (initCustomProcessor != null) {
            customCopy = initCustomProcessor.init(callbackClass, sourceObject, destinationObject);
        } else {
            customCopy = (ProcessCustomCopy<S, D>) reflectionProcessor.newInstance(callbackClass);
        }
        return customCopy;
    }

    private <S, D> ObjectCopierInfoDetails<?, ?> processInfo(Object object, S sourceObject, D destinationObject) {
        ObjectCopierInfoDetails<S, D> objectCopierInfo = new ObjectCopierInfoDetails<>();
        objectCopierInfo.isStrictMapping = isStrictMapping(object.getClass());
        objectCopierInfo.mappingClassName = copierDefaultName(object.getClass());
        objectCopierInfo.processCustomCopy = initCustomProcessor(object, sourceObject, destinationObject);
        return objectCopierInfo;
    }

    private Field getField(Field field, CopySourceDstField copySourceDstField) {
        copySourceDstField.sourceFieldName = getSourceFieldName(field, copySourceDstField.isStrictMapping);
        if (copySourceDstField.sourceFieldName != null && copySourceDstField.dataObject != null) {
            Field sourceField = reflectionProcessor.getAnyFieldFromObject(copySourceDstField.dataObject, copySourceDstField.sourceFieldName);
            if (sourceField != null) {
                copySourceDstField.isCallback = isFieldCustomCall(field);
            }
            return sourceField;
        }
        return null;
    }

    private CopySourceDstField getCopiableSrcDstField(CopySourceDstField copySourceDstField) {
        if (copySourceDstField.destination != null) {
            copySourceDstField.source = getField(copySourceDstField.destination, copySourceDstField);
        } else if (copySourceDstField.source != null) {
            copySourceDstField.destination = getField(copySourceDstField.source, copySourceDstField);
        }
        return copySourceDstField;
    }

    private List<CopySourceDstField> dstAnnotatedNotSrc(List<Field> dstFields, Object dataObject, String nestedKey, ObjectCopierInfoDetails objectCopierInfoDetails) {
        List<CopySourceDstField> list = new ArrayList<>();
        CopySourceDstField copySourceDstField;
        for (Field field : dstFields) {
            copySourceDstField = new CopySourceDstField();
            copySourceDstField.setDataObject(dataObject);
            copySourceDstField.setDestination(field);
            copySourceDstField.isStrictMapping = objectCopierInfoDetails.isStrictMapping;
            copySourceDstField = getCopiableSrcDstField(copySourceDstField);
            if (isValidateTypeOrReport(copySourceDstField, nestedKey)) {
                list.add(copySourceDstField);
            }
        }
        return list;
    }

    private List<CopySourceDstField> srcAnnotatedNotDst(List<Field> srcFields, Object dataObject, String nestedKey, ObjectCopierInfoDetails objectCopierInfoDetails) {
        List<CopySourceDstField> list = new ArrayList<>();
        CopySourceDstField copySourceDstField;
        for (Field field : srcFields) {
            copySourceDstField = new CopySourceDstField();
            copySourceDstField.setDataObject(dataObject);
            copySourceDstField.setSource(field);
            copySourceDstField.isStrictMapping = objectCopierInfoDetails.isStrictMapping;
            copySourceDstField = getCopiableSrcDstField(copySourceDstField);
            if (isValidateTypeOrReport(copySourceDstField, nestedKey)) {
                list.add(copySourceDstField);
            }
        }
        return list;
    }

    private List<CopySourceDstField> srcDstNotAnnotated(List<Field> fields, Object dataObject, String nestedKey, ObjectCopierInfoDetails objectCopierInfoDetails) {
        return dstAnnotatedNotSrc(fields, dataObject, nestedKey, objectCopierInfoDetails);
    }

    private <S, D> ObjectCopierInfoDetails<?, ?> processDetailsInfo(S sourceObject, D destinationObject, String nestedKey) {
        Class<?> sourceClass = sourceObject.getClass();
        Class<?> destinationClass = destinationObject.getClass();
        ObjectCopierInfoDetails<?, ?> objectCopierInfoDetails = processInfo(destinationObject, sourceObject, destinationObject);
        objectCopierInfoDetails.amIDestination = true;

        List<Field> toKlassFields = reflectionProcessor.getAllField(destinationClass);
        if (isDataMappingInfoAnnotation(destinationClass) || isDataMapperAnnotationAvailable(toKlassFields)) {
            objectCopierInfoDetails.copySourceDstFields = dstAnnotatedNotSrc(toKlassFields, sourceObject, nestedKey, objectCopierInfoDetails);
            return objectCopierInfoDetails;
        }

        objectCopierInfoDetails = processInfo(sourceObject, sourceObject, destinationObject);
        List<Field> fromObjectFields = reflectionProcessor.getAllField(sourceClass);
        if (isDataMappingInfoAnnotation(sourceClass) || isDataMapperAnnotationAvailable(fromObjectFields)) {
            objectCopierInfoDetails = processInfo(sourceObject, sourceObject, destinationObject);
            objectCopierInfoDetails.amIDestination = false;
            objectCopierInfoDetails.copySourceDstFields = srcAnnotatedNotDst(fromObjectFields, destinationObject, nestedKey, objectCopierInfoDetails);
            return objectCopierInfoDetails;
        }

        if (!objectCopierInfoDetails.isStrictMapping) {
            objectCopierInfoDetails.copySourceDstFields = srcDstNotAnnotated(toKlassFields, sourceObject, nestedKey, objectCopierInfoDetails);
        }

        return objectCopierInfoDetails;
    }

    private Object processMap(Object sourceObject, Class<?> destinationProperty) throws IllegalAccessException, ObjectCopierException {
        if (sourceObject == null || destinationProperty == null) {
            return null;
        }
        Map<?, ?> map = (Map<?, ?>) sourceObject;
        Map response = reflectionProcessor.instanceOfMap(destinationProperty);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            response.put(processAndGetValue(entry.getKey(), getObjectNewInstance(entry.getKey()), entry.getKey().getClass()), processAndGetValue(entry.getValue(), getObjectNewInstance(entry.getValue()), entry.getValue().getClass()));
        }
        if (response.size() == 0) {
            return null;
        }
        return response;
    }


    private Object processSet(Object sourceObject, Class<?> destinationProperty) throws ObjectCopierException, IllegalAccessException {
        if (sourceObject == null || destinationProperty == null) {
            return null;
        }
        Set<?> list = (Set<?>) sourceObject;
        Set response = reflectionProcessor.instanceOfSet(destinationProperty);
        for (Object data : list) {
            if (data != null) {
                response.add(processAndGetValue(data, getObjectNewInstance(data), data.getClass()));
            }
        }
        if (response.size() == 0) {
            return null;
        }
        return response;
    }

    private Object processQueue(Object sourceObject, Class<?> destinationProperty) throws ObjectCopierException, IllegalAccessException {
        if (sourceObject == null || destinationProperty == null) {
            return null;
        }
        Queue<?> list = (Queue<?>) sourceObject;
        Queue response = reflectionProcessor.instanceOfQueue(destinationProperty);
        for (Object data : list) {
            if (data != null) {
                response.add(processAndGetValue(data, getObjectNewInstance(data), data.getClass()));
            }
        }
        if (response.size() == 0) {
            return null;
        }
        return response;
    }

    private Object processList(Object sourceObject, Class<?> destinationProperty) throws IllegalAccessException, ObjectCopierException {
        if (sourceObject == null || destinationProperty == null) {
            return null;
        }
        Collection<?> list = (Collection<?>) sourceObject;
        Collection response = reflectionProcessor.instanceOfList(destinationProperty);
        for (Object data : list) {
            if (data != null) {
                response.add(processAndGetValue(data, getObjectNewInstance(data), data.getClass()));
            }
        }
        if (response.size() == 0) {
            return null;
        }
        return response;
    }


    private Object processAndGetValue(Object source, Object destination, Class<?> klass) throws ObjectCopierException, IllegalAccessException {
        if (source == null && destination != null) {
            return destination;
        } else if (source == null) {
            return null;
        } else if (reflectionProcessor.isPrimitive(source.getClass())) {
            return source;
        } else if (reflectionProcessor.isList(source.getClass())) {
            return processList(source, klass);
        } else if (reflectionProcessor.isMap(source.getClass())) {
            return processMap(source, klass);
        } else if (reflectionProcessor.isSet(source.getClass())) {
            return processSet(source, klass);
        } else if (reflectionProcessor.isQueue(source.getClass())) {
            return processQueue(source, klass);
        }
        return processCopy(source, destination, destination.getClass().getSimpleName());
    }

    private Object getObjectNewInstance(Object object) {
        return reflectionProcessor.newInstance(object.getClass());
    }

    private Object getFieldValue(Object data, Field field) throws IllegalAccessException {
        if (field != null && data != null) {
            field.setAccessible(true);
            return field.get(data);
        }
        return null;
    }

    private Object getFieldValueOrObject(Object data, Field field) throws IllegalAccessException {
        Object fieldValue = getFieldValue(data, field);
        if (fieldValue == null) {
            return reflectionProcessor.newInstance(field.getType());
        }
        return fieldValue;
    }


    private <S, D> D processCopy(S source, D destination, String nestedKey) throws ObjectCopierException {
        try {

            if (source == null || destination == null) {
                return null;
            }

            ObjectCopierInfoDetails<?, ?> details = processDetailsInfo(source, destination, nestedKey);
            details.callGlobalCallBack(source, destination);
            Object sourceValue, destinationValue;
            for (CopySourceDstField copySourceDstField : details.copySourceDstFields) {
                sourceValue = getFieldValue(source, copySourceDstField.source);
                destinationValue = getFieldValueOrObject(destination, copySourceDstField.destination);
                copySourceDstField.destination.set(destination, processAndGetValue(sourceValue, destinationValue, copySourceDstField.destination.getType()));
            }
            return destination;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new ObjectCopierException(e.getMessage());
        }
    }

    private <S, D> D processCopy(S source, Class<D> klass, String nestedKey) throws ObjectCopierException {
        D toInstance = reflectionProcessor.newInstance(klass);
        return processCopy(source, toInstance, nestedKey);
    }


    public <S, D> D copy(S source, D destination) throws ObjectCopierException {
        return processCopy(source, destination, null);
    }

    public <S, D> D copy(S source, Class<D> destination) throws ObjectCopierException {
        return processCopy(source, destination, null);
    }


}
