package com.problemfighter.java.oc.common;

public interface InitCustomProcessor {
    <S, D> ProcessCustomCopy<S, D> init(Class<?> klass, S source, D destination);
}
