package com.github.dapeng.util;

import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ShmUtil {
    /**
     * nodePage数量, 128K
     */
    private final static int NODE_PAGE_COUNT = 128 * 1024;
    // private final static int NODE_PAGE_COUNT = 2;
    /**
     * Root Page:存储基本的信息,该域占用4K
     */
    private final static long DICTION_ROOT_OFFSET = 4096;
    /**
     * DictionRoot域的地址偏移量, 该域占用12K
     */
    private final static long DICTION_DATA_OFFSET = DICTION_ROOT_OFFSET + 12 * 1024;
    /**
     * DictionData域的地址偏移量, 该域占用128K
     */
    private final static long NODE_PAGE_OFFSET = DICTION_DATA_OFFSET + 128 * 1024;
    /**
     * 整块共享内存的大小(4K+12K+128K+128M)
     */
    private final static int TOTAL_MEM_BYTES = 134365184;
    /**
     * 内存操作对象
     */
    private static Unsafe unsafe;
    /**
     * 共享内存起始地址
     */
    private static long homeAddr;

    private static MappedByteBuffer buffer;

    public static  class Node{
        public short appId;
        public short ruleTypeId;
        public int key;
        public int minCount;
        public int midCount;
        public int maxCount;
    }

    public static class NodePageMeta {
        public int hash;
        public short nodes;
    }

    private ShmUtil(){}
    private static ShmUtil instance = null;
    public static ShmUtil getInstance() throws NoSuchFieldException, IllegalAccessException, IOException {
        if (instance==null){
            init();
            instance = new ShmUtil();
        }
        return instance;
    }

    private static void init() throws NoSuchFieldException, IllegalAccessException, IOException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        File file = new File(System.getProperty("user.home")+"/shm.data");
        RandomAccessFile access = new RandomAccessFile(file, "rw");

        buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, TOTAL_MEM_BYTES);
        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);

        homeAddr = (Long) address.get(buffer);
    }

    public static String freqControlCount(String app, String rule_Type, int key) throws IOException, IllegalAccessException, NoSuchFieldException {

        getInstance();
        StringBuilder sb = new StringBuilder();
        Node node = null;

        short appId = getIdFromShm(app);
        short ruleTypeId = getIdFromShm(rule_Type);

        int nodePageHash = (appId << 16 | ruleTypeId) ^ key;
        int nodePageIndex = nodePageHash % NODE_PAGE_COUNT;
        long nodeAddr = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex + 16;
        NodePageMeta nodePageMeta = getNodePageMeta(nodePageIndex);

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

            node = new Node();
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
        sb.append("\n[mincount/midcount/maxcount]:\n[")
                .append(node.minCount)
                .append("/")
                .append(node.midCount)
                .append("/")
                .append(node.maxCount)
                .append("]");
        return sb.toString();
    }

    private static short getIdFromShm(String key) throws UnsupportedEncodingException {

        short id = 0;
        short dictionaryItemInfoSize = (short) Short.BYTES * 3;
        long dictionaryItemAddr = homeAddr + DICTION_ROOT_OFFSET;
        byte[] keyBytes = key.getBytes("utf-8");

        while ((dictionaryItemAddr < homeAddr + DICTION_DATA_OFFSET) && (id = getShort(dictionaryItemAddr + Short.BYTES)) > 0) {
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
        return id;
    }

    private static NodePageMeta getNodePageMeta(int nodePageIndex) {

        long pageOffset = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex;
        NodePageMeta meta = new NodePageMeta();
        pageOffset += Integer.BYTES;
        meta.hash = getInt(pageOffset);
        pageOffset += Integer.BYTES;
        meta.nodes = getShort(pageOffset);

        return meta;
    }

    private static int getInt(long addr) {
        return unsafe.getInt(null, addr);
    }

    private static short getShort(long addr) {
        return unsafe.getShort(null, addr);
    }

    private static byte getByte(long addr) {
        return unsafe.getByte(null, addr);
    }
}
