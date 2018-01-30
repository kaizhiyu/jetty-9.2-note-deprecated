//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.eclipse.jetty.util.StringUtil;

/**
 * ThreadLocal Date formatters for HTTP style dates.
 *
 * 日期生成器
 */
public class DateGenerator {

    /**
     * 时区
     */
    private static final TimeZone __GMT = TimeZone.getTimeZone("GMT");


    static {
        __GMT.setID("GMT");
    }

    /**
     * 一周的七天
     * 注意这里两个周六
     */
    static final String[] DAYS = { "Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    /**
     * 一年中的十二个月份
     * 注意这里两个一月
     */
    static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan"};


    /**
     * 在ThreadLocal内保存日期
     */
    private static final ThreadLocal<DateGenerator> __dateGenerator =new ThreadLocal<DateGenerator>() {
        @Override
        protected DateGenerator initialValue() {
            return new DateGenerator();
        }
    };


    /**
     * 时间戳开始的时间
     */
    public final static String __01Jan1970 = DateGenerator.formatDate(0);
    
    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
     *
     * 格式化时间
     */
    public static String formatDate(long date) {
        return __dateGenerator.get().doFormatDate(date);
    }

    /**
     * Format "EEE, dd-MMM-yyyy HH:mm:ss 'GMT'" for cookies
     *
     * 从时间戳格式化cookie的日期
     */
    public static void formatCookieDate(StringBuilder buf, long date) {
        __dateGenerator.get().doFormatCookieDate(buf,date);
    }

    /**
     * Format "EEE, dd-MMM-yyyy HH:mm:ss 'GMT'" for cookies
     *
     * 从时间戳格式化cookie的日期
     */
    public static String formatCookieDate(long date) {
        StringBuilder buf = new StringBuilder(28);
        formatCookieDate(buf, date);
        return buf.toString();
    }

    /**
     * 字符串数据缓存
     */
    private final StringBuilder buf = new StringBuilder(32);


    /**
     * 格林尼治时间日历
     */
    private final GregorianCalendar gc = new GregorianCalendar(__GMT);

    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
     *
     * 对日期进行格式化
     */
    public String doFormatDate(long date) {
        buf.setLength(0);
        gc.setTimeInMillis(date);

        int day_of_week = gc.get(Calendar.DAY_OF_WEEK);
        int day_of_month = gc.get(Calendar.DAY_OF_MONTH);
        int month = gc.get(Calendar.MONTH);
        int year = gc.get(Calendar.YEAR);
        int century = year / 100;
        year = year % 100;

        int hours = gc.get(Calendar.HOUR_OF_DAY);
        int minutes = gc.get(Calendar.MINUTE);
        int seconds = gc.get(Calendar.SECOND);

        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        StringUtil.append2digits(buf, day_of_month);

        buf.append(' ');
        buf.append(MONTHS[month]);
        buf.append(' ');
        StringUtil.append2digits(buf, century);
        StringUtil.append2digits(buf, year);

        buf.append(' ');
        StringUtil.append2digits(buf, hours);
        buf.append(':');
        StringUtil.append2digits(buf, minutes);
        buf.append(':');
        StringUtil.append2digits(buf, seconds);
        buf.append(" GMT");
        return buf.toString();
    }

    /**
     * Format "EEE, dd-MMM-yy HH:mm:ss 'GMT'" for cookies
     *
     * 格式化时间
     */
    public void doFormatCookieDate(StringBuilder buf, long date) {
        gc.setTimeInMillis(date);

        int day_of_week = gc.get(Calendar.DAY_OF_WEEK);
        int day_of_month = gc.get(Calendar.DAY_OF_MONTH);
        int month = gc.get(Calendar.MONTH);
        int year = gc.get(Calendar.YEAR);
        year = year % 10000;

        int epoch = (int) ((date / 1000) % (60 * 60 * 24));
        int seconds = epoch % 60;
        epoch = epoch / 60;
        int minutes = epoch % 60;
        int hours = epoch / 60;

        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        StringUtil.append2digits(buf, day_of_month);

        buf.append('-');
        buf.append(MONTHS[month]);
        buf.append('-');
        StringUtil.append2digits(buf, year/100);
        StringUtil.append2digits(buf, year%100);

        buf.append(' ');
        StringUtil.append2digits(buf, hours);
        buf.append(':');
        StringUtil.append2digits(buf, minutes);
        buf.append(':');
        StringUtil.append2digits(buf, seconds);
        buf.append(" GMT");
    }
}
