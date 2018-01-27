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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令行参数构建者
 */
public class CommandLineBuilder {
    /**
     * 获取可执行文件
     */
    public static File findExecutable(File root, String path) {
        String npath = path.replace('/',File.separatorChar);
        File exe = new File(root,npath);
        if (!exe.exists()) {
            return null;
        }
        return exe;
    }

    /**
     * 获取java启动命令
     *
     * @return
     */
    public static String findJavaBin() {
        File javaHome = new File(System.getProperty("java.home"));
        if (!javaHome.exists()) {
            return null;
        }

        File javabin = findExecutable(javaHome,"bin/java");
        if (javabin != null) {
            return javabin.getAbsolutePath();
        }

        javabin = findExecutable(javaHome,"bin/java.exe");
        if (javabin != null) {
            return javabin.getAbsolutePath();
        }

        // 相当于一个降级措施
        return "java";
    }

    /**
     * Perform an optional quoting of the argument, being intelligent with spaces and quotes as needed.
     * If a subString is set in quotes it won't the subString won't be escaped.
     *
     * 转义
     *
     * @param arg
     * @return
     */
    public static String quote(String arg) {
        // 如果没有空格或者没有双引号，那么无需转义
        boolean needsQuoting = (arg.indexOf(' ') >= 0) || (arg.indexOf('"') >= 0);
        if (!needsQuoting) {
            return arg;
        }

        // 开始执行转义操作
        StringBuilder buf = new StringBuilder();
        // buf.append('"');
        boolean escaped = false;
        boolean quoted = false;
        for (char c : arg.toCharArray()) {
            if (!quoted && !escaped && ((c == '"') || (c == ' '))) {
                buf.append("\\");
            }
            // don't quote text in single quotes
            if (!escaped && (c == '\'')) {
                quoted = !quoted;
            }
            escaped = (c == '\\');
            buf.append(c);
        }
        // buf.append('"');
        return buf.toString();
    }

    /**
     * 字符串列表
     */
    private List<String> args;

    /**
     * 构造方法
     */
    public CommandLineBuilder() {
        args = new ArrayList<String>();
    }

    /**
     * 构造方法
     *
     * @param bin
     */
    public CommandLineBuilder(String bin) {
        this();
        args.add(bin);
    }

    /**
     * Add a simple argument to the command line.
     * <p>
     * Will quote arguments that have a space in them.
     *
     * 添加一个简单的命令行
     * 注意这里要进行转义
     *
     * @param arg
     *            the simple argument to add
     */
    public void addArg(String arg) {
        if (arg != null) {
            args.add(quote(arg));
        }
    }

    /**
     * Similar to {@link #addArg(String)} but concats both name + value with an "=" sign, quoting were needed, and excluding the "=" portion if the value is
     * undefined or empty.
     * <p>
     * 
     * <pre>
     *   addEqualsArg("-Dname", "value") = "-Dname=value"
     *   addEqualsArg("-Djetty.home", "/opt/company inc/jetty (7)/") = "-Djetty.home=/opt/company\ inc/jetty\ (7)/"
     *   addEqualsArg("-Djenkins.workspace", "/opt/workspaces/jetty jdk7/") = "-Djenkins.workspace=/opt/workspaces/jetty\ jdk7/"
     *   addEqualsArg("-Dstress", null) = "-Dstress"
     *   addEqualsArg("-Dstress", "") = "-Dstress"
     * </pre>
     *
     * 添加带有等号的命令行参数
     *
     * @param name
     *            the name
     * @param value
     *            the value
     */
    public void addEqualsArg(String name, String value) {
        if ((value != null) && (value.length() > 0)) {
            args.add(quote(name + "=" + value));
        } else {
            args.add(quote(name));
        }
    }

    /**
     * Add a simple argument to the command line.
     * <p>
     * Will <b>NOT</b> quote/escape arguments that have a space in them.
     *
     * 添加一个原始数据
     * 这里和addArg最大的区别是它不进行转义
     *
     * @param arg
     *            the simple argument to add
     */
    public void addRawArg(String arg) {
        if (arg != null) {
            args.add(arg);
        }
    }

    /**
     * 获取参数列表
     *
     * @return
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    @Override
    public String toString() {
        return toString(" ");
    }

    /**
     * 转换为字符串
     * 可以定制分隔符
     *
     * @param delim
     * @return
     */
    public String toString(String delim) {
        StringBuilder buf = new StringBuilder();

        for (String arg : args) {
            if (buf.length()>0) {
                buf.append(delim);
            }
            buf.append(quote(arg));
        }

        return buf.toString();
    }

    /**
     * 显示调试信息
     */
    public void debug() {
        if (!StartLog.isDebugEnabled()) {
            return;
        }

        int len = args.size();
        StartLog.debug("Command Line: %,d entries",args.size());
        for (int i = 0; i < len; i++) {
            StartLog.debug(" [%d]: \"%s\"",i,args.get(i));
        }
    }
}
