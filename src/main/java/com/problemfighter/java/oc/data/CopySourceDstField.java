package com.problemfighter.java.oc.data;

import java.lang.reflect.Field;

public class CopySourceDstField {
    public Field source;
    public Field destination;
    public String sourceFieldName;

    public Boolean isStrictMapping = true;
    public Boolean isCallback = false;

    public Object dataObject;

    public CopySourceDstField() {}

    public CopySourceDstField(Field destination, Field source, String sourceFieldName) {
        this.source = source;
        this.destination = destination;
        this.sourceFieldName = sourceFieldName;
    }

    public CopySourceDstField setSource(Field source) {
        this.source = source;
        return this;
    }

    public CopySourceDstField setDestination(Field destination) {
        this.destination = destination;
        return this;
    }

    public CopySourceDstField setDataObject(Object dataObject) {
        this.dataObject = dataObject;
        return this;
    }

}
