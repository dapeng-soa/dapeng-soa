package com.github.dapeng.transaction.api.domain;

import com.github.dapeng.org.apache.thrift.TEnum;

public enum TGlobalTransactionsStatus implements TEnum {

    /**
     *
     **/
    New(1),

    /**
     *
     **/
    Success(2),

    /**
     *
     **/
    Fail(3),

    /**
     *
     **/
    HasRollback(4),

    /**
     *
     **/
    PartiallyRollback(5),

    /**
     *
     **/
    Suspend(99);


    private final int value;

    private TGlobalTransactionsStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static TGlobalTransactionsStatus findByValue(int value) {
        switch (value) {

            case 1:
                return New;

            case 2:
                return Success;

            case 3:
                return Fail;

            case 4:
                return HasRollback;

            case 5:
                return PartiallyRollback;

            case 99:
                return Suspend;

            default:
                return null;
        }
    }
}
      