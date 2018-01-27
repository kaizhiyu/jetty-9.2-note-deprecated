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

package org.eclipse.jetty.start;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.start.Props.Prop;

import static org.eclipse.jetty.start.UsageException.ERR_BAD_ARG;

/**
 * Management of Properties.
 * <p>
 * This is larger in scope than the standard {@link java.util.Properties}, as it will also handle tracking the origin of each property, if it was overridden,
 * and also allowing for <code>${property}</code> expansion.
 *
 * 属性
 * 它比jdk自带的java.util.Properties要强大很多
 */
public final class Props implements Iterable<Prop> {


    /**
     * 单个属性
     */
    public static class Prop {
        /**
         * 键
         */
        public String key;

        /**
         * 值
         */
        public String value;

        /**
         * 来源
         * 是来自命令行、还是系统自带的等等
         */
        public String origin;

        /**
         * 覆盖
         * 其实是它的外部类中的引用
         */
        public Prop overrides;

        /**
         * 构造方法
         *
         * @param key
         * @param value
         * @param origin
         */
        public Prop(String key, String value, String origin) {
            this.key = key;
            this.value = value;
            this.origin = origin;
        }

        /**
         * 构造方法
         *
         * @param key
         * @param value
         * @param origin
         * @param overrides
         */
        public Prop(String key, String value, String origin, Prop overrides) {
            this(key,value,origin);
            this.overrides = overrides;
        }

        /**
         * 转换为字符串
         *
         * @return
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Prop [key=");
            builder.append(key);
            builder.append(", value=");
            builder.append(value);
            builder.append(", origin=");
            builder.append(origin);
            builder.append(", overrides=");
            builder.append(overrides);
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * 原始的系统属性
     */
    public static final String ORIGIN_SYSPROP = "<system-property>";

    /**
     * 获取值
     *
     * @param arg
     * @return
     */
    public static String getValue(String arg) {
        int idx = arg.indexOf('=');
        if (idx == (-1)) {
            throw new UsageException(ERR_BAD_ARG,"Argument is missing a required value: %s",arg);
        }
        String value = arg.substring(idx + 1).trim();
        if (value.length() <= 0) {
            throw new UsageException(ERR_BAD_ARG,"Argument is missing a required value: %s",arg);
        }
        return value;
    }

    /**
     * 获取值
     *
     * @param arg
     * @return
     */
    public static List<String> getValues(String arg) {
        String v = getValue(arg);
        ArrayList<String> l = new ArrayList<>();
        for (String s : v.split(",")) {
            if (s != null) {
                s = s.trim();
                if (s.length() > 0) {
                    l.add(s);
                }
            }
        }
        return l;
    }

    /**
     * 属性映射
     * 这里是大小写不敏感的
     */
    private Map<String, Prop> props = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * 属性列表
     */
    private List<String> sysPropTracking = new ArrayList<>();

    /**
     * 添加另一个属性集
     *
     * @param other
     */
    public void addAll(Props other) {
        this.props.putAll(other.props);
        this.sysPropTracking.addAll(other.sysPropTracking);
    }
    
    /**
     * Add a potential argument as a property.
     * <p>
     * If arg is not a property, ignore it.
     *
     * 添加属性
     *
     * @param arg the argument to parse for a potential property
     * @param source the source for this argument (to track origin of property from)
     */
    public boolean addPossibleProperty(String arg, String source) {
        // Start property (syntax similar to System property)
        // 带有-D参数
        if (arg.startsWith("-D")) {
            String[] assign = arg.substring(2).split("=",2);
            switch (assign.length) {
                case 2:
                    setSystemProperty(assign[0],assign[1]);
                    setProperty(assign[0],assign[1],source);
                    return true;
                case 1:
                    setSystemProperty(assign[0],"");
                    setProperty(assign[0],"",source);
                    return true;
                default:
                    return false;
            }
        }

        // Is this a raw property declaration?
        // 是否有原始属性的声明
        int idx = arg.indexOf('=');
        if (idx >= 0) {
            String key = arg.substring(0,idx);
            String value = arg.substring(idx + 1);

            setProperty(key,value,source);
            return true;
        }

        // All other strings are ignored
        // 忽略其他字符串
        return false;
    }

    /**
     * 处理引用
     * 这里主要是取得它引用的属性名
     *
     * @param property
     * @return
     */
    public String cleanReference(String property) {
        String name = property.trim();
        if (name.startsWith("${") && name.endsWith("}")) {
            name = name.substring(2,name.length() - 1);
        }
        return name.trim();
    }

    /**
     * 是否包含某个key
     *
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        return props.containsKey(key);
    }

    /**
     * 扩展
     *
     * @param str
     * @return
     */
    public String expand(String str) {
        return expand(str,new Stack<String>());
    }

    /**
     * 扩展
     *
     * @param str
     * @param seenStack
     * @return
     */
    public String expand(String str, Stack<String> seenStack) {
        if (str == null) {
            return str;
        }

        if (str.indexOf("${") < 0) {
            // Contains no potential expressions.
            return str;
        }

        Pattern pat = Pattern.compile("(?<=[^$]|^)(\\$\\{[^}]*\\})");
        Matcher mat = pat.matcher(str);
        StringBuilder expanded = new StringBuilder();
        int offset = 0;
        String property;
        String value;

        while (mat.find(offset)) {
            property = cleanReference(mat.group(1));

            // Loop detection
            if (seenStack.contains(property)) {
                StringBuilder err = new StringBuilder();
                err.append("Property expansion loop detected: ");
                int idx = seenStack.lastIndexOf(property);
                for (int i = idx; i < seenStack.size(); i++) {
                    err.append(seenStack.get(i));
                    err.append(" -> ");
                }
                err.append(property);
                throw new PropsException(err.toString());
            }

            // find property name
            expanded.append(str.subSequence(offset,mat.start(1)));
            // get property value
            value = getString(property);
            if (value == null) {
                StartLog.trace("Unable to expand: %s",property);
                expanded.append(mat.group(1));
            } else {
                // recursively expand
                seenStack.push(property);
                value = expand(value,seenStack);
                seenStack.pop();
                expanded.append(value);
            }
            // update offset
            offset = mat.end(1);
        }

        // leftover
        expanded.append(str.substring(offset));

        // special case for "$$"
        // 解析$$的特定情况
        if (expanded.indexOf("$$") >= 0) {
            return expanded.toString().replaceAll("\\$\\$","\\$");
        }

        return expanded.toString();
    }

    /**
     * 获取单个属性
     *
     * @param key
     * @return
     */
    public Prop getProp(String key) {
        return getProp(key,true);
    }

    /**
     * 获取单个属性
     *
     * @param key
     * @param searchSystemProps
     * @return
     */
    public Prop getProp(String key, boolean searchSystemProps) {
        Prop prop = props.get(key);
        if ((prop == null) && searchSystemProps) {
            // try system property
            prop = getSystemProperty(key);
        }
        return prop;
    }

    /**
     * 获取属性的字符串形式
     * 可能抛出属性相关的异常
     *
     * @param key
     * @return
     */
    public String getString(String key) {
        if (key == null) {
            throw new PropsException("Cannot get value for null key");
        }

        String name = cleanReference(key);

        if (name.length() == 0) {
            throw new PropsException("Cannot get value for empty key");
        }

        Prop prop = getProp(name);
        if (prop == null) {
            return null;
        }
        return prop.value;
    }

    /**
     * 获取属性的字符串形式
     * 并且带有默认值
     *
     * @param key
     * @param defVal
     * @return
     */
    public String getString(String key, String defVal) {
        String val = getString(key);
        if (val == null) {
            return defVal;
        }
        return val;
    }

    /**
     * 获取系统属性
     *
     * @param key
     * @return
     */
    private Prop getSystemProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            return null;
        }
        return new Prop(key,value,ORIGIN_SYSPROP);
    }

    /**
     * 是否存在某个属性
     *
     * @param name
     * @return
     */
    public static boolean hasPropertyKey(String name) {
        return Pattern.compile("(?<=[^$]|^)(\\$\\{[^}]*\\})").matcher(name).find();
    }

    /**
     * 获取迭代器
     *
     * @return
     */
    @Override
    public Iterator<Prop> iterator() {
        return props.values().iterator();
    }

    /**
     * 重置
     */
    public void reset() {
        props.clear();
    }

    /**
     * 添加一个属性
     *
     * @param prop
     */
    public void setProperty(Prop prop) {
        props.put(prop.key,prop);
    }

    /**
     * 设置属性
     *
     * @param key
     * @param value
     * @param origin
     */
    public void setProperty(String key, String value, String origin) {
        Prop prop = props.get(key);
        if (prop == null) {
            prop = new Prop(key,value,origin);
        } else {
            prop = new Prop(key,value,origin,prop);
        }
        props.put(key,prop);
    }

    /**
     * 获取属性数量
     *
     * @return
     */
    public int size() {
        return props.size();
    }

    /**
     * 存储
     *
     * @param stream
     * @param comments
     * @throws IOException
     */
    public void store(OutputStream stream, String comments) throws IOException {
        Properties props = new Properties();
        // add all Props as normal properties, with expansion performed.
        for (Prop prop : this) {
            props.setProperty(prop.key,expand(prop.value));
        }
        // write normal properties file
        // 持久化
        props.store(stream,comments);
    }

    /**
     * 设置系统属性
     *
     * @param key
     * @param value
     */
    public void setSystemProperty(String key, String value) {
        System.setProperty(key,value);
        sysPropTracking.add(key);
    }
}
