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
 *
 * todo
 * volatile 是否需要每个字节都如此操作?
 * @author ever
 */
public class MMapUtil {
    private final static int FREE_LOCK = 0;
    private final static short VERSION = 1;
    private final static short NODE_PAGE_COUNT = (short) (128 * 1024);
    private final static long DICTION_ROOT_OFFSET = 4096;
    private final static long DICTION_DATA_OFFSET = DICTION_ROOT_OFFSET + 12 * 1024;
    private final static long NODE_PAGE_OFFSET = DICTION_DATA_OFFSET + 128 * 1024;
    private final static int TOTAL_MEM_BYTES = 4*1024 + 12*1024 + 128*1024 + 128*1024*1024;
    private Unsafe unsafe;
    private final Map<String, Short> localIdCache = new ConcurrentHashMap<>(64);

    /**
     * 共享内存起始地址
     */
    private long addr;

    private void init() throws NoSuchFieldException, IllegalAccessException, IOException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        File file = new File("/data/shm.data");
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        MappedByteBuffer buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1024);

        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);

        addr = (Long) address.get(buffer);

        short version = unsafe.getShortVolatile(null, addr);
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
            long offset = addr;
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
            unsafe.setMemory(null, offset, TOTAL_MEM_BYTES - 12, (byte)0);
            freeRootLock();
        }
    }

    /**
     * 获取id
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

    private short getIdFromShm(String key) {
        short id = 0;

        try {
            getSpinRootLock(key.hashCode());

            short dictionaryItemInfoSize = (short) 6;
            long dictionaryItemAddr = addr + DICTION_ROOT_OFFSET;
            byte[] keyBytes = key.getBytes("utf-8");

            while ((id = unsafe.getShort(dictionaryItemAddr + 2)) > 0) {
                short length = unsafe.getShortVolatile(null, dictionaryItemAddr);
                if (length == keyBytes.length) {
                    short utf8offset = unsafe.getShortVolatile(null, dictionaryItemAddr + 4);
                    byte[] bytes = new byte[length];
                    for (short i = 0; i < length; i++) {
                        bytes[i] = unsafe.getByteVolatile(null, addr + DICTION_DATA_OFFSET + utf8offset + i);
                        if ((bytes[i] & keyBytes[i]) != keyBytes[i]) {
                            break;
                        }
                    }
                    return id;
                }
                dictionaryItemAddr += dictionaryItemInfoSize;
            }

            // id not found, just create one
            id = unsafe.getShortVolatile(null, addr + 10);
            short nextUtf8offset = unsafe.getShortVolatile(null, addr + 8);
            // update RootPage
            unsafe.putShortVolatile(null, addr + 8, (short) (nextUtf8offset + keyBytes.length));
            unsafe.putShortVolatile(null, addr + 10, (short) (id + 1));
            // create a dictionaryItem
            unsafe.putShortVolatile(null, dictionaryItemAddr, (short) keyBytes.length);
            unsafe.putShortVolatile(null, dictionaryItemAddr + 2, id);
            unsafe.putShortVolatile(null, dictionaryItemAddr + 4, (short) (nextUtf8offset + keyBytes.length));
            // create an item for dictionary data
            for (int i = 0; i < keyBytes.length; i++) {
                unsafe.putByteVolatile(null, addr + DICTION_DATA_OFFSET + nextUtf8offset + i, keyBytes[i]);
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
                addr + Integer.BYTES, FREE_LOCK,
                nodeHash);
    }

    private void freeRootLock() {
        unsafe.putIntVolatile(null, addr + Integer.BYTES, FREE_LOCK);
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
                System.out.println("set to addr:" + addr + " succeed");
            } else if (cmd.equals("get")) {
                int it = unsafe.getInt(addr);
                System.out.println("addr:" + addr + ", value:" + it);
            }
            cmd = scanner.nextLine();
        }


//        System.out.println(unsafe.getInt(addr));
//
//        buffer.putInt(0x31_32_33_34);
//
//        System.out.println("buffer = " + buffer + " addr = " + addr);
//
//        int it = unsafe.getInt(addr);
//        System.out.println("it = " + it); // 0x34333231
//
//        boolean set = unsafe.compareAndSwapInt(null, addr, 0x34333231, 0x35363738);
//
//        System.out.println("set = " + set);
//        System.out.println("it = " + unsafe.getInt(addr));
    }
}
