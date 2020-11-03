package com.github.dapeng.org.apache.thrift;

public class WrappedTException extends RuntimeException {

    public WrappedTException(TException ex){
        super(ex);
    }

    public TException getCause(){
        return (TException) super.getCause();
    }
}
