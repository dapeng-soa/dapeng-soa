package com.github.dapeng.impl.filters;


import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.impl.filters.freq.CounterNode;
import com.github.dapeng.impl.filters.freq.NodePageMeta;
import com.github.dapeng.impl.filters.freq.ShmManager;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class testShm {
    private static CounterNode checkNodeCount(FreqControlRule rule, int key) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        ShmManager shmManagerRef = ShmManager.getInstance();
        Method getIdFromShm = shmManagerRef.getClass().getDeclaredMethod("getIdFromShm",String.class);
        getIdFromShm.setAccessible(true);
        short appId = (short)getIdFromShm.invoke(shmManagerRef,rule.app);
        short ruleTypeId = (short)getIdFromShm.invoke(shmManagerRef,rule.ruleType);

        Field nodePageCountRef = shmManagerRef.getClass().getDeclaredField("NODE_PAGE_COUNT");
        nodePageCountRef.setAccessible(true);

        int nodePageHash = (appId << 16 | ruleTypeId) ^ key;
        int nodePageIndex = nodePageHash % (int)nodePageCountRef.get(shmManagerRef);

        Method getSpinNodePageLock = shmManagerRef.getClass().getDeclaredMethod("getSpinNodePageLock",int.class);
        getSpinNodePageLock.setAccessible(true);
        getSpinNodePageLock.invoke(shmManagerRef,nodePageIndex);

        Field nodeaddrRef = shmManagerRef.getClass().getDeclaredField("homeAddr");
        nodeaddrRef.setAccessible(true);

        Field nodePageOffsetRef = shmManagerRef.getClass().getDeclaredField("NODE_PAGE_OFFSET");
        nodePageOffsetRef.setAccessible(true);

        long nodeAddr = (long)nodeaddrRef.get(shmManagerRef) + (long)nodePageOffsetRef.get(shmManagerRef) + 1024 * nodePageIndex + 16;

        Method getNodePageMeta = shmManagerRef.getClass().getDeclaredMethod("getNodePageMeta",int.class);
        getNodePageMeta.setAccessible(true);
        NodePageMeta nodePageMeta = (NodePageMeta)getNodePageMeta.invoke(shmManagerRef,nodePageIndex);

        CounterNode node = getNodeData(appId, ruleTypeId, key, nodePageMeta, nodeAddr);


        Method freeNodePageLock = shmManagerRef.getClass().getDeclaredMethod("freeNodePageLock",int.class);
        freeNodePageLock.setAccessible(true);
        freeNodePageLock.invoke(shmManagerRef,nodePageIndex);

        return node;
    }

    private static CounterNode getNodeData(short appId, short ruleTypeId, int key,
                                           NodePageMeta nodePageMeta, long nodeAddr) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CounterNode node = null;

        ShmManager shmManagerRef = ShmManager.getInstance();
        Method getShort = shmManagerRef.getClass().getDeclaredMethod("getShort",long.class);
        getShort.setAccessible(true);
        Method getInt = shmManagerRef.getClass().getDeclaredMethod("getInt",long.class);
        getInt.setAccessible(true);

        for (int index = 0; index < nodePageMeta.nodes; index++) {
            short _appId = (short)getShort.invoke(shmManagerRef,nodeAddr);
            if (_appId == 0) break;
            if (appId != _appId) {
                nodeAddr += 24;
                continue;
            }

            nodeAddr += Short.BYTES;
            short _ruleTypeId = (short)getShort.invoke(shmManagerRef,nodeAddr);
            if (ruleTypeId != _ruleTypeId) {
                nodeAddr += 22;
                continue;
            }

            nodeAddr += Short.BYTES;
            int _key = (int)getInt.invoke(shmManagerRef,nodeAddr);
            if (key != _key) {
                nodeAddr += Integer.BYTES * 5;
                continue;
            }

            node = new CounterNode();
            node.appId = _appId;
            node.ruleTypeId = _ruleTypeId;
            node.key = _key;

            nodeAddr += Integer.BYTES; //timestamp

            nodeAddr += Integer.BYTES;
            node.minCount = (int)getInt.invoke(shmManagerRef,nodeAddr);

            nodeAddr += Integer.BYTES;
            node.midCount = (int)getInt.invoke(shmManagerRef,nodeAddr);

            nodeAddr += Integer.BYTES;
            node.maxCount = (int)getInt.invoke(shmManagerRef,nodeAddr);
            break;
        }
        return node;
    }

    private static void testShmCallerId() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        boolean result = false;
        CounterNode node = null;
        rule.app = "com.today.hello";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 10;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 50;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 80;

        System.out.println("test callerId:----------------------------------------");

        for (int i = 0; i < 100; i++) {

            result = manager.reportAndCheck(rule, 214);
            node = checkNodeCount(rule, 214);

            if (i == 0) {
                System.out.println(" first call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            if (i == 9) {
                System.out.println(" 10th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            if (i == 10) {
                System.out.println(" 11th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            try {
                if (i == 48) {
                    System.out.println(" 49th call :");
                    System.out.println(" flowControl = " + result +
                            " mincount = " + node.minCount +
                            " midcount = " + node.midCount +
                            " maxcount = " + node.maxCount);
                    System.out.println("sleep 1 minute");
                    Thread.sleep(60000);
                    System.out.println();
                }
            }catch (InterruptedException e){
                System.out.println(" InterruptedException ");
            }

            if (i == 49) {
                System.out.println(" 50th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            if (i == 50) {
                System.out.println(" 51th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }

        }


        System.out.println("key1 = 258, call times = 9 ");
        for (int i = 0; i < 9; i++) {
            result = manager.reportAndCheck(rule, 258);
            node = checkNodeCount(rule, 258);
        }
        System.out.println(" flowControl = " + result +
                " mincount = " + node.minCount +
                " midcount = " + node.midCount +
                " maxcount = " + node.maxCount);
        System.out.println();
        System.out.println("key2 = 564, call times = 8 ");
        for (int i = 0; i < 8; i++) {
            result = manager.reportAndCheck(rule, 564);
            node = checkNodeCount(rule, 564);
        }
        System.out.println(" flowControl = " + result +
                " mincount = " + node.minCount +
                " midcount = " + node.midCount +
                " maxcount = " + node.maxCount);
        System.out.println();
    }

    private static void testShmCallerIp() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        boolean result = false;
        CounterNode node = null;
        rule.app = "com.today.servers0";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 200;

        System.out.println("test callerIp:----------------------------------------");

        for (int i = 0; i < 100; i++){

            result = manager.reportAndCheck(rule, 2145463247);
            node = checkNodeCount(rule,2145463247);

            if ( i == 0){
                System.out.println(" first call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            if (i == 19){
                System.out.println(" 20th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            if (i == 20){
                System.out.println(" 21th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            try {
                if (i == 78) {
                    System.out.println(" 79th call :");
                    System.out.println(" flowControl = " + result +
                            " mincount = " + node.minCount +
                            " midcount = " + node.midCount +
                            " maxcount = " + node.maxCount);
                    System.out.println("sleep 1 minute");
                    Thread.sleep(60000);
                    System.out.println();
                }
            }catch (InterruptedException e){
                System.out.println(" InterruptedException ");
            }
            if (i == 79){
                System.out.println(" 80th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }
            if (i == 80){
                System.out.println(" 81th call :");
                System.out.println(" flowControl = " + result +
                        " mincount = " + node.minCount +
                        " midcount = " + node.midCount +
                        " maxcount = " + node.maxCount);
                System.out.println();
            }

        }


        System.out.println("key1 = 2147463665, call times = 19 ");
        for (int i = 0; i < 19; i++) {
            result = manager.reportAndCheck(rule, 2147463665);
        }
        System.out.println(" flowControl = " + result +
                " mincount = " + node.minCount +
                " midcount = " + node.midCount +
                " maxcount = " + node.maxCount);
        System.out.println();
        System.out.println("key2 = 2147463651, call times = 18 ");
        for (int i = 0; i < 18; i++) {
            result = manager.reportAndCheck(rule, 2147463651);
        }
        System.out.println(" flowControl = " + result +
                " mincount = " + node.minCount +
                " midcount = " + node.midCount +
                " maxcount = " + node.maxCount);
        System.out.println();

    }

    private static void testShmId() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        CounterNode node = null;
        System.out.println("test ID:----------------------------------------------");
        System.out.println();
        System.out.println("insert new item");
        rule.app = "com.today.servers1";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;

        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 216);
        System.out.println();

        rule.app = "com.today.servers2";
        rule.ruleType = "callId";
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 214);
        System.out.println();

        rule.app = "com.today.servers3";
        rule.ruleType = "callId";
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 214);
        System.out.println();

        rule.app = "com.today.servers4";
        rule.ruleType = "callId";
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 214);
        System.out.println();

        System.out.println("check item");
        System.out.println();
        rule.app = "com.today.servers5";
        rule.ruleType = "callIp";
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 214);
        node = checkNodeCount(rule,214);
        if (node.minCount == 1){
            System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214 insert new ");
        }else{
            System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214 is in the shm ");
        }

        rule.app = "com.today.servers2";
        rule.ruleType = "callId";
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 214);
        node = checkNodeCount(rule,214);
        if (node.minCount == 1){
            System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214 insert new ");
        }else{
            System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214 is in the shm ");
        }
        System.out.println();

        rule.app = "com.today.servers6";
        rule.ruleType = "callIp";
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214");
        manager.reportAndCheck(rule, 214);
        node = checkNodeCount(rule,214);
        if (node.minCount == 1){
            System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214 insert new ");
        }else{
            System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214 is in the shm ");
        }
        System.out.println();

    }

    private static void processMutilDiff() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {

        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        CounterNode node = null;
        Thread t = Thread.currentThread();

        rule.app = "com.today.servers1";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;
        long t1 = System.nanoTime();
        for (int i = 0; i <500; i++) {
            manager.reportAndCheck(rule,325);
            node = checkNodeCount(rule,325);

        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:325"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println("threadname: "+t.getName()+" cost = "+ (System.nanoTime() - t1));
        System.out.println("threadname: "+t.getName()+" mincout = " + node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();

        rule.app = "com.today.servers2";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;
        t1 = System.nanoTime();
        for (int i = 0; i <500; i++) {
            manager.reportAndCheck(rule, 222);
            node = checkNodeCount(rule,222);
        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:222"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println("threadname: "+t.getName()+" cost = "+ (System.nanoTime() - t1));
        System.out.println("threadname: "+t.getName()+" mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();

        rule.app = "com.today.servers3";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 10;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 50;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 80;
        t1 = System.nanoTime();
        for (int i = 0; i <500; i++) {
            manager.reportAndCheck(rule, 654);
            node = checkNodeCount(rule,654);
        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:654"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println("threadname: "+t.getName()+" cost = "+ (System.nanoTime() - t1));
        System.out.println("threadname: "+t.getName()+" mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();

    }

    private static void testShmMutilDiff(){

        System.out.println("test MutilDiff:---------------------------------------");

        new Thread(() -> {
            try {
                processMutilDiff();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                processMutilDiff();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private static void processMutilSame() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {

        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        CounterNode node = null;
        Thread t = Thread.currentThread();

        rule.app = "com.today.servers1";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;
        long t1 = System.nanoTime();
        for (int i = 0; i <10000; i++) {
            manager.reportAndCheck(rule, 326);
            node = checkNodeCount(rule,326);

        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:325"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println("threadname: "+t.getName()+" cost = "+ (System.nanoTime() - t1));
        System.out.println("threadname: "+t.getName()+" mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();


    }

    private static void testShmMutilSame() throws InterruptedException {

        Thread.sleep(1000);

        System.out.println("test MutilSame:---------------------------------------");

        new Thread(() -> {
            try {
                processMutilSame();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                processMutilSame();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private static void testShmSingDiff() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        CounterNode node = null;

        System.out.println("test SingDiff:----------------------------------------");
        rule.app = "com.today.servers1";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;

        long t1 = System.nanoTime();
        for (int i = 0; i <500; i++) {
            manager.reportAndCheck(rule, 215);
            node = checkNodeCount(rule,215);
        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println( " cost = "+ (System.nanoTime() - t1));
        System.out.println( " mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();

        rule.app = "com.today.servers2";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;

        t1 = System.nanoTime();
        for (int i = 0; i <500; i++) {
            manager.reportAndCheck(rule, 2147463647);
            node = checkNodeCount(rule,2147463647);
        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:2147463647"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println( " cost = "+ (System.nanoTime() - t1));
        System.out.println( " mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();

        rule.app = "com.today.servers3";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 10;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 50;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 80;
        t1 = System.nanoTime();
        for (int i = 0; i <500; i++) {
            manager.reportAndCheck(rule, 400);
            node = checkNodeCount(rule,400);

        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:400"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println( " cost = "+ (System.nanoTime() - t1));
        System.out.println( " mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();
    }

    private static void testShmSingSame() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        CounterNode node = null;

        System.out.println("test SingSame:----------------------------------------");
        rule.app = "com.today.servers1";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 100;

        long t1 = System.nanoTime();
        for (int i = 0; i <10000; i++) {
            manager.reportAndCheck(rule, 214);
            node = checkNodeCount(rule,214);
        }
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:214"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println( " cost = "+ (System.nanoTime() - t1));
        System.out.println( " mincout = " +  node.minCount +
                " midcount = " +  node.midCount +
                " maxcount = " +  node.maxCount);
        System.out.println();
    }

    private static void testShmPerformance(){
        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        rule.app = "com.today.servers0";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 80;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 200;

        System.out.println("test performance:-------------------------------------");

        System.out.println();
        System.out.println("app:" + rule.app + ", ruleType:" + rule.ruleType + ", key:2147483647"+ ", freqRule:["
                + rule.minInterval + "," + rule.maxReqForMinInterval + "/"
                + rule.midInterval + "," + rule.maxReqForMidInterval + "/"
                + rule.maxInterval + "," + rule.maxReqForMaxInterval + "];");
        System.out.println();

        long t1 = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            manager.reportAndCheck(rule, 2147483647);
        }
        System.out.println("cost1:" + (System.nanoTime() - t1));
        System.out.println();

    }

    public static void main(String[] args) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, InterruptedException {

          testShmCallerId();

          testShmCallerIp();

          testShmId();

          testShmSingDiff();

          testShmSingSame();

          testShmPerformance();

          testShmMutilDiff();

          testShmMutilSame();


        }
    }
