package com.github.dapeng.monitor.api;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.monitor.api.MonitorServiceCodec.*;
import com.github.dapeng.remoting.BaseServiceClient;
import com.github.dapeng.org.apache.thrift.TException;

public class MonitorServiceClient extends BaseServiceClient {

    public MonitorServiceClient() {
        super("com.github.dapeng.monitor.api.service.MonitorService", "1.0.0");
    }

    @Override
    protected boolean isSoaTransactionalProcess() {
        return false;
    }


    /**
     * 上送QPS信息
     **/

    public void uploadQPSStat(java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> qpsStats) throws SoaException {
        initContext("uploadQPSStat");

        try {
            uploadQPSStat_args uploadQPSStat_args = new uploadQPSStat_args();
            uploadQPSStat_args.setQpsStats(qpsStats);


            uploadQPSStat_result response = sendBase(uploadQPSStat_args, new uploadQPSStat_result(), new UploadQPSStat_argsSerializer(), new UploadQPSStat_resultSerializer());


        } catch (SoaException e) {
            throw e;
        } catch (TException e) {
            throw new SoaException(e);
        } finally {
            destoryContext();
        }
    }


    /**
     * 上送平台处理数据
     **/

    public void uploadPlatformProcessData(java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> platformProcessDatas) throws SoaException {
        initContext("uploadPlatformProcessData");

        try {
            uploadPlatformProcessData_args uploadPlatformProcessData_args = new uploadPlatformProcessData_args();
            uploadPlatformProcessData_args.setPlatformProcessDatas(platformProcessDatas);


            uploadPlatformProcessData_result response = sendBase(uploadPlatformProcessData_args, new uploadPlatformProcessData_result(), new UploadPlatformProcessData_argsSerializer(), new UploadPlatformProcessData_resultSerializer());


        } catch (SoaException e) {
            throw e;
        } catch (TException e) {
            throw new SoaException(e);
        } finally {
            destoryContext();
        }
    }


    /**
     * 上送DataSource信息
     **/

    public void uploadDataSourceStat(java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> dataSourceStat) throws SoaException {
        initContext("uploadDataSourceStat");

        try {
            uploadDataSourceStat_args uploadDataSourceStat_args = new uploadDataSourceStat_args();
            uploadDataSourceStat_args.setDataSourceStat(dataSourceStat);


            uploadDataSourceStat_result response = sendBase(uploadDataSourceStat_args, new uploadDataSourceStat_result(), new UploadDataSourceStat_argsSerializer(), new UploadDataSourceStat_resultSerializer());


        } catch (SoaException e) {
            throw e;
        } catch (TException e) {
            throw new SoaException(e);
        } finally {
            destoryContext();
        }
    }


    /**
     * getServiceMetadata
     **/
    public String getServiceMetadata() throws SoaException {
        initContext("getServiceMetadata");
        try {
            getServiceMetadata_args getServiceMetadata_args = new getServiceMetadata_args();
            getServiceMetadata_result response = sendBase(getServiceMetadata_args, new getServiceMetadata_result(), new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
            return response.getSuccess();
        } catch (SoaException e) {
            throw e;
        } catch (TException e) {
            throw new SoaException(e);
        } finally {
            destoryContext();
        }
    }

}
      