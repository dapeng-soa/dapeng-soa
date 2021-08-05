/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.plugins.scheduler;

import org.springframework.scheduling.support.CronSequenceGenerator;

import java.text.SimpleDateFormat;
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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        if(date == null) {
            date = calendar.getTime();
        }
        calendar.add(Calendar.MILLISECOND, -1);
        Date nextDate = generator.next(calendar.getTime());
        long count = 0;
        while (simpleDateFormat.format(nextDate).equals(simpleDateFormat.format(date))){
            count++;
            nextDate = generator.next(nextDate);
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
        System.out.println(count("0 00 12 4 * ?"));
    }
}
