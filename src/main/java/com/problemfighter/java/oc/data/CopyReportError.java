package com.problemfighter.java.oc.data;

public enum CopyReportError {

    DATA_TYPE_MISMATCH("Data Type Mismatch"),
    DST_PROPERTY_UNAVAILABLE("Destination Property Unavailable"),
    SRC_PROPERTY_UNAVAILABLE("Source Property Unavailable");

    public final String label;
    CopyReportError(String label) {
        this.label = label;
    }
}
