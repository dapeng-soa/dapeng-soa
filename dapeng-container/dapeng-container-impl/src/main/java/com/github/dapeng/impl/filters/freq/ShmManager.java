package com.github.dapeng.impl.filters.freq;

import com.github.dapeng.core.FreqControlRule;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
     * 每个NodePage的容量
     */
    private final static short MAX_NODE_PER_PAGE = 42;
    /**
     * 整块共享内存的大小(4K+12K+128K+128M)
     */
    private final static int TOTAL_MEM_BYTES = 134365184;

    /**
    * 基准时间 2018-08-01 00:00:00
    */
    private final static long BASE_TIME_MILLIS = 1533052800000L;
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

    /**
     * 持有buffer的强引用, 以防止该对象给gc回收
     */
    private MappedByteBuffer buffer;

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
     * ShmManager 初始化
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IOException
     */
    private void init() throws NoSuchFieldException, IllegalAccessException, IOException {
        LOGGER.info(getClass().getSimpleName() + "::init begin");
        long t1 = System.nanoTime();
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        File file = new File(System.getProperty("user.home") + "/shm.data");
        RandomAccessFile access = new RandomAccessFile(file, "rw");

        buffer = access.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, TOTAL_MEM_BYTES);

        Field address = Buffer.class.getDeclaredField("address");
        address.setAccessible(true);

        homeAddr = (Long) address.get(buffer);

        short version = getShort(homeAddr);
        if (version != VERSION) {
            constructShm();
        }

        LOGGER.info(getClass().getSimpleName() + "::init end, homeAddr:" + homeAddr + ", cost:" + (System.nanoTime() - t1));
    }


    /**
     * 限流入口，检测每一次请求，返回true 表示 access ，返回false 表示被限流
     *
     * @param rule 规则对象
     * @param key  目前仅支持 int 值，例如 userId， userIp值等。
     *             如果是字符串，需要先取hash，再按此进行限流。
     * @return true access , false can't access
     */
    public boolean reportAndCheck(FreqControlRule rule, int key) {
        boolean result;
        short appId = allocId(rule.app);
        short ruleTypeId = allocId(rule.ruleType);
        //key若为负数，则取绝对值用于计算nodePageHash，主要针对IP地址为负数的情况
        int keyTemp = key < 0 ? Math.abs(key):key;
        int nodePageHash = (appId << 16 | ruleTypeId) ^ keyTemp;
        int nodePageIndex = nodePageHash % NODE_PAGE_COUNT;
        //以2018-08-01 00:00:00 为基准，计算到目前时间的秒数
        int now = (int) ((System.currentTimeMillis() - BASE_TIME_MILLIS) / 1000);

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
                nodePageMeta = new NodePageMeta(nodePageHash, (short) 1);
                node = insertCounterNode(appId, ruleTypeId, key, now, nodeAddr);
            } else if (nodePageMeta.nodes == MAX_NODE_PER_PAGE) {
                // 淘汰老node
                node = eliminateAndInsertNodes(nodePageMeta, appId, ruleTypeId, key, now, nodeAddr, rule);
            } else {
                node = createNodeIfNotExist(appId, ruleTypeId, key,
                        nodePageMeta, now,
                        nodeAddr, rule);
                LOGGER.debug("createNodeIfNotExist returned");
            }

            result = node.minCount <= rule.maxReqForMinInterval
                    && node.midCount <= rule.maxReqForMidInterval
                    && node.maxCount <= rule.maxReqForMaxInterval;
            updateNodePageMeta(nodePageMeta, nodePageIndex);
            LOGGER.debug("updateNodePageMeta nodePageIndex = {}", nodePageIndex);
        } finally {
            freeNodePageLock(nodePageIndex);
            LOGGER.debug("freeNodePageLock {}", nodePageIndex);
        }

        LOGGER.debug("reportAndCheck end, result:{}, cost:{}", result, System.nanoTime() - t1);
        return result;
    }

    /**
     * 根据指定的限流规则以及指定key, 得到该key当前的计数情况
     *
     * @param app
     * @param ruleType
     * @param key
     * @return
     * @throws IOException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public String getCounterInfo(String app, String ruleType, int key) {
        short appId = getId(app);
        if (appId <= 0) {
            return "[" + app + "/" + ruleType + "/" + key + ":0/0/0]";
        }
        short ruleTypeId = getId(ruleType);

        if (ruleTypeId <= 0) {
            return "[" + app + "/" + ruleType + "/" + key + ":0/0/0]";
        }

        int keyTemp = key < 0 ? Math.abs(key):key;
        int nodePageHash = (appId << 16 | ruleTypeId) ^ keyTemp;
        int nodePageIndex = nodePageHash % NODE_PAGE_COUNT;
        long nodeAddr = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex + 16;
        NodePageMeta nodePageMeta = getNodePageMeta(nodePageIndex);

        CounterNode node = null;
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

            //timestamp
            nodeAddr += Integer.BYTES;
            int timestamp = getInt(nodeAddr);

            nodeAddr += Integer.BYTES;
            int minCount = getInt(nodeAddr);

            nodeAddr += Integer.BYTES;
            int midCount = getInt(nodeAddr);

            nodeAddr += Integer.BYTES;
            int maxCount = getInt(nodeAddr);
            node = new CounterNode(appId, ruleTypeId, key, timestamp, minCount, midCount, maxCount);
            break;
        }
        if (node == null) {
            return "[" + app + "/" + ruleType + "/" + key + ":0/0/0]";
        } else {
            return "[" + app + "/" + ruleType + "/" + key + ":" + node.minCount + "/" + node.midCount + "/" + node.maxCount + "]";
        }
    }

    /**
     * 获取id, 注意如果不存在则返回0
     *
     * @param key
     * @return
     */
    public short getId(final String key) {
        Short id = localStringIdCache.get(key);
        if (id == null) {
            id = allocIdAndCache(key, true);
        }

        return id;
    }


    /**
     * 从 share memory 获取CountNode，如果不存在就创建 <br/>
     * 内存结构如下:
     * <pre>
     * struct CounterNode {
     *   i16 app_id;  // app 被映射为 16bit id
     *   i16 rule_type_id;  // rule_type_id 被映射为 16bit id
     *   i32 key;  // ip, userId, callerMid etc.
     *   i32 timestamp;  // last updated unix epoch, seconds since 1970.
     *   i32 min_count;  // min interval counter
     *   i32 mid_count;  // mid interval counter
     *   i32 max_count;  // max interval counter
     * }
     * </pre>
     */
    private CounterNode createNodeIfNotExist(short appId, short ruleTypeId, int key,
                                             NodePageMeta nodePageMeta, int now, long nodeAddr,
                                             FreqControlRule rule) {
        long t1 = System.nanoTime();

        CounterNode node = null;
        for (int index = 0; index < nodePageMeta.nodes; index++) {
            short _appId = getShort(nodeAddr);
            // 已经遍历完所有节点
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
                //skip rest bytes
                nodeAddr += Integer.BYTES * 5;
                continue;
            }

            nodeAddr += Integer.BYTES;
            long lastTimeStamp = getAndSetInt(nodeAddr, now);
            boolean isSameMin = (now % 86400) / rule.minInterval == (lastTimeStamp % 86400) / rule.minInterval;
            boolean isSameMid = (now % 86400) / rule.midInterval == (lastTimeStamp % 86400) / rule.midInterval;
            boolean isSameMax = now / 86400 == lastTimeStamp / 86400;

            int minCount, midCount, maxCount;
            nodeAddr += Integer.BYTES;
            if (isSameMin) {
                minCount = getAndIncreseInt(nodeAddr) + 1;
            } else {
                minCount = 1;
                putInt(nodeAddr, minCount);
            }

            nodeAddr += Integer.BYTES;
            if (isSameMid) {
                midCount = getAndIncreseInt(nodeAddr) + 1;
            } else {
                midCount = 1;
                putInt(nodeAddr, midCount);
            }

            nodeAddr += Integer.BYTES;
            if (isSameMax) {
                maxCount = getAndIncreseInt(nodeAddr) + 1;
            } else {
                maxCount = 1;
                putInt(nodeAddr, maxCount);
            }

            node = new CounterNode(appId, ruleTypeId, key, now,
                    minCount, midCount, maxCount);
            LOGGER.debug("createNodeIfNotExist, found node:{}, index:{}, cost:{}", node, index, System.nanoTime() - t1);
            break;
        }

        if (node == null) {
            LOGGER.debug("createNodeIfNotExist, node not found, index:{}, cost:{}", nodePageMeta.nodes, System.nanoTime() - t1);
            node = insertCounterNode(appId, ruleTypeId, key, now, nodeAddr);

            nodePageMeta.increaseNode();
        }

        return node;
    }

    /**
     * 向 share memory 插入CountNode
     */
    private CounterNode insertCounterNode(short appId, short ruleTypeId, int key,
                                          int timestamp, long addr) {
        long t1 = System.nanoTime();
        CounterNode node = new CounterNode(appId, ruleTypeId, key, timestamp, 1, 1, 1);

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


    /**
     * 淘汰算法,如果一个nodePage里的node数超过了42个，再次插入时，需要淘汰已插入node
     */
    private CounterNode eliminateAndInsertNodes(NodePageMeta nodePageMeta, short appId, short ruleTypeId, int key, int now, long nodeAddr, FreqControlRule rule) {
        long t1 = System.nanoTime();
        List<MarkNode> markNodes = new ArrayList<>(64);
        for (int index = 0; index < nodePageMeta.nodes; index++) {
            MarkNode markNode = new MarkNode(nodeAddr + index * 24);

            CounterNode node = getCounterNode(nodeAddr);
            nodeAddr += 24;
            processNodeRate(node, markNode, rule, now);
            markNode.key = node.key;
            markNodes.add(markNode);
        }
        long postion = eliminateNode(markNodes);
        LOGGER.debug("eliminateAndInsertNode, index:{}, cost:{}", nodePageMeta.nodes, System.nanoTime() - t1);
        return insertCounterNode(appId, ruleTypeId, key, now, postion);
    }


    /**
     * 从 nodePage 获取 node 值
     * 内存结构如下:
     * <pre>
     * struct CounterNode {
     *   i16 app_id;  // app 被映射为 16bit id
     *   i16 rule_type_id;  // rule_type_id 被映射为 16bit id
     *   i32 key;  // ip, userId, callerMid etc.
     *   i32 timestamp;  // last updated unix epoch, seconds since 1970.
     *   i32 min_count;  // min interval counter
     *   i32 mid_count;  // mid interval counter
     *   i32 max_count;  // max interval counter
     * }
     * </pre>
     *
     * @param nodeAddr
     * @return
     */
    private CounterNode getCounterNode(final long nodeAddr) {
        return new CounterNode(getShort(nodeAddr), getShort(nodeAddr + 2),
                getInt(nodeAddr + 4), getInt(nodeAddr + nodeAddr + 8),
                getInt(nodeAddr + 12), getInt(nodeAddr + 16), getInt(nodeAddr + 20));
    }

    /**
     * process node rate due to eliminate
     *
     * @param node
     * @param markNode
     * @param rule
     * @param now
     */
    private void processNodeRate(CounterNode node, MarkNode markNode, FreqControlRule rule, int now) {
        boolean isSameMin = (now % 86400) / rule.minInterval == (node.timestamp % 86400) / rule.minInterval;
        boolean isSameMid = (now % 86400) / rule.midInterval == (node.timestamp % 86400) / rule.midInterval;
        boolean isSameMax = now / 86400 == node.timestamp / 86400;

        int minCount = node.minCount;
        int midCount = node.midCount;
        int maxCount = node.maxCount;

        if (!isSameMin) {
            minCount = 0;
        }
        if (!isSameMid) {
            midCount = 0;
        }
        if (!isSameMax) {
            maxCount = 0;
        }

        double minRate = minCount / rule.maxReqForMinInterval * rule.minInterval / (now % rule.minInterval + 1);
        double midRate = midCount / rule.maxReqForMidInterval * rule.midInterval / (now % rule.midInterval + 1);
        double maxRate = maxCount / rule.maxReqForMaxInterval * rule.maxInterval / (now % rule.maxInterval + 1);

        double rate;
        if (minRate < midRate) {
            if (midRate < maxRate) {
                rate = maxRate;
            } else {
                rate = midRate;
            }
        } else {
            if (minRate < maxRate) {
                rate = maxRate;
            } else {
                rate = minRate;
            }
        }

        if (rate < 0.1) {
            markNode.isRemove = true;
        } else {
            // 如果没有 rate < 0.1的节点，则 筛选 rate 最小的2个节点，删除之。
            markNode.rate = rate;
        }
    }

    /**
     * do get position
     */
    private long eliminateNode(List<MarkNode> markNodes) {
        List<MarkNode> collect = markNodes.stream().filter(node -> node.isRemove).collect(Collectors.toList());
        if (collect.size() > 0) {
            return collect.get(0).position;
        }
        List<MarkNode> sortList = markNodes.stream().sorted((n1, n2) -> (int) (n1.rate - n2.rate)).collect(Collectors.toList());
        long postion = sortList.get(0).position;

        return postion;
    }

    /**
     * 更新 nodePage 元信息
     *
     * @param nodePageMeta
     * @param nodePageIndex
     */
    private void updateNodePageMeta(NodePageMeta nodePageMeta, int nodePageIndex) {
        long pageOffset = homeAddr + NODE_PAGE_OFFSET + 1024 * nodePageIndex + Integer.BYTES;
        putInt(pageOffset, nodePageMeta.hash);
        pageOffset += Integer.BYTES;
        putShort(pageOffset, nodePageMeta.nodes);
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
     *
     * @param key
     * @return
     */
    private short allocId(final String key) {
        Short id = localStringIdCache.get(key);
        LOGGER.debug("allocId, from cache, key:{} -> {}", key, id);
        if (id == null) {
            long t1 = System.nanoTime();
            id = allocIdAndCache(key, false);
            LOGGER.debug("allocId, from shm, key:{} -> {}, cost:{}",
                    key, id, System.nanoTime() - t1);
        }

        return id;
    }

    /**
     * 分配并缓存Id(如果已分配的话, 仅需从共享内存中加载即可)
     *
     * @param key
     * @param readOnly 是否仅加载id(id不存在的话, 不会分配)
     * @return
     */
    private short allocIdAndCache(final String key, boolean readOnly) {
        short id = 0;

        try {
            long t1 = System.nanoTime();
            getSpinRootLock(key.hashCode());
            LOGGER.debug("allocIdAndCache acquire spinRootLock cost:{}", System.nanoTime() - t1);

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
                        LOGGER.debug("allocIdAndCache found existId:{} for key:{}, cost:{}", id, key, System.nanoTime() - t1);
                        return id;
                    }
                }
                dictionaryItemAddr += dictionaryItemInfoSize;
            }

            if (readOnly) {
                return id;
            }

            if (dictionaryItemAddr >= homeAddr + DICTION_DATA_OFFSET) {
                String errorMsg = "allocIdAndCache dictionRoot is full, homeAddr:" + homeAddr + ", currentAddr:" + dictionaryItemAddr;
                LOGGER.error(errorMsg);
                throw new IndexOutOfBoundsException(errorMsg);
            }

            // id not found, just create one
            id = (short) (getShort(homeAddr + 10) + 1);
            short nextUtf8offset = getShort(homeAddr + 8);
            // create a dictionaryItem
            putShort(dictionaryItemAddr, (short) keyBytes.length);
            putShort(dictionaryItemAddr + 2, id);
            putShort(dictionaryItemAddr + 4, nextUtf8offset);
            // create an item for dictionary data
            long dictDataOffset = homeAddr + DICTION_DATA_OFFSET + nextUtf8offset * 2;
            for (int i = 0; i < keyBytes.length; i++) {
                putByte(dictDataOffset + i, keyBytes[i]);
            }
            // update RootPage
            putShort(homeAddr + 8, (short) (nextUtf8offset + keyBytes.length));
            putShort(homeAddr + 10, id);

            LOGGER.debug("allocIdAndCache create id:{} for key:{}, cost:{}", id, key, System.nanoTime() - t1);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            // 本地cache
            if (id > 0) {
                localStringIdCache.put(key, id);
            }
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
        // skip the pageLock
        return new NodePageMeta(getInt(pageOffset + Integer.BYTES),
                getShort(pageOffset + Integer.BYTES * 2));
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

    /**
     * 获取自旋锁
     *
     * @param nodePageIndex
     */
    private void getSpinNodePageLock(int nodePageIndex) {
        while (!getNodePageLock(nodePageIndex)) ;
    }


    /**
     * 0 表示锁是空闲的， 1 表示 锁被占有， 这里比较并set 所以要加 1 ， 从而占有锁
     *
     * @param nodeIndex
     * @return
     */
    private boolean getNodePageLock(int nodeIndex) {
        return unsafe.compareAndSwapInt(null, homeAddr + NODE_PAGE_OFFSET + 1024 * nodeIndex, FREE_LOCK, nodeIndex + 1);
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


    public static void main(String[] args) throws InterruptedException {
        final ShmManager manager = ShmManager.getInstance();

        final FreqControlRule rule = new FreqControlRule();
        rule.app = "com.today.hello";
        rule.ruleType = "callId";
        rule.minInterval = 60;
        rule.maxReqForMinInterval = 20;
        rule.midInterval = 3600;
        rule.maxReqForMidInterval = 100;
        rule.maxInterval = 86400;
        rule.maxReqForMaxInterval = 500;

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            for (int j = 0; j < 100; j++) {
                manager.reportAndCheck(rule, j);

            }
        }
        long t2 = System.currentTimeMillis();

        System.out.println((t2 - t1));


       /* new Thread( ()-> { ttt(manager, rule, 100); }).start();
        new Thread( ()-> { ttt(manager, rule, 101); }).start();
        new Thread( ()-> { ttt(manager, rule, 102); }).start();
        new Thread( ()-> { ttt(manager, rule, 103); }).start();*/


//        System.out.println("cost1:" + (System.currentTimeMillis() - t1));
//
//
//        for (int j = 0; j < 10; j++) {
//            t1 = System.currentTimeMillis();
//            for (int i = 0; i < 1000000; i++) {
//                manager.reportAndCheck(rule, 100);
//            }
//            System.out.println("cost2:" + (System.currentTimeMillis() - t1));
//        }
    }

    static void ttt(ShmManager manager, FreqControlRule rule, int key) {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            manager.reportAndCheck(rule, key);
        }
        long t2 = System.currentTimeMillis();
        System.out.println(Thread.currentThread() + " cost:" + (t2 - t1) + "ms");
    }
}
