package com.github.dapeng.transaction.api.domain;

import java.io.Serializable;

/**
 *
 **/
public class TGlobalTransaction implements Serializable{

    /**
     *
     **/
    private Integer id;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    /**
     *
     **/
    private TGlobalTransactionsStatus status;

    public TGlobalTransactionsStatus getStatus() {
        return this.status;
    }

    public void setStatus(TGlobalTransactionsStatus status) {
        this.status = status;
    }


    /**
     *
     **/
    private Integer currSequence;

    public Integer getCurrSequence() {
        return this.currSequence;
    }

    public void setCurrSequence(Integer currSequence) {
        this.currSequence = currSequence;
    }


    /**
     * @datatype(name="date")
     **/
    private java.util.Date createdAt;

    public java.util.Date getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt;
    }


    /**
     * @datatype(name="date")
     **/
    private java.util.Date updatedAt;

    public java.util.Date getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updatedAt = updatedAt;
    }


    /**
     *
     **/
    private Integer createdBy;

    public Integer getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }


    /**
     *
     **/
    private Integer updatedBy;

    public Integer getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"").append("id").append("\":").append(this.id).append(",");
        stringBuilder.append("\"").append("status").append("\":").append(this.status).append(",");
        stringBuilder.append("\"").append("currSequence").append("\":").append(this.currSequence).append(",");
        stringBuilder.append("\"").append("createdAt").append("\":").append(this.createdAt).append(",");
        stringBuilder.append("\"").append("updatedAt").append("\":").append(this.updatedAt).append(",");
        stringBuilder.append("\"").append("createdBy").append("\":").append(this.createdBy).append(",");
        stringBuilder.append("\"").append("updatedBy").append("\":").append(this.updatedBy).append(",");

        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
      