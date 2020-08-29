package com.problemfighter.java.oc.common;

/**
 * Created by Touhid Mia on 11/09/2014.
 */
public class ObjectCopierException extends Exception {

    public ObjectCopierException(){
        super("Object Copier Exception");
    }

    public ObjectCopierException(String message){
        super(message);
    }
}
