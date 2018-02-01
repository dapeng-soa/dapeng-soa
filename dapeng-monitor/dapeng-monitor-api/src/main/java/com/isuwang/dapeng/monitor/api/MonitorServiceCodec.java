package com.github.dapeng.monitor.api;

import com.github.dapeng.core.*;
import com.github.dapeng.monitor.api.domain.QPSStat;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

public class MonitorServiceCodec {
    public static class QPSStatSerializer implements BeanSerializer<QPSStat> {

        @Override
        public com.github.dapeng.monitor.api.domain.QPSStat read( TProtocol iprot) throws TException {

            com.github.dapeng.monitor.api.domain.QPSStat bean =new com.github.dapeng.monitor.api.domain.QPSStat();
            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {

                    case 1:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setPeriod(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 2:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setAnalysisTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 3:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setServerIP(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 4:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setServerPort(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 5:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setCallCount(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 6:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setServiceName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 7:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setMethodName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 8:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setVersionName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;


                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
            return bean;
        }

        @Override
        public void write(com.github.dapeng.monitor.api.domain.QPSStat bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("QPSStat"));


            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("period", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 1));
            Integer elem0 = bean.getPeriod();
            oprot.writeI32(elem0);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("analysisTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 2));
            Long elem1 = bean.getAnalysisTime();
            oprot.writeI64(elem1);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serverIP", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 3));
            String elem2 = bean.getServerIP();
            oprot.writeString(elem2);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serverPort", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 4));
            Integer elem3 = bean.getServerPort();
            oprot.writeI32(elem3);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("callCount", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 5));
            Integer elem4 = bean.getCallCount();
            oprot.writeI32(elem4);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serviceName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 6));
            String elem5 = bean.getServiceName();
            oprot.writeString(elem5);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("methodName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 7));
            String elem6 = bean.getMethodName();
            oprot.writeString(elem6);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("versionName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 8));
            String elem7 = bean.getVersionName();
            oprot.writeString(elem7);

            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(com.github.dapeng.monitor.api.domain.QPSStat bean) throws TException {

            if (bean.getPeriod() == null)
                throw new SoaException(SoaBaseCode.NotNull, "period字段不允许为空");

            if (bean.getAnalysisTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "analysisTime字段不允许为空");

            if (bean.getServerIP() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serverIP字段不允许为空");

            if (bean.getServerPort() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serverPort字段不允许为空");

            if (bean.getCallCount() == null)
                throw new SoaException(SoaBaseCode.NotNull, "callCount字段不允许为空");

            if (bean.getServiceName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serviceName字段不允许为空");

            if (bean.getMethodName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "methodName字段不允许为空");

            if (bean.getVersionName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "versionName字段不允许为空");

        }

        @Override
        public String toString(com.github.dapeng.monitor.api.domain.QPSStat bean) {
            return bean == null ? "null" : bean.toString();
        }
    }

    public static class DataSourceStatSerializer implements TBeanSerializer<com.github.dapeng.monitor.api.domain.DataSourceStat> {

        @Override
        public void read(com.github.dapeng.monitor.api.domain.DataSourceStat bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {

                    case 1:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setPeriod(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 2:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setAnalysisTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 3:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setServerIP(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 4:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setServerPort(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 5:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setUrl(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 6:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setUserName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 7:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setIdentity(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 8:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setDbType(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 9:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setPoolingCount(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 10:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setPoolingPeak(Optional.of(elem0));
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 11:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setPoolingPeakTime(Optional.of(elem0));
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 13:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setActiveCount(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 14:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setActivePeak(Optional.of(elem0));
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 15:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setActivePeakTime(Optional.of(elem0));
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 16:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setExecuteCount(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 17:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setErrorCount(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;


                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(com.github.dapeng.monitor.api.domain.DataSourceStat bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("DataSourceStat"));


            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("period", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 1));
            Integer elem0 = bean.getPeriod();
            oprot.writeI32(elem0);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("analysisTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 2));
            Long elem1 = bean.getAnalysisTime();
            oprot.writeI64(elem1);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serverIP", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 3));
            String elem2 = bean.getServerIP();
            oprot.writeString(elem2);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serverPort", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 4));
            Integer elem3 = bean.getServerPort();
            oprot.writeI32(elem3);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("url", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 5));
            String elem4 = bean.getUrl();
            oprot.writeString(elem4);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("userName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 6));
            String elem5 = bean.getUserName();
            oprot.writeString(elem5);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("identity", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 7));
            String elem6 = bean.getIdentity();
            oprot.writeString(elem6);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("dbType", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 8));
            String elem7 = bean.getDbType();
            oprot.writeString(elem7);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("poolingCount", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 9));
            Integer elem8 = bean.getPoolingCount();
            oprot.writeI32(elem8);

            oprot.writeFieldEnd();
            if (bean.getPoolingPeak().isPresent()) {
                oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("poolingPeak", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 10));
                Integer elem9 = bean.getPoolingPeak().get();
                oprot.writeI32(elem9);

            }
            if (bean.getPoolingPeakTime().isPresent()) {
                oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("poolingPeakTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 11));
                Long elem10 = bean.getPoolingPeakTime().get();
                oprot.writeI64(elem10);

            }

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("activeCount", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 13));
            Integer elem11 = bean.getActiveCount();
            oprot.writeI32(elem11);

            oprot.writeFieldEnd();
            if (bean.getActivePeak().isPresent()) {
                oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("activePeak", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 14));
                Integer elem12 = bean.getActivePeak().get();
                oprot.writeI32(elem12);

            }
            if (bean.getActivePeakTime().isPresent()) {
                oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("activePeakTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 15));
                Long elem13 = bean.getActivePeakTime().get();
                oprot.writeI64(elem13);

            }

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("executeCount", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 16));
            Integer elem14 = bean.getExecuteCount();
            oprot.writeI32(elem14);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("errorCount", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 17));
            Integer elem15 = bean.getErrorCount();
            oprot.writeI32(elem15);

            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(com.github.dapeng.monitor.api.domain.DataSourceStat bean) throws TException {

            if (bean.getPeriod() == null)
                throw new SoaException(SoaBaseCode.NotNull, "period字段不允许为空");

            if (bean.getAnalysisTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "analysisTime字段不允许为空");

            if (bean.getServerIP() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serverIP字段不允许为空");

            if (bean.getServerPort() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serverPort字段不允许为空");

            if (bean.getUrl() == null)
                throw new SoaException(SoaBaseCode.NotNull, "url字段不允许为空");

            if (bean.getUserName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "userName字段不允许为空");

            if (bean.getIdentity() == null)
                throw new SoaException(SoaBaseCode.NotNull, "identity字段不允许为空");

            if (bean.getDbType() == null)
                throw new SoaException(SoaBaseCode.NotNull, "dbType字段不允许为空");

            if (bean.getPoolingCount() == null)
                throw new SoaException(SoaBaseCode.NotNull, "poolingCount字段不允许为空");

            if (bean.getActiveCount() == null)
                throw new SoaException(SoaBaseCode.NotNull, "activeCount字段不允许为空");

            if (bean.getExecuteCount() == null)
                throw new SoaException(SoaBaseCode.NotNull, "executeCount字段不允许为空");

            if (bean.getErrorCount() == null)
                throw new SoaException(SoaBaseCode.NotNull, "errorCount字段不允许为空");

        }

        @Override
        public String toString(com.github.dapeng.monitor.api.domain.DataSourceStat bean) {
            return bean == null ? "null" : bean.toString();
        }
    }

    public static class PlatformProcessDataSerializer implements TBeanSerializer<com.github.dapeng.monitor.api.domain.PlatformProcessData> {

        @Override
        public void read(com.github.dapeng.monitor.api.domain.PlatformProcessData bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {

                    case 1:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setPeriod(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 2:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setAnalysisTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 3:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setServiceName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 4:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setMethodName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 5:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setVersionName(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 6:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            String elem0 = iprot.readString();
                            bean.setServerIP(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 7:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setServerPort(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 8:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setPMinTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 9:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setPMaxTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 10:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setPAverageTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 11:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setPTotalTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 12:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setIMinTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 13:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setIMaxTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 14:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setIAverageTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 15:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I64) {
                            long elem0 = iprot.readI64();
                            bean.setITotalTime(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 16:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setTotalCalls(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 17:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setSucceedCalls(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 18:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setFailCalls(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 19:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setRequestFlow(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;

                    case 20:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32) {
                            int elem0 = iprot.readI32();
                            bean.setResponseFlow(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;


                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(com.github.dapeng.monitor.api.domain.PlatformProcessData bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("PlatformProcessData"));


            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("period", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 1));
            Integer elem0 = bean.getPeriod();
            oprot.writeI32(elem0);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("analysisTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 2));
            Long elem1 = bean.getAnalysisTime();
            oprot.writeI64(elem1);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serviceName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 3));
            String elem2 = bean.getServiceName();
            oprot.writeString(elem2);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("methodName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 4));
            String elem3 = bean.getMethodName();
            oprot.writeString(elem3);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("versionName", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 5));
            String elem4 = bean.getVersionName();
            oprot.writeString(elem4);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serverIP", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 6));
            String elem5 = bean.getServerIP();
            oprot.writeString(elem5);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("serverPort", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 7));
            Integer elem6 = bean.getServerPort();
            oprot.writeI32(elem6);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("pMinTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 8));
            Long elem7 = bean.getPMinTime();
            oprot.writeI64(elem7);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("pMaxTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 9));
            Long elem8 = bean.getPMaxTime();
            oprot.writeI64(elem8);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("pAverageTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 10));
            Long elem9 = bean.getPAverageTime();
            oprot.writeI64(elem9);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("pTotalTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 11));
            Long elem10 = bean.getPTotalTime();
            oprot.writeI64(elem10);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("iMinTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 12));
            Long elem11 = bean.getIMinTime();
            oprot.writeI64(elem11);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("iMaxTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 13));
            Long elem12 = bean.getIMaxTime();
            oprot.writeI64(elem12);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("iAverageTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 14));
            Long elem13 = bean.getIAverageTime();
            oprot.writeI64(elem13);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("iTotalTime", com.github.dapeng.org.apache.thrift.protocol.TType.I64, (short) 15));
            Long elem14 = bean.getITotalTime();
            oprot.writeI64(elem14);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("totalCalls", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 16));
            Integer elem15 = bean.getTotalCalls();
            oprot.writeI32(elem15);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("succeedCalls", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 17));
            Integer elem16 = bean.getSucceedCalls();
            oprot.writeI32(elem16);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("failCalls", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 18));
            Integer elem17 = bean.getFailCalls();
            oprot.writeI32(elem17);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("requestFlow", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 19));
            Integer elem18 = bean.getRequestFlow();
            oprot.writeI32(elem18);

            oprot.writeFieldEnd();

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("responseFlow", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 20));
            Integer elem19 = bean.getResponseFlow();
            oprot.writeI32(elem19);

            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(com.github.dapeng.monitor.api.domain.PlatformProcessData bean) throws TException {

            if (bean.getPeriod() == null)
                throw new SoaException(SoaBaseCode.NotNull, "period字段不允许为空");

            if (bean.getAnalysisTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "analysisTime字段不允许为空");

            if (bean.getServiceName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serviceName字段不允许为空");

            if (bean.getMethodName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "methodName字段不允许为空");

            if (bean.getVersionName() == null)
                throw new SoaException(SoaBaseCode.NotNull, "versionName字段不允许为空");

            if (bean.getServerIP() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serverIP字段不允许为空");

            if (bean.getServerPort() == null)
                throw new SoaException(SoaBaseCode.NotNull, "serverPort字段不允许为空");

            if (bean.getPMinTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "pMinTime字段不允许为空");

            if (bean.getPMaxTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "pMaxTime字段不允许为空");

            if (bean.getPAverageTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "pAverageTime字段不允许为空");

            if (bean.getPTotalTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "pTotalTime字段不允许为空");

            if (bean.getIMinTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "iMinTime字段不允许为空");

            if (bean.getIMaxTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "iMaxTime字段不允许为空");

            if (bean.getIAverageTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "iAverageTime字段不允许为空");

            if (bean.getITotalTime() == null)
                throw new SoaException(SoaBaseCode.NotNull, "iTotalTime字段不允许为空");

            if (bean.getTotalCalls() == null)
                throw new SoaException(SoaBaseCode.NotNull, "totalCalls字段不允许为空");

            if (bean.getSucceedCalls() == null)
                throw new SoaException(SoaBaseCode.NotNull, "succeedCalls字段不允许为空");

            if (bean.getFailCalls() == null)
                throw new SoaException(SoaBaseCode.NotNull, "failCalls字段不允许为空");

            if (bean.getRequestFlow() == null)
                throw new SoaException(SoaBaseCode.NotNull, "requestFlow字段不允许为空");

            if (bean.getResponseFlow() == null)
                throw new SoaException(SoaBaseCode.NotNull, "responseFlow字段不允许为空");

        }

        @Override
        public String toString(com.github.dapeng.monitor.api.domain.PlatformProcessData bean) {
            return bean == null ? "null" : bean.toString();
        }
    }


    public static class uploadQPSStat_args {

        private java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> qpsStats;

        public java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> getQpsStats() {
            return this.qpsStats;
        }

        public void setQpsStats(java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> qpsStats) {
            this.qpsStats = qpsStats;
        }


        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{");

            stringBuilder.append("\"").append("qpsStats").append("\":").append(qpsStats).append(",");

            if (stringBuilder.lastIndexOf(",") > 0)
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
            stringBuilder.append("}");

            return stringBuilder.toString();
        }

    }


    public static class uploadQPSStat_result {


        @Override
        public String toString() {
            return "{}";
        }

    }

    public static class UploadQPSStat_argsSerializer implements TBeanSerializer<uploadQPSStat_args> {

        @Override
        public void read(uploadQPSStat_args bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {

                    case 1:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.LIST) {
                            com.github.dapeng.org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                            java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> elem0 = new java.util.ArrayList<>(_list0.size);
                            for (int _i0 = 0; _i0 < _list0.size; ++_i0) {
                                com.github.dapeng.monitor.api.domain.QPSStat elem1;
                                elem1=new QPSStatSerializer().read(iprot);
                                elem0.add(elem1);
                            }
                            iprot.readListEnd();
                            bean.setQpsStats(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;


                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(uploadQPSStat_args bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("uploadQPSStat_args"));


            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("qpsStats", com.github.dapeng.org.apache.thrift.protocol.TType.LIST, (short) 1));
            java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> elem0 = bean.getQpsStats();

            oprot.writeListBegin(new com.github.dapeng.org.apache.thrift.protocol.TList(com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, elem0.size()));
            for (com.github.dapeng.monitor.api.domain.QPSStat elem1 : elem0) {
                new QPSStatSerializer().write(elem1, oprot);
            }
            oprot.writeListEnd();


            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(uploadQPSStat_args bean) throws TException {

            if (bean.getQpsStats() == null)
                throw new SoaException(SoaBaseCode.NotNull, "qpsStats字段不允许为空");

        }


        @Override
        public String toString(uploadQPSStat_args bean) {
            return bean == null ? "null" : bean.toString();
        }

    }

    public static class UploadQPSStat_resultSerializer implements TBeanSerializer<uploadQPSStat_result> {
        @Override
        public void read(uploadQPSStat_result bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {
                    case 0:  //SUCCESS
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.VOID) {

                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                  /*
                  case 1: //ERROR
                  bean.setSoaException(new SoaException());
                  new SoaExceptionSerializer().read(bean.getSoaException(), iprot);
                  break A;
                  */
                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(uploadQPSStat_result bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("uploadQPSStat_result"));


            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }


        public void validate(uploadQPSStat_result bean) throws TException {

        }


        @Override
        public String toString(uploadQPSStat_result bean) {
            return bean == null ? "null" : bean.toString();
        }
    }

    public static class uploadQPSStat<I extends com.github.dapeng.monitor.api.service.MonitorService> extends SoaProcessFunction<I, uploadQPSStat_args, uploadQPSStat_result, UploadQPSStat_argsSerializer, UploadQPSStat_resultSerializer> {
        public uploadQPSStat() {
            super("uploadQPSStat", new UploadQPSStat_argsSerializer(), new UploadQPSStat_resultSerializer());
        }

        @Override
        public uploadQPSStat_result getResult(I iface, uploadQPSStat_args args) throws TException {
            uploadQPSStat_result result = new uploadQPSStat_result();

            iface.uploadQPSStat(args.qpsStats);

            return result;
        }


        @Override
        public uploadQPSStat_args getEmptyArgsInstance() {
            return new uploadQPSStat_args();
        }

        @Override
        protected boolean isOneway() {
            return false;
        }
    }

    public static class uploadPlatformProcessData_args {

        private java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> platformProcessDatas;

        public java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> getPlatformProcessDatas() {
            return this.platformProcessDatas;
        }

        public void setPlatformProcessDatas(java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> platformProcessDatas) {
            this.platformProcessDatas = platformProcessDatas;
        }


        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{");

            stringBuilder.append("\"").append("platformProcessDatas").append("\":").append(platformProcessDatas).append(",");

            if (stringBuilder.lastIndexOf(",") > 0)
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
            stringBuilder.append("}");

            return stringBuilder.toString();
        }

    }


    public static class uploadPlatformProcessData_result {


        @Override
        public String toString() {
            return "{}";
        }

    }

    public static class UploadPlatformProcessData_argsSerializer implements TBeanSerializer<uploadPlatformProcessData_args> {

        @Override
        public void read(uploadPlatformProcessData_args bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {

                    case 1:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.LIST) {
                            com.github.dapeng.org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                            java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> elem0 = new java.util.ArrayList<>(_list0.size);
                            for (int _i0 = 0; _i0 < _list0.size; ++_i0) {
                                com.github.dapeng.monitor.api.domain.PlatformProcessData elem1 = new com.github.dapeng.monitor.api.domain.PlatformProcessData();
                                new PlatformProcessDataSerializer().read(elem1, iprot);
                                elem0.add(elem1);
                            }
                            iprot.readListEnd();
                            bean.setPlatformProcessDatas(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;


                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(uploadPlatformProcessData_args bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("uploadPlatformProcessData_args"));


            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("platformProcessDatas", com.github.dapeng.org.apache.thrift.protocol.TType.LIST, (short) 1));
            java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> elem0 = bean.getPlatformProcessDatas();

            oprot.writeListBegin(new com.github.dapeng.org.apache.thrift.protocol.TList(com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, elem0.size()));
            for (com.github.dapeng.monitor.api.domain.PlatformProcessData elem1 : elem0) {
                new PlatformProcessDataSerializer().write(elem1, oprot);
            }
            oprot.writeListEnd();


            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(uploadPlatformProcessData_args bean) throws TException {

            if (bean.getPlatformProcessDatas() == null)
                throw new SoaException(SoaBaseCode.NotNull, "platformProcessDatas字段不允许为空");

        }


        @Override
        public String toString(uploadPlatformProcessData_args bean) {
            return bean == null ? "null" : bean.toString();
        }

    }

    public static class UploadPlatformProcessData_resultSerializer implements TBeanSerializer<uploadPlatformProcessData_result> {
        @Override
        public void read(uploadPlatformProcessData_result bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {
                    case 0:  //SUCCESS
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.VOID) {

                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                  /*
                  case 1: //ERROR
                  bean.setSoaException(new SoaException());
                  new SoaExceptionSerializer().read(bean.getSoaException(), iprot);
                  break A;
                  */
                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(uploadPlatformProcessData_result bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("uploadPlatformProcessData_result"));


            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }


        public void validate(uploadPlatformProcessData_result bean) throws TException {

        }


        @Override
        public String toString(uploadPlatformProcessData_result bean) {
            return bean == null ? "null" : bean.toString();
        }
    }

    public static class uploadPlatformProcessData<I extends com.github.dapeng.monitor.api.service.MonitorService> extends SoaProcessFunction<I, uploadPlatformProcessData_args, uploadPlatformProcessData_result, UploadPlatformProcessData_argsSerializer, UploadPlatformProcessData_resultSerializer> {
        public uploadPlatformProcessData() {
            super("uploadPlatformProcessData", new UploadPlatformProcessData_argsSerializer(), new UploadPlatformProcessData_resultSerializer());
        }

        @Override
        public uploadPlatformProcessData_result getResult(I iface, uploadPlatformProcessData_args args) throws TException {
            uploadPlatformProcessData_result result = new uploadPlatformProcessData_result();

            iface.uploadPlatformProcessData(args.platformProcessDatas);

            return result;
        }


        @Override
        public uploadPlatformProcessData_args getEmptyArgsInstance() {
            return new uploadPlatformProcessData_args();
        }

        @Override
        protected boolean isOneway() {
            return false;
        }
    }

    public static class uploadDataSourceStat_args {

        private java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> dataSourceStat;

        public java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> getDataSourceStat() {
            return this.dataSourceStat;
        }

        public void setDataSourceStat(java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> dataSourceStat) {
            this.dataSourceStat = dataSourceStat;
        }


        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{");

            stringBuilder.append("\"").append("dataSourceStat").append("\":").append(dataSourceStat).append(",");

            if (stringBuilder.lastIndexOf(",") > 0)
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
            stringBuilder.append("}");

            return stringBuilder.toString();
        }

    }


    public static class uploadDataSourceStat_result {


        @Override
        public String toString() {
            return "{}";
        }

    }

    public static class UploadDataSourceStat_argsSerializer implements TBeanSerializer<uploadDataSourceStat_args> {

        @Override
        public void read(uploadDataSourceStat_args bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {

                    case 1:
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.LIST) {
                            com.github.dapeng.org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                            java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> elem0 = new java.util.ArrayList<>(_list0.size);
                            for (int _i0 = 0; _i0 < _list0.size; ++_i0) {
                                com.github.dapeng.monitor.api.domain.DataSourceStat elem1 = new com.github.dapeng.monitor.api.domain.DataSourceStat();
                                new DataSourceStatSerializer().read(elem1, iprot);
                                elem0.add(elem1);
                            }
                            iprot.readListEnd();
                            bean.setDataSourceStat(elem0);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;


                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(uploadDataSourceStat_args bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("uploadDataSourceStat_args"));


            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("dataSourceStat", com.github.dapeng.org.apache.thrift.protocol.TType.LIST, (short) 1));
            java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> elem0 = bean.getDataSourceStat();

            oprot.writeListBegin(new com.github.dapeng.org.apache.thrift.protocol.TList(com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, elem0.size()));
            for (com.github.dapeng.monitor.api.domain.DataSourceStat elem1 : elem0) {
                new DataSourceStatSerializer().write(elem1, oprot);
            }
            oprot.writeListEnd();


            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(uploadDataSourceStat_args bean) throws TException {

            if (bean.getDataSourceStat() == null)
                throw new SoaException(SoaBaseCode.NotNull, "dataSourceStat字段不允许为空");

        }


        @Override
        public String toString(uploadDataSourceStat_args bean) {
            return bean == null ? "null" : bean.toString();
        }

    }

    public static class UploadDataSourceStat_resultSerializer implements TBeanSerializer<uploadDataSourceStat_result> {
        @Override
        public void read(uploadDataSourceStat_result bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {
                    case 0:  //SUCCESS
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.VOID) {

                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                  /*
                  case 1: //ERROR
                  bean.setSoaException(new SoaException());
                  new SoaExceptionSerializer().read(bean.getSoaException(), iprot);
                  break A;
                  */
                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(uploadDataSourceStat_result bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("uploadDataSourceStat_result"));


            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }


        public void validate(uploadDataSourceStat_result bean) throws TException {

        }


        @Override
        public String toString(uploadDataSourceStat_result bean) {
            return bean == null ? "null" : bean.toString();
        }
    }

    public static class uploadDataSourceStat<I extends com.github.dapeng.monitor.api.service.MonitorService> extends SoaProcessFunction<I, uploadDataSourceStat_args, uploadDataSourceStat_result, UploadDataSourceStat_argsSerializer, UploadDataSourceStat_resultSerializer> {
        public uploadDataSourceStat() {
            super("uploadDataSourceStat", new UploadDataSourceStat_argsSerializer(), new UploadDataSourceStat_resultSerializer());
        }

        @Override
        public uploadDataSourceStat_result getResult(I iface, uploadDataSourceStat_args args) throws TException {
            uploadDataSourceStat_result result = new uploadDataSourceStat_result();

            iface.uploadDataSourceStat(args.dataSourceStat);

            return result;
        }


        @Override
        public uploadDataSourceStat_args getEmptyArgsInstance() {
            return new uploadDataSourceStat_args();
        }

        @Override
        protected boolean isOneway() {
            return false;
        }
    }


    public static class getServiceMetadata_args {

        @Override
        public String toString() {
            return "{}";
        }
    }


    public static class getServiceMetadata_result {

        private String success;

        public String getSuccess() {
            return success;
        }

        public void setSuccess(String success) {
            this.success = success;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{");
            stringBuilder.append("\"").append("success").append("\":\"").append(this.success).append("\",");
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
            stringBuilder.append("}");

            return stringBuilder.toString();
        }
    }

    public static class GetServiceMetadata_argsSerializer implements TBeanSerializer<getServiceMetadata_args> {

        @Override
        public void read(getServiceMetadata_args bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }
                switch (schemeField.id) {
                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);

                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }


        @Override
        public void write(getServiceMetadata_args bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_args"));
            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(getServiceMetadata_args bean) throws TException {
        }

        @Override
        public String toString(getServiceMetadata_args bean) {
            return bean == null ? "null" : bean.toString();
        }

    }

    public static class GetServiceMetadata_resultSerializer implements TBeanSerializer<getServiceMetadata_result> {
        @Override
        public void read(getServiceMetadata_result bean, TProtocol iprot) throws TException {

            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                    break;
                }

                switch (schemeField.id) {
                    case 0:  //SUCCESS
                        if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                            bean.setSuccess(iprot.readString());
                        } else {
                            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                    default:
                        com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            validate(bean);
        }

        @Override
        public void write(getServiceMetadata_result bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_result"));

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 0));
            oprot.writeString(bean.getSuccess());
            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

        public void validate(getServiceMetadata_result bean) throws TException {

            if (bean.getSuccess() == null)
                throw new SoaException(SoaBaseCode.NotNull, "success字段不允许为空");
        }

        @Override
        public String toString(getServiceMetadata_result bean) {
            return bean == null ? "null" : bean.toString();
        }
    }

    public static class getServiceMetadata<I extends com.github.dapeng.monitor.api.service.MonitorService> extends SoaProcessFunction<I, getServiceMetadata_args, getServiceMetadata_result, GetServiceMetadata_argsSerializer, GetServiceMetadata_resultSerializer> {
        public getServiceMetadata() {
            super("getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
        }

        @Override
        public getServiceMetadata_result getResult(I iface, getServiceMetadata_args args) throws TException {
            getServiceMetadata_result result = new getServiceMetadata_result();

            try (InputStreamReader isr = new InputStreamReader(MonitorServiceCodec.class.getClassLoader().getResourceAsStream("com.github.dapeng.monitor.api.service.MonitorService.xml"));
                 BufferedReader in = new BufferedReader(isr)) {
                int len = 0;
                StringBuilder str = new StringBuilder("");
                String line;
                while ((line = in.readLine()) != null) {

                    if (len != 0) {
                        str.append("\r\n").append(line);
                    } else {
                        str.append(line);
                    }
                    len++;
                }
                result.success = str.toString();

            } catch (Exception e) {
                e.printStackTrace();
                result.success = "";
            }

            return result;
        }

        @Override
        public getServiceMetadata_args getEmptyArgsInstance() {
            return new getServiceMetadata_args();
        }

        @Override
        protected boolean isOneway() {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static class Processor<I extends com.github.dapeng.monitor.api.service.MonitorService> extends SoaCommonBaseProcessor {
        public Processor(I iface) {
            super(iface, getProcessMap(new java.util.HashMap<>()));
        }

        @SuppressWarnings("unchecked")
        private static <I extends com.github.dapeng.monitor.api.service.MonitorService> java.util.Map<String, SoaProcessFunction<I, ?, ?, ? extends TBeanSerializer<?>, ? extends TBeanSerializer<?>>> getProcessMap(java.util.Map<String, SoaProcessFunction<I, ?, ?, ? extends TBeanSerializer<?>, ? extends TBeanSerializer<?>>> processMap) {

            processMap.put("uploadQPSStat", new uploadQPSStat());

            processMap.put("uploadPlatformProcessData", new uploadPlatformProcessData());

            processMap.put("uploadDataSourceStat", new uploadDataSourceStat());

            processMap.put("getServiceMetadata", new getServiceMetadata());

            return processMap;
        }
    }

}
      