package com.github.dapeng.impl.filters;

import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * @author ever
 */
public class ShmManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShmManager.class.getName());
    private static final ShmManager instance = new ShmManager();
    /**
     * 自旋锁标志, 0为free
     */
    private final static int FREE_LOCK = 0;
    /**
     * 限流内存结构版本
     */
    private final static short VERSION = 1;
    /**
     * nodePage数量, 128K
     */
    private final static int NODE_PAGE_COUNT = 128 * 1024;
    /**
     * DictionRoot域的地址偏移量, 该域占用12K
     */
    private final static long DICTION_ROOT_OFFSET = 4096;
    /**
     * DictionData域的地址偏移量, 该域占用128K
     */
    private final static long DICTION_DATA_OFFSET = DICTION_ROOT_OFFSET + 12 * 1024;
    /**
     * NodePage域的地址偏移量, 该域占用128M
     */
    private final static long NODE_PAGE_OFFSET = DICTION_DATA_OFFSET + 128 * 1024;
    /**
     * 每个NodePage的容量
     */
    private final static short MAX_NODE_PER_PAGE = 42;
    /**
     * 整块共享内存的大小(4K+12K+128K+128M)
     */
    private final static int TOTAL_MEM_BYTES = 134365184;
    /**
     * 内存操作对象
     */
    private Unsafe unsafe;
    /**
     * 本地字符串ID映射表
     */
    private final Map<String, Short> localStringIdCache = new ConcurrentHashMap<>(64);

    /**
     * 共享内存起始地址
     */
    private long homeAddr;

    private ShmManager() {
        try {
            init();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static ShmManager getInstance() {
        return instance;
    }

    /**
     * 限流规则
     */
    static class FreqControlRule {
        String app;
        String ruleType;
        int minInterval;             //每分钟请求数[min,max]
        int maxReqForMinInterval;
        int midInterval;             //每小时请求数[min,max]
        int maxReqForMidInterval;
        int maxInterval;             //每天请求数[min,max]
        int maxReqForMaxInterval;

        @Override
        public String toString() {
            return "app:" + app + ", ruleType:" + ruleType + ", freqRule:["
                    + minInterval + "," + maxReqForMinInterval + "/"
                    + midInterval + "," + maxReqForMidInterval + "/"
                    + maxInterval + "," + maxReqForMaxInterval + ";";
        }
    }

    static class DictionaryItem {
        short length;
        int id;
        short utf8offset; // DictionaryData[ 2 * utf8offset ] 处开始存储这个字符串。
    }

    /**
     * nodePage元数据
     */
    static class NodePageMeta {
        int pageLock;
        int hash;
        short nodes;

        @Override
        public String toString() {
            return "hash:" + hash + ", nodes:" + nodes;
        }
    }

    /**
     * 计数节点
     */
    static class CounterNode {
        short appId;  // app 被映射为 16bit id
        short ruleTypeId;  // rule_type_id 被映射为 16bit id
        int key;  // ip, userId, callerMid etc.
        int timestamp;  // last updated unix epoch, seconds since 1970.
        int minCount;  // min interval counter
        int midCount;  // mid interval counter
        int maxCount;  // max interval counter

        @Override
        public String toString() {
            return "appId:" + appId + ", ruleTypeId:" + ruleTypeId + ", key:" + key
                    + ", timestamp:" + timestamp + ", counters:" + minCount + "/" + midCount + "/" + maxCount;
        }
    }

    /**
     * @param rule 规则对象
     * @param key  目前仅支持 int 值，例如 userId， userIp值等。
     *             如果是字符串，需要先取hash，再按此进行限流。
     * @return
     */
    public boolean reportAndCheck(FreqControlRule rule, int key) {
        boolean result;
        short appId = getId(rule.app);
        short ruleTypeId = getId(rule.ruleType);

        int nodePageHash = (appId << 16 | ruleTypeId) ^ key;
        int nodePageIndex = nodePageHash % NODE_PAGE_COUNT;
        int now = (int) (System.currentTimeMillis() / 1000) % 86400;


        LOGGER.debug("reportAndCheck, rule:{}, nodePageHash:{}, nodePageIndex:{}, timestamp:{}, appId/ruleTypeId/key:{}/{}/{}",
                rule, nodePageHash, nodePageIndex, now, appId, ruleTypeId, key);

        long t1 = System.nanoTime();

        try {
            getSpinNodePageLock(nodePageIndex);
            LOGGER.debug("reportAndCheck, acquire spin lock cost:{}", System.nanoTime() - t1);

            CounterNode node = null;
            long nodeAddr = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex + 16;

            NodePageMeta nodePageMeta = getNodePageMeta(nodePageIndex);
            LOGGER.debug("reportAndCheck, nodePageMeta:{}, cost:{}", nodePageMeta, System.nanoTime() - t1);
            if (nodePageMeta.nodes == 0) {
                nodePageMeta.hash = nodePageHash;
                nodePageMeta.nodes = 1;
                node = insertCounterNode(appId, ruleTypeId, key, now, nodeAddr);
            } else if (nodePageMeta.nodes == MAX_NODE_PER_PAGE) {
                //todo 淘汰老node
            } else {
                node = createNodeIfNotExist(appId, ruleTypeId, key,
                        nodePageMeta, now,
                        nodeAddr, rule);
            }

            result = node.minCount <= rule.maxReqForMinInterval
                    && node.midCount <= rule.maxReqForMidInterval
                    && node.maxCount <= rule.maxReqForMaxInterval;
            updateNodePageMeta(nodePageMeta, nodePageIndex);
        } finally {
            freeNodePageLock(nodePageIndex);
        }

        LOGGER.debug("reportAndCheck end, result:{}, cost:{}", result, System.nanoTime() - t1);
        return result;
    }

    private CounterNode createNodeIfNotExist(short appId, short ruleTypeId, int key,
                                             NodePageMeta nodePageMeta, int now, long nodeAddr,
                                             FreqControlRule rule) {
        long t1 = System.nanoTime();

        CounterNode node = null;
        for (int index = 0; index < nodePageMeta.nodes; index++) {
            short _appId = getShort(nodeAddr);
            if (_appId == 0) break;

            if (appId != _appId) continue;

            nodeAddr += Short.BYTES;
            short _ruleTypeId = getShort(nodeAddr);
            if (ruleTypeId != _ruleTypeId) continue;

            nodeAddr += Short.BYTES;
            int _key = getInt(nodeAddr);
            if (key != _key) {
                //skip rest bytes
                nodeAddr += Integer.BYTES * 4;
                continue;
            }

            node = new CounterNode();
            node.appId = _appId;
            node.ruleTypeId = _ruleTypeId;
            node.key = _key;

            nodeAddr += Integer.BYTES;
            node.timestamp = getAndSetInt(nodeAddr, now);

            boolean isSameMin = now / rule.minInterval == node.timestamp / rule.minInterval;
            boolean isSameMid = now / rule.midInterval == node.timestamp / rule.midInterval;
            boolean isSameMax = now / rule.maxInterval == node.timestamp / rule.maxInterval;

            nodeAddr += Integer.BYTES;
            if (isSameMin) {
                node.minCount = getAndIncreseInt(nodeAddr) + 1;
            } else {
                node.minCount = 1;
                putInt(nodeAddr, node.minCount);
            }

            nodeAddr += Integer.BYTES;
            if (isSameMid) {
                node.midCount = getAndIncreseInt(nodeAddr) + 1;
            } else {
                node.midCount = 1;
                putInt(nodeAddr, node.midCount);
            }

            nodeAddr += Integer.BYTES;
            if (isSameMax) {
                node.maxCount = getAndIncreseInt(nodeAddr) + 1;
            } else {
                node.maxCount = 1;
                putInt(nodeAddr, node.maxCount);
            }

            node.timestamp = now;
            LOGGER.debug("createNodeIfNotExist, found node:{}, index:{}, cost:{}", node, index, System.nanoTime() - t1);
            break;
        }

        if (node == null) {
            LOGGER.debug("createNodeIfNotExist, node not found, index:{}, cost:{}", nodePageMeta.nodes, System.nanoTime() - t1);
            node = insertCounterNode(appId, ruleTypeId, key, now, nodeAddr);
            nodePageMeta.nodes += 1;
        }

        return node;
    }

    private CounterNode insertCounterNode(short appId, short ruleTypeId, int key,
                                          int timestamp, long addr) {
        long t1 = System.nanoTime();
        CounterNode node = new CounterNode();
        node.appId = appId;
        node.ruleTypeId = ruleTypeId;
        node.key = key;
        node.timestamp = timestamp;
        node.minCount = 1;
        node.midCount = 1;
        node.maxCount = 1;

        putShort(addr, node.appId);
        addr += Short.BYTES;
        putShort(addr, node.ruleTypeId);
        addr += Short.BYTES;
        putInt(addr, node.key);
        addr += Integer.BYTES;
        putInt(addr, node.timestamp);
        addr += Integer.BYTES;
        putInt(addr, node.minCount);
        addr += Integer.BYTES;
        putInt(addr, node.midCount);
        addr += Integer.BYTES;
        putInt(addr, node.maxCount);

        LOGGER.debug("insertCounterNode node:{}, cost:{}", node, System.nanoTime() - t1);
        return node;
    }

    private void updateNodePageMeta(NodePageMeta nodePageMeta, int nodePageIndex) {
        long pageOffset = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex + Integer.BYTES;
        putInt(pageOffset, nodePageMeta.hash);
        pageOffset += Integer.BYTES;
        putShort(pageOffset, nodePageMeta.nodes);
    }

    private void init() throws NoSuchFieldException, IllegalAccessException, IOException {
        LOGGER.info(getClass().getSimpleName() + "::init begin");
        long t1 = System.nanoTime();
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        File file = new File("/data/shm.data");
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        MappedByteBuffer buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, TOTAL_MEM_BYTES);

        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);

        homeAddr = (Long) address.get(buffer);

        short version = getShort(homeAddr);
        if (version != VERSION) {
            constructShm();
        }

        LOGGER.info(getClass().getSimpleName() + "::init end, homeAddr:" + homeAddr + ", cost:" + (System.nanoTime() - t1));
    }

    private void close() {

    }

    /**
     * 构建并初始化共享内存
     */
    private void constructShm() {
        if (getRootLock(hashCode())) {
            LOGGER.warn(getClass().getSimpleName() + "::constructShm begin");
            long addr = homeAddr;
            // version=1 i16
            putShort(addr, VERSION);
            addr += Short.BYTES;
            // nodePageCount=64K i16
            putInt(addr, NODE_PAGE_COUNT);
            addr += Integer.BYTES;
            // RootPageLock i32
            addr += Integer.BYTES;
            // nextUtf8offset i16
            putShort(addr, (short) 0);
            addr += Short.BYTES;
            // nextDictionId i16
            putShort(addr, (short) 1);
            addr += Short.BYTES;

            //set rest of mem to 0
            unsafe.setMemory(null, addr, TOTAL_MEM_BYTES - 14, (byte) 0);
            freeRootLock();
            LOGGER.warn(getClass().getSimpleName() + "::constructShm end");
        }
    }

    /**
     * 获取id
     * 先从本地获取key值，如果不存在则从共享内存中查找获取
     * @param key
     * @return
     */
    private short getId(final String key) {
        Short id = localStringIdCache.get(key);
        LOGGER.debug("getId, from cache, key:{} -> {}", key, id);
        if (id == null) {
            long t1 = System.nanoTime();
            id = getIdFromShm(key);
            LOGGER.debug("getId, from shm, key:{} -> {}, cost:{}",
                    key, id, System.nanoTime() - t1);
            localStringIdCache.put(key, id);
        }

        return id;
    }

    /**
     * todo 字符串比较算法
     *
     * @param key
     * @return
     */
    private short getIdFromShm(final String key) {
        short id = 0;

        try {
            long t1 = System.nanoTime();
            getSpinRootLock(key.hashCode());
            LOGGER.debug("getIdFromShm acquire spinRootLock cost:{}", System.nanoTime() - t1);

            short dictionaryItemInfoSize = (short) Short.BYTES * 3;
            long dictionaryItemAddr = homeAddr + DICTION_ROOT_OFFSET;
            byte[] keyBytes = key.getBytes("utf-8");

            while ((dictionaryItemAddr < homeAddr + DICTION_DATA_OFFSET)
                    && (id = getShort(dictionaryItemAddr + Short.BYTES)) > 0) {
                boolean foundId = true;
                // length(i16), id(i16), offset(i16),
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
                        LOGGER.debug("getIdFromShm found existId:{} for key:{}, cost:{}", id, key, System.nanoTime() - t1);
                        return id;
                    }
                }
                dictionaryItemAddr += dictionaryItemInfoSize;
            }

            if (dictionaryItemAddr >= homeAddr + DICTION_DATA_OFFSET) {
                String errorMsg = "getIdFromShm dictionRoot is full, homeAddr:" + homeAddr + ", currentAddr:" + dictionaryItemAddr;
                LOGGER.error(errorMsg);
                throw new IndexOutOfBoundsException(errorMsg);
            }

            // id not found, just create one
            id = (short) (getShort(homeAddr + 10) + 1);
            short nextUtf8offset = (short) (getShort(homeAddr + 8) + keyBytes.length);
            // update RootPage
            putShort(homeAddr + 8, nextUtf8offset);
            putShort(homeAddr + 10, id);
            // create a dictionaryItem
            putShort(dictionaryItemAddr, (short) keyBytes.length);
            putShort(dictionaryItemAddr + 2, id);
            putShort(dictionaryItemAddr + 4, nextUtf8offset);
            // create an item for dictionary data
            long dictDataOffset = homeAddr + DICTION_DATA_OFFSET + nextUtf8offset * 2;
            for (int i = 0; i < keyBytes.length; i++) {
                putByte(dictDataOffset + i, keyBytes[i]);
            }
            LOGGER.debug("getIdFromShm create id:{} for key:{}, cost:{}", id, key, System.nanoTime() - t1);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            freeRootLock();
        }

        return id;
    }

    /**
     * 拿到某个NodePage的元数据信息
     *
     * @param nodePageIndex
     * @return
     */
    private NodePageMeta getNodePageMeta(int nodePageIndex) {
        long pageOffset = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex;
        NodePageMeta meta = new NodePageMeta();
        // skip the pageLock
        pageOffset += Integer.BYTES;
        meta.hash = getInt(pageOffset);
        pageOffset += Integer.BYTES;
        meta.nodes = getShort(pageOffset);

        return meta;
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
     * <p>
     * # unsafe.compareAndSwapInt 参数说明
     * 1）第一个参数为需要改变的对象
     * 2）偏移量(即之前求出来的valueOffset的值
     * 3）期待的值
     * 4）更新后的值
     *
     * @return
     */
    private boolean getRootLock(int nodeHash) {
        return unsafe.compareAndSwapInt(null,
                homeAddr + Integer.BYTES, FREE_LOCK,
                nodeHash);
    }

    private void freeRootLock() {
        putInt(homeAddr + Integer.BYTES, FREE_LOCK);
    }

    private void getSpinNodePageLock(int nodePageIndex) {      //获取自旋锁
        while (!getNodePageLock(nodePageIndex)) ;
    }

    private boolean getNodePageLock(int nodeIndex) {
        return unsafe.compareAndSwapInt(null, homeAddr + NODE_PAGE_OFFSET + 1024 * nodeIndex, FREE_LOCK, nodeIndex);
    }

    private void freeNodePageLock(int nodeIndex) {
        putInt(homeAddr + NODE_PAGE_OFFSET + 1024 * nodeIndex, FREE_LOCK);
    }

    private int getInt(long addr) {
        return unsafe.getInt(null, addr);
    }

    private int getAndSetInt(long addr, int newValue) {
        return unsafe.getAndSetInt(null, addr, newValue);
    }

    private int getAndIncreseInt(long addr) {
        return unsafe.getAndAddInt(null, addr, 1);
    }

    private void putInt(long addr, int value) {
        unsafe.putInt(null, addr, value);
    }

    private short getShort(long addr) {
        return unsafe.getShort(null, addr);
    }

    private void putShort(long addr, short value) {
        unsafe.putShort(null, addr, value);
    }

    private byte getByte(long addr) {
        return unsafe.getByte(null, addr);
    }

    private void putByte(long addr, byte value) {
        unsafe.putByte(null, addr, value);
    }


    public static void main(String[] args) {
        ShmManager manager = ShmManager.getInstance();

        FreqControlRule rule = new FreqControlRule();
        rule.app = "com.today.hello";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 100;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 500;

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            manager.reportAndCheck(rule, 100);
        }

        System.out.println("cost1:" + (System.currentTimeMillis() - t1));

        t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            manager.reportAndCheck(rule, 100);
        }

        System.out.println("cost2:" + (System.currentTimeMillis() - t1));
    }
}
