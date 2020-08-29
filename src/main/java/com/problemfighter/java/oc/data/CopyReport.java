package com.problemfighter.java.oc.data;

import java.util.LinkedHashMap;

public class CopyReport {
    public String name;
    public String errorType;
    public String reason;
    public LinkedHashMap<String, CopyReport> nested;

    public CopyReport() {}

    public CopyReport(String name, String errorType) {
        this.name = name;
        this.errorType = errorType;
    }

    public CopyReport setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public CopyReport addNestedReport(CopyReport report) {
        if (nested == null) {
            nested = new LinkedHashMap<>();
        }
        nested.put(report.name, report);
        return this;
    }
}
