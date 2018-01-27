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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

/**
 * Simple common abstraction for Text files, that consist of a series of lines.
 * <p>
 * Ignoring lines that are empty, deemed to be comments, or are duplicates of prior lines.
 *
 * 文本文件
 * 对一系列行字符串的处理
 */
public class TextFile implements Iterable<String> {

    /**
     * 文件
     */
    private final Path file;

    /**
     * 字符串列表
     */
    private final List<String> lines = new ArrayList<>();


    /**
     * 构造方法
     *
     * @param file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public TextFile(Path file) throws FileNotFoundException, IOException {
        this.file = file;
        init();
        
        if (!FS.canReadFile(file)) {
            StartLog.debug("Skipping read of missing file: %s",file.toAbsolutePath());
            return;
        }

        try (BufferedReader buf = Files.newBufferedReader(file,StandardCharsets.UTF_8)) {
            String line;
            while ((line = buf.readLine()) != null) {
                // 空行
                if (line.length() == 0) {
                    continue;
                }

                // 注释
                if (line.charAt(0) == '#') {
                    continue;
                }

                // TODO - bad form calling derived method from base class constructor
                process(line.trim());
            }
        }
    }

    /**
     * 添加不同的行
     *
     * @param line
     */
    public void addUniqueLine(String line) {
        if (lines.contains(line)) {
            // skip
            return;
        }
        lines.add(line);
    }

    /**
     * 获取文件
     *
     * @return
     */
    public Path getFile() {
        return file;
    }

    /**
     * 从行中提取数据
     *
     * @param pattern
     * @return
     */
    public List<String> getLineMatches(Pattern pattern) {
        List<String> ret = new ArrayList<>();
        for (String line : lines) {
            if (pattern.matcher(line).matches()) {
                ret.add(line);
            }
        }
        return ret;
    }

    /**
     * 获取所有的行数据
     *
     * @return
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * 初始化
     */
    public void init() {
    }

    /**
     * 迭代
     *
     * @return
     */
    @Override
    public Iterator<String> iterator() {
        return lines.iterator();
    }

    /**
     * 获取列表迭代器
     *
     * @return
     */
    public ListIterator<String> listIterator() {
        return lines.listIterator();
    }

    /**
     * 执行
     * 其实就是添加
     *
     * @param line
     */
    public void process(String line) {
        addUniqueLine(line);
    }
}
