package com.github.dapeng.impl.listener;

import org.springframework.scheduling.support.CronSequenceGenerator;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 计算cron 执行次数
 *
 * @author huyj
 * @Created 2018-12-19 14:56
 */
public class CronCountUtils {
    /**
     * 计算一天内定时任务执行次数
     *
     * @param expression 表达式
     * @return 执行次数
     */
    public static final long count(String expression) {
        return count(null, expression);
    }

    /**
     * 计算一天内定时任务执行次数
     *
     * @param date       时间
     * @param expression 表达式
     * @return 执行次数
     */
    public static final long count(Date date, String expression) {
        CronSequenceGenerator generator = new CronSequenceGenerator(expression, TimeZone.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.MILLISECOND, -1);
        if (date == null)
            date = calendar.getTime();
        int today = calendar.get(Calendar.DATE) + 1;
        int now = today;
        long count = 0;
        while (true) {
            date = generator.next(date);
            calendar.setTime(date);
            now = calendar.get(Calendar.DATE);
            if (now == today) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println(count("0 0 2 * * ?"));
        System.out.println(count("0 0 0 * * ?"));
        System.out.println(count("0 0 0/3 * * ?"));
        System.out.println(count("0 0/5 * * * ?"));
        System.out.println(count("0 10 1 * * ?"));
        System.out.println(count("0 30 0/1 * * ?"));
    }
}
