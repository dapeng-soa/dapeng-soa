package com.github.dapeng.core.helper;


public class DapengUtilTest {
    public static void main(String[] args) throws InterruptedException {
        Long tid = DapengUtil.generateTid();
        System.out.println("finally: " + DapengUtil.longToHexStr(tid));
        long begin = System.currentTimeMillis();
        long counter = 0;
        while (true) {
            long newTid = DapengUtil.generateTid();
            counter++;
            if (newTid == tid) {
                System.out.println("counter:" + counter + " end:" + (System.currentTimeMillis() - begin));
                System.out.println(DapengUtil.longToHexStr(newTid));
                begin = System.currentTimeMillis();
            }
        }
    }
}
