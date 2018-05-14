package com.github.dapeng.impl.filters;


import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.impl.filters.freq.ShmManager;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class testShmCallerId {
    private final static int FREE_LOCK = 0;
    private final static short VERSION = 1;
    private final static int NODE_PAGE_COUNT = 128 * 1024;
    private final static long DICTION_ROOT_OFFSET = 4096;
    private final static long DICTION_DATA_OFFSET = DICTION_ROOT_OFFSET + 12 * 1024;
    private final static long NODE_PAGE_OFFSET = DICTION_DATA_OFFSET + 128 * 1024;
    private final static short MAX_NODE_PER_PAGE = 42;
    private final static int TOTAL_MEM_BYTES = 134365184;
    private static Unsafe unsafe;
    private static long homeAddr;
    private static MappedByteBuffer buffer;

    private static ShmManager.CounterNode checkNodeCount(FreqControlRule rule, int key) throws IOException, NoSuchFieldException, IllegalAccessException {

        short appId = getIdFromShm(rule.app);
        short ruleTypeId = getIdFromShm(rule.ruleType);
        int nodePageHash = (appId << 16 | ruleTypeId) ^ key;
        int nodePageIndex = nodePageHash % NODE_PAGE_COUNT;


        getSpinNodePageLock(nodePageIndex);

        long nodeAddr = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex + 16;
        ShmManager.NodePageMeta nodePageMeta = getNodePageMeta(nodePageIndex);
        ShmManager.CounterNode node = getNodeData(appId, ruleTypeId, key, nodePageMeta, nodeAddr);

        freeNodePageLock(nodePageIndex);

        return node;
    }

    private static ShmManager.CounterNode getNodeData(short appId, short ruleTypeId, int key,
                                                      ShmManager.NodePageMeta nodePageMeta, long nodeAddr) {
        ShmManager.CounterNode node = null;
        for (int index = 0; index < nodePageMeta.nodes; index++) {
            short _appId = getShort(nodeAddr);
            if (_appId == 0) break;
            if (appId != _appId) {
                nodeAddr += 24;
                continue;
            }

            nodeAddr += Short.BYTES;
            short _ruleTypeId = getShort(nodeAddr);
            if (ruleTypeId != _ruleTypeId) {
                nodeAddr += 22;
                continue;
            }

            nodeAddr += Short.BYTES;
            int _key = getInt(nodeAddr);
            if (key != _key) {
                nodeAddr += Integer.BYTES * 5;
                continue;
            }

            node = new ShmManager.CounterNode();
            node.appId = _appId;
            node.ruleTypeId = _ruleTypeId;
            node.key = _key;

            nodeAddr += Integer.BYTES; //timestamp

            nodeAddr += Integer.BYTES;
            node.minCount = getInt(nodeAddr);

            nodeAddr += Integer.BYTES;
            node.midCount = getInt(nodeAddr);

            nodeAddr += Integer.BYTES;
            node.maxCount = getInt(nodeAddr);
            break;
        }
        return node;
    }

    private static short getIdFromShm(String key) throws NoSuchFieldException, IllegalAccessException, IOException {
        short id = 0;

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        File file = new File(System.getProperty("user.home") + "/shm.data");
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, TOTAL_MEM_BYTES);

        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);
        homeAddr = (Long) address.get(buffer);





        try {
            getSpinRootLock(key.hashCode());
            short dictionaryItemInfoSize = (short) Short.BYTES * 3;
            long dictionaryItemAddr = homeAddr + DICTION_ROOT_OFFSET;
            byte[] keyBytes = key.getBytes("utf-8");

            while ((dictionaryItemAddr < homeAddr + DICTION_DATA_OFFSET)
                    && (id = getShort(dictionaryItemAddr + Short.BYTES)) > 0) {
                boolean foundId = true;
                short length = getShort(dictionaryItemAddr);
                if (length == keyBytes.length) {
                    short utf8offset = getShort(dictionaryItemAddr + Short.BYTES * 2);
                    byte[] bytes = new byte[length];
                    long dataAddr = homeAddr + DICTION_DATA_OFFSET + utf8offset * 2;
                    for (short i = 0; i < length; i++) {
                        bytes[i] = getByte(dataAddr + i);
                        if ((bytes[i] != keyBytes[i])) {
                            foundId = false;
                            break;
                        }
                    }
                    if (foundId) {
                        return id;
                    }
                }
                dictionaryItemAddr += dictionaryItemInfoSize;
            }
        } finally {
            freeRootLock();
        }


        return id;
    }


    private static ShmManager.NodePageMeta getNodePageMeta(int nodePageIndex) {
        long pageOffset = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex;
        ShmManager.NodePageMeta meta = new ShmManager.NodePageMeta();
        pageOffset += Integer.BYTES;
        meta.hash = getInt(pageOffset);
        pageOffset += Integer.BYTES;
        meta.nodes = getShort(pageOffset);
        return meta;
    }

    private static void getSpinRootLock(int nodeHash) {
        while (!getRootLock(nodeHash)) ;
    }

    private static boolean getRootLock(int nodeHash) {
        return unsafe.compareAndSwapInt(null, homeAddr + Integer.BYTES, FREE_LOCK, nodeHash);
    }

    private static void freeRootLock() {
        putInt(homeAddr + Integer.BYTES, FREE_LOCK);
    }

    private static void getSpinNodePageLock(int nodePageIndex) {
        while (!getNodePageLock(nodePageIndex)) ;
    }

    private static boolean getNodePageLock(int nodeIndex) {
        return unsafe.compareAndSwapInt(null, homeAddr + NODE_PAGE_OFFSET + 1024 * nodeIndex, FREE_LOCK, nodeIndex);
    }

    private static void freeNodePageLock(int nodeIndex) {
        putInt(homeAddr + NODE_PAGE_OFFSET + 1024 * nodeIndex, FREE_LOCK);
    }

    private static int getInt(long addr) {
        return unsafe.getInt(null, addr);
    }

    private static void putInt(long addr, int value) {
        unsafe.putInt(null, addr, value);
    }

    private static short getShort(long addr) {
        return unsafe.getShort(null, addr);
    }

    private static byte getByte(long addr) {
        return unsafe.getByte(null, addr);
    }


    public static void main(String[] args) throws IllegalAccessException, NoSuchFieldException, IOException {


        ShmManager manager = ShmManager.getInstance();
        FreqControlRule rule = new FreqControlRule();
        boolean result = false;
        ShmManager.CounterNode node = null;
        rule.app = "com.today.hello";
        rule.ruleType = "callIp";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 10;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 50;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 80;


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
}
