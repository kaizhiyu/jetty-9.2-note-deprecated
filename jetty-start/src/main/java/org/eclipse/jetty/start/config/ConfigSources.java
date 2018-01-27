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

package org.eclipse.jetty.start.config;

import static org.eclipse.jetty.start.UsageException.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.start.FS;
import org.eclipse.jetty.start.Props;
import org.eclipse.jetty.start.RawArgs;
import org.eclipse.jetty.start.Props.Prop;
import org.eclipse.jetty.start.UsageException;

/**
 * Weighted List of ConfigSources.
 * <p>
 *
 * 配置源列表
 */
public class ConfigSources implements Iterable<ConfigSource> {

    /**
     * 按照权重来排序的配置源比较器
     */
    private static class WeightedConfigSourceComparator implements Comparator<ConfigSource> {
        @Override
        public int compare(ConfigSource o1, ConfigSource o2)
        {
            return o1.getWeight() - o2.getWeight();
        }
    }


    /**
     * 链表
     */
    private LinkedList<ConfigSource> sources = new LinkedList<>();

    /**
     * 所有的属性
     */
    private Props props = new Props();

    /**
     * 原子性自增整数
     */
    private AtomicInteger sourceWeight = new AtomicInteger(1);

    /**
     * 添加配置源
     *
     * @param source
     * @throws IOException
     */
    public void add(ConfigSource source) throws IOException {
        if (sources.contains(source)) {
            // TODO: needs a better/more clear error message
            // 说明已经重复
            throw new UsageException(ERR_BAD_ARG,"Duplicate Configuration Source Reference: " + source);
        }
        sources.add(source);

        // 按照权重进行排序
        Collections.sort(sources,new WeightedConfigSourceComparator());

        updateProps();

        // look for --include-jetty-dir entries
        // 获取 --include-jetty-dir 这个属性
        for (RawArgs.Entry arg : source.getArgs()) {
            if (arg.startsWith("--include-jetty-dir")) {
                String ref = getValue(arg.getLine());
                String dirName = props.expand(ref);
                Path dir = FS.toPath(dirName);
                DirConfigSource dirsource = new DirConfigSource(ref,dir,sourceWeight.incrementAndGet(),true);
                add(dirsource);
            }
        }
    }

    /**
     * 获取命令行配置源
     *
     * @return
     */
    public CommandLineConfigSource getCommandLineSource() {
        for (ConfigSource source : sources) {
            if (source instanceof CommandLineConfigSource) {
                return (CommandLineConfigSource)source;
            }
        }
        return null;
    }

    /**
     * 获取单个属性
     *
     * @param key
     * @return
     */
    public Prop getProp(String key) {
        return props.getProp(key);
    }

    /**
     * 获取所有属性
     *
     * @return
     */
    public Props getProps() {
        return props;
    }

    /**
     * 获取特定的值
     *
     * @param arg
     * @return
     */
    private String getValue(String arg) {
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
     * 迭代
     *
     * @return
     */
    @Override
    public Iterator<ConfigSource> iterator() {
        return sources.iterator();
    }

    /**
     * 解析迭代列表
     *
     * @return
     */
    public ListIterator<ConfigSource> reverseListIterator() {
        return sources.listIterator(sources.size());
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getClass().getSimpleName());
        str.append('[');
        boolean delim = false;
        for (ConfigSource source : sources) {
            if (delim) {
                str.append(',');
            }
            str.append(source.getId());
            delim = true;
        }
        str.append(']');
        return str.toString();
    }

    /**
     * 更新所有的属性
     */
    private void updateProps() {
        props.reset();

        // add all properties from config sources (in reverse order)
        ListIterator<ConfigSource> iter = sources.listIterator(sources.size());
        while (iter.hasPrevious()) {
            ConfigSource source = iter.previous();
            props.addAll(source.getProps());
        }
    }
}
