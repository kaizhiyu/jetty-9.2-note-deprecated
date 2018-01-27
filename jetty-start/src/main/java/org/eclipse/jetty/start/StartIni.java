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
import java.nio.file.Path;

/**
 * Simple Start .INI handler
 *
 * 简单的start.ini处理器
 */
public class StartIni extends TextFile {
    /**
     * 基目录
     */
    private Path basedir;

    /**
     * 构造方法
     *
     * @param file
     * @throws IOException
     */
    public StartIni(Path file) throws IOException {
        super(file);
    }

    /**
     * 添加不同的一行
     *
     * @param line
     */
    @Override
    public void addUniqueLine(String line) {
        if (line.startsWith("--module=")) {
            // 添加目录
            int idx = line.indexOf('=');
            String value = line.substring(idx + 1);
            for (String part : value.split(",")) {
                super.addUniqueLine("--module=" + expandBaseDir(part));
            }
        } else {
            super.addUniqueLine(expandBaseDir(line));
        }
    }

    /**
     * 扩展目录
     * 之所以要扩展目录，是因为很多配置中通常只写一个模块名
     *
     * @param line
     * @return
     */
    private String expandBaseDir(String line) {
        if (line == null) {
            return line;
        }

        return line.replace("${start.basedir}",basedir.toString());
    }

    /**
     * 初始化
     */
    @Override
    public void init() {
        basedir = getFile().getParent().toAbsolutePath();
    }

    /**
     * 获取基目录
     *
     * @return
     */
    public Path getBaseDir() {
        return basedir;
    }
}
