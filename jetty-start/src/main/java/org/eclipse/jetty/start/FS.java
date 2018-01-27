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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Locale;

/**
 * 文件系统
 * 它是对文件的操作的一个功能的聚合
 */
public class FS {
    /**
     * 是否是有读权限的目录
     *
     * @param path
     * @return
     */
    public static boolean canReadDirectory(Path path) {
        return Files.exists(path) && Files.isDirectory(path) && Files.isReadable(path);
    }

    /**
     * 是否是有读权限的文件
     *
     * @param path
     * @return
     */
    public static boolean canReadFile(Path path) {
        return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }

    /**
     * 是否可写
     *
     * @param path
     * @return
     */
    public static boolean canWrite(Path path) {
        return Files.isWritable(path);
    }

    /**
     * 关闭
     *
     * @param c
     */
    public static void close(Closeable c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (IOException ignore) {
            /* ignore */
        }
    }

    /**
     * 创建新文件
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static boolean createNewFile(Path path) throws IOException {
        Path ret = Files.createFile(path);
        return Files.exists(ret);
    }

    /**
     * 确保目录存在
     *
     * @param dir
     * @throws IOException
     */
    public static void ensureDirectoryExists(Path dir) throws IOException {
        if (exists(dir)) {
            // exists already, nothing to do
            return;
        }
        Files.createDirectories(dir);
    }

    /**
     * 确保目录可写
     *
     * @param dir
     * @throws IOException
     */
    public static void ensureDirectoryWritable(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            throw new IOException("Path does not exist: " + dir.toAbsolutePath());
        }
        if (!Files.isDirectory(dir)) {
            throw new IOException("Directory does not exist: " + dir.toAbsolutePath());
        }
        if (!Files.isWritable(dir)) {
            throw new IOException("Unable to write to directory: " + dir.toAbsolutePath());
        }
    }

    /**
     * 文件是否存在
     *
     * @param path
     * @return
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * 是否是合法的目录
     *
     * @param path
     * @return
     */
    public static boolean isValidDirectory(Path path) {
        if (!Files.exists(path)) {
            // doesn't exist, not a valid directory
            // 不存在肯定是不合法的
            return false;
        }

        if (!Files.isDirectory(path)) {
            // not a directory (as expected)
            // 不是目录，也肯定是不合法的
            StartLog.warn("Not a directory: " + path);
            return false;
        }

        return true;
    }

    /**
     * 判断是否是xml文件
     *
     * @param filename
     * @return
     */
    public static boolean isXml(String filename) {
        return filename.toLowerCase(Locale.ENGLISH).endsWith(".xml");
    }

    /**
     * 转换为相对路径
     *
     * @param baseDir
     * @param path
     * @return
     */
    public static String toRelativePath(File baseDir, File path) {
        return baseDir.toURI().relativize(path.toURI()).toASCIIString();
    }

    /**
     * 是否是properties文件
     *
     * @param filename
     * @return
     */
    public static boolean isPropertyFile(String filename) {
        return filename.toLowerCase(Locale.ENGLISH).endsWith(".properties");
    }

    /**
     * 替换目录分隔符
     * 它是用遍历的方式来逐个替换
     *
     * @param path
     * @return
     */
    public static String separators(String path) {
        StringBuilder ret = new StringBuilder();
        for (char c : path.toCharArray()) {
            if ((c == '/') || (c == '\\')) {
                ret.append(File.separatorChar);
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    /**
     * 转换为Path
     *
     * @param path
     * @return
     */
    public static Path toPath(String path) {
        return FileSystems.getDefault().getPath(FS.separators(path));
    }

    /**
     * 创建
     *
     * @param path
     * @throws IOException
     */
    public static void touch(Path path) throws IOException {
        FileTime now = FileTime.fromMillis(System.currentTimeMillis());
        Files.setLastModifiedTime(path,now);
    }

    /**
     * 真正的绝对路径
     * 它是对绝对路径的一种强化
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Path toRealPath(Path path) throws IOException {
        return path.toRealPath();
    }
}
