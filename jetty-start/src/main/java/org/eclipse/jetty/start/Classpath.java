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
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class to handle CLASSPATH construction
 *
 * 类路径
 */
public class Classpath implements Iterable<File> {

    /**
     * 加载器
     *
     */
    private static class Loader extends URLClassLoader {
        /**
         * 构造方法
         *
         * @param urls
         * @param parent
         */
        Loader(URL[] urls, ClassLoader parent) {
            super(urls,parent);
        }

        /**
         * 转换为字符串
         *
         * @return
         */
        @Override
        public String toString() {
            return "startJarLoader@" + Long.toHexString(hashCode());
        }
    }

    /**
     * 文件
     */
    private final List<File> elements = new ArrayList<File>();

    /**
     * 构造方法
     */
    public Classpath() {
    }

    /**
     * 构造方法
     *
     * @param initial
     */
    public Classpath(String initial) {
        addClasspath(initial);
    }

    /**
     * 添加路径
     *
     * @param s
     * @return
     */
    public boolean addClasspath(String s) {
        boolean added = false;
        if (s != null) {
            StringTokenizer t = new StringTokenizer(s,File.pathSeparator);
            while (t.hasMoreTokens()) {
                added |= addComponent(t.nextToken());
            }
        }
        return added;
    }

    /**
     * 添加组件
     *
     * @param path
     * @return
     */
    public boolean addComponent(File path) {
        StartLog.debug("Adding classpath component: %s",path);
        if ((path == null) || (!path.exists())) {
            // not a valid component
            // 不是一个合法的组件
            return false;
        }

        try {
            File key = path.getCanonicalFile();
            if (!elements.contains(key)) {
                elements.add(key);
                return true;
            }
        } catch (IOException e) {
            StartLog.debug(e);
        }

        return false;
    }

    /**
     * 添加组件
     *
     * @param component
     * @return
     */
    public boolean addComponent(String component) {
        if ((component == null) || (component.length() <= 0)) {
            // nothing to add
            return false;
        }

        return addComponent(new File(component));
    }

    /**
     * 返回元素个数
     *
     * @return
     */
    public int count() {
        return elements.size();
    }

    /**
     * 打印信息
     *
     * @param out
     */
    public void dump(PrintStream out) {
        int i = 0;
        for (File element : elements) {
            out.printf("%2d: %s%n",i++,element.getAbsolutePath());
        }
    }

    /**
     * 获取类加载器
     *
     * @return
     */
    public ClassLoader getClassLoader() {
        int cnt = elements.size();
        URL[] urls = new URL[cnt];
        for (int i = 0; i < cnt; i++) {
            try {
                urls[i] = elements.get(i).toURI().toURL();
                StartLog.debug("URLClassLoader.url[%d] = %s",i,urls[i]);
            } catch (MalformedURLException e) {
                StartLog.warn(e);
            }
        }
        StartLog.debug("Loaded %d URLs into URLClassLoader",urls.length);

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = Classpath.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return new Loader(urls,parent);
    }

    /**
     * 获取所有元素
     *
     * @return
     */
    public List<File> getElements() {
        return elements;
    }

    /**
     * 是否为空
     *
     * @return
     */
    public boolean isEmpty() {
        return (elements == null) || (elements.isEmpty());
    }

    /**
     * 获取迭代器
     *
     * @return
     */
    @Override
    public Iterator<File> iterator() {
        return elements.iterator();
    }

    /**
     * Overlay another classpath, copying its elements into place on this Classpath,
     * while eliminating duplicate entries on the classpath.
     *
     * 采用不覆盖的方式来添加
     *
     * @param other
     *            the other classpath to overlay
     */
    public void overlay(Classpath other) {
        for (File otherElement : other.elements) {
            if (this.elements.contains(otherElement)) {
                // Skip duplicate entries
                // 避免重复内容
                continue;
            }
            this.elements.add(otherElement);
        }
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuffer cp = new StringBuffer(1024);
        boolean needDelim = false;
        for (File element : elements) {
            if (needDelim) {
                cp.append(File.pathSeparatorChar);
            }
            cp.append(element.getAbsolutePath());
            needDelim = true;
        }
        return cp.toString();
    }
}
