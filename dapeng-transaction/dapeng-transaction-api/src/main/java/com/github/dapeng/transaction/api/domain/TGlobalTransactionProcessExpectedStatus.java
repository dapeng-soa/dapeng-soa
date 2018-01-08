package com.github.dapeng.transaction.api.domain;

import com.github.dapeng.org.apache.thrift.TEnum;

public enum TGlobalTransactionProcessExpectedStatus implements TEnum {

    /**
     *
     **/
    Success(1),

    /**
     *
     **/
    HasRollback(2);


    private final int value;

    private TGlobalTransactionProcessExpectedStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static TGlobalTransactionProcessExpectedStatus findByValue(int value) {
        switch (value) {

            case 1:
                return Success;

            case 2:
                return HasRollback;

            default:
                return null;
        }
    }
}
      