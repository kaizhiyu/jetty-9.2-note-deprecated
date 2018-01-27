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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jetty.start.RawArgs.Entry;

/**
 * 原始参数列表
 *
 */
public class RawArgs implements Iterable<Entry> {

    /**
     * 原始的参数
     */
    public class Entry {
        /**
         * 行
         */
        private String line;

        /**
         * 原始
         */
        private String origin;

        /**
         * 原始参数的个体
         *
         * @param line
         * @param origin
         */
        private Entry(String line, String origin) {
            this.line = line;
            this.origin = origin;
        }

        /**
         * 获取行
         *
         * @return
         */
        public String getLine() {
            return line;
        }

        /**
         * 获取origin
         *
         * @return
         */
        public String getOrigin() {
            return origin;
        }

        /**
         * 是否以某个值开头
         *
         * @param val
         * @return
         */
        public boolean startsWith(String val) {
            return line.startsWith(val);
        }
    }

    /**
     * All of the args, in argument order
     *
     * 所有的参数
     */
    private List<Entry> args = new ArrayList<>();

    /**
     * 添加一些参数
     *
     * @param lines
     * @param sourceFile
     */
    public void addAll(List<String> lines, Path sourceFile) {
        String source = sourceFile.toAbsolutePath().toString();
        for (String line : lines) {
            addArg(line,source);
        }
    }

    /**
     * 添加一个参数
     *
     * @param rawline
     * @param source
     */
    public void addArg(final String rawline, final String source) {
        if (rawline == null) {
            return;
        }

        String line = rawline.trim();
        if (line.length() == 0) {
            return;
        }

        args.add(new Entry(line,source));
    }

    /**
     * 获取迭代器
     *
     * @return
     */
    @Override
    public Iterator<Entry> iterator() {
        return args.iterator();
    }

    /**
     * 获取容量
     *
     * @return
     */
    public int size() {
        return args.size();
    }
}
