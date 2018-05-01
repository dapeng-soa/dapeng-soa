package com.github.dapeng.impl.filters;

import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Root Page 4K(version:1, nodePageCount:128K, rootPageLock(i32), nextUtf8offset, nextDictionId, resolved)
 * Dictionary Root：12K
 * Dictionary Data: 128K
 * Node Page: 128M
 * <p>
 * todo
 * volatile 是否需要每个字节都如此操作?
 *
 * @author ever
 */
public class MMapUtil {
    private final static int FREE_LOCK = 0;
    private final static short VERSION = 1;
    private final static short NODE_PAGE_COUNT = (short) (128 * 1024);
    private final static long DICTION_ROOT_OFFSET = 4096;
    private final static long DICTION_DATA_OFFSET = DICTION_ROOT_OFFSET + 12 * 1024;
    private final static long NODE_PAGE_OFFSET = DICTION_DATA_OFFSET + 128 * 1024;
    private final static int TOTAL_MEM_BYTES = 4 * 1024 + 12 * 1024 + 128 * 1024 + 128 * 1024 * 1024;
    private Unsafe unsafe;
    private final Map<String, Short> localIdCache = new ConcurrentHashMap<>(64);

    /**
     * 共享内存起始地址
     */
    private long homeAddr;

    /**
     * @param app 服务名称
     * @param rule_type 规则名称
     * @param key 目前仅支持 int 值，例如 callerIp值， userIp值等。如果是字符串，需要先取hash，再按此进行限流。
     * @return
     */
    public boolean reportAndCheck(String app, String rule_type, int key) {
        boolean result = false;

        int appId = getId(app);
        int ruleTypeId = getId(rule_type);

        int nodeHash =  (appId << 16 | ruleTypeId) ^ key;

        int nodePageIndex = nodeHash % NODE_PAGE_COUNT;

        return result;
    }

    private void init() throws NoSuchFieldException, IllegalAccessException, IOException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        File file = new File("/data/shm.data");
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        MappedByteBuffer buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1024);

        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);

        homeAddr = (Long) address.get(buffer);

        short version = unsafe.getShortVolatile(null, homeAddr);
        if (version != VERSION) {
            constructShm();
        }
    }

    private void close() {

    }

    /**
     * 构建并初始化共享内存
     */
    private void constructShm() {
        if (getRootLock(hashCode())) {
            long offset = homeAddr;
            // version=1 i16
            unsafe.putShortVolatile(null, offset, VERSION);
            offset += Short.BYTES;
            // nodePageCount=64K i16
            unsafe.putShortVolatile(null, offset, NODE_PAGE_COUNT);
            offset += Short.BYTES;
            // RootPageLock i32
            offset += Integer.BYTES;
            // nextUtf8offset i16
            unsafe.putShortVolatile(null, offset, (short) 0);
            offset += Short.BYTES;
            // nextDictionId i16
            unsafe.putShortVolatile(null, offset, (short) 1);
            offset += Short.BYTES;

            //set rest of mem to 0
            unsafe.setMemory(null, offset, TOTAL_MEM_BYTES - 12, (byte) 0);
            freeRootLock();
        }
    }

    /**
     * 获取id
     *
     * @param key
     * @return
     */
    private int getId(String key) {
        Short id = localIdCache.get(key);
        if (id == null) {
            id = getIdFromShm(key);
            localIdCache.put(key, id);
        }

        return id;
    }

    /**
     * todo 字符串比较算法
     * @param key
     * @return
     */
    private short getIdFromShm(String key) {
        short id = 0;

        try {
            getSpinRootLock(key.hashCode());

            short dictionaryItemInfoSize = (short) 6;
            long dictionaryItemAddr = homeAddr + DICTION_ROOT_OFFSET;
            byte[] keyBytes = key.getBytes("utf-8");

            while ((dictionaryItemAddr < homeAddr + DICTION_DATA_OFFSET)
                    && (id = unsafe.getShort(dictionaryItemAddr + Short.BYTES)) > 0) {
                boolean foundId = true;
                // length(i16), id(i16), offset(i16),
                short length = unsafe.getShortVolatile(null, dictionaryItemAddr);
                if (length == keyBytes.length) {
                    short utf8offset = unsafe.getShortVolatile(null, dictionaryItemAddr + Short.BYTES * 2);
                    byte[] bytes = new byte[length];
                    for (short i = 0; i < length; i++) {
                        //Volatile or not?
                        bytes[i] = unsafe.getByte(homeAddr + DICTION_DATA_OFFSET + utf8offset + i);
                        if ((bytes[i] & keyBytes[i]) != keyBytes[i]) {
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

            // id not found, just create one
            id = (short)(unsafe.getShortVolatile(null, homeAddr + 10) + 1);
            short nextUtf8offset = unsafe.getShortVolatile(null, homeAddr + 8);
            // update RootPage
            nextUtf8offset += keyBytes.length;
            unsafe.putShortVolatile(null, homeAddr + 8, nextUtf8offset);
            unsafe.putShortVolatile(null, homeAddr + 10, id);
            // create a dictionaryItem
            unsafe.putShortVolatile(null, dictionaryItemAddr, (short) keyBytes.length);
            unsafe.putShortVolatile(null, dictionaryItemAddr + 2, id);
            unsafe.putShortVolatile(null, dictionaryItemAddr + 4, nextUtf8offset);
            // create an item for dictionary data
            long dictDataOffset = homeAddr + DICTION_DATA_OFFSET + nextUtf8offset*2;
            for (int i = 0; i < keyBytes.length; i++) {
                unsafe.putByteVolatile(null,  dictDataOffset + i, keyBytes[i]);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            freeRootLock();
        }

        return id;
    }

    /**
     * 自旋方式获得RootLock
     *
     * @param nodeHash
     */
    private void getSpinRootLock(int nodeHash) {
        while (!getRootLock(nodeHash)) ;
    }

    /**
     * 获取RootLock
     *
     * @return
     */
    private boolean getRootLock(int nodeHash) {
        return unsafe.compareAndSwapInt(null,
                homeAddr + Integer.BYTES, FREE_LOCK,
                nodeHash);
    }

    private void freeRootLock() {
        unsafe.putIntVolatile(null, homeAddr + Integer.BYTES, FREE_LOCK);
    }


    public static void main(String[] args) throws NoSuchFieldException, IOException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); //Internal reference
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        File file = new File("/data/shm.data");

        RandomAccessFile access = new RandomAccessFile(file, "rw");

        MappedByteBuffer buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1024);

        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);

        long addr = (Long) address.get(buffer);

        unsafe.putShort(addr, (short) 1024);
        unsafe.putShort(addr + 2, (short) 1023);

        System.out.println(unsafe.getShort(addr));
        System.out.println(unsafe.getShort(addr + 110));

        System.out.println("please input value for testing");

        Scanner scanner = new Scanner(System.in);

        String cmd = scanner.nextLine();
        while (!cmd.trim().equals("exit")) {
            if (cmd.startsWith("set")) {
                String[] segs = cmd.split(" ");
                unsafe.getAndSetInt(null, addr, Integer.valueOf(segs[1]));
//                buffer.putInt(Integer.valueOf(segs[1]));
                System.out.println("set to homeAddr:" + addr + " succeed");
            } else if (cmd.equals("get")) {
                int it = unsafe.getInt(addr);
                System.out.println("homeAddr:" + addr + ", value:" + it);
            }
            cmd = scanner.nextLine();
        }


//        System.out.println(unsafe.getInt(homeAddr));
//
//        buffer.putInt(0x31_32_33_34);
//
//        System.out.println("buffer = " + buffer + " homeAddr = " + homeAddr);
//
//        int it = unsafe.getInt(homeAddr);
//        System.out.println("it = " + it); // 0x34333231
//
//        boolean set = unsafe.compareAndSwapInt(null, homeAddr, 0x34333231, 0x35363738);
//
//        System.out.println("set = " + set);
//        System.out.println("it = " + unsafe.getInt(homeAddr));
    }
}
