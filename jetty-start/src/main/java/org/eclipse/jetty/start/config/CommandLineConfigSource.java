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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.start.BaseHome;
import org.eclipse.jetty.start.FS;
import org.eclipse.jetty.start.Props;
import org.eclipse.jetty.start.Props.Prop;
import org.eclipse.jetty.start.RawArgs;
import org.eclipse.jetty.start.UsageException;

/**
 * Configuration Source representing the Command Line arguments.
 *
 * 命令行配置源
 * 这里有一个homePath和basePath的区分，这里概述一下:
 * 1.home_path通常是jetty的安装目录，它只有一个
 * 2.base_path通常是jetty的启动目录，它通常是每个实例有一个，即每次启动都有自己的路径
 * base_path下对应的内容的优先级要高
 */
public class CommandLineConfigSource implements ConfigSource {
    /**
     * 在降级时给属性的赋值
     */
    public static final String ORIGIN_INTERNAL_FALLBACK = "<internal-fallback>";

    /**
     * 原始的命令行占位符
     * 在没有数据可以填充的时候使用
     */
    public static final String ORIGIN_CMD_LINE = "<command-line>";

    /**
     * 原始的参数
     */
    private final RawArgs args;

    /**
     * 解析后的属性
     */
    private final Props props;

    /**
     * 家目录
     */
    private final Path homePath;

    /**
     * 基目录
     */
    private final Path basePath;

    /**
     * 构造方法
     * 其实在执行构造方法的时候已经完成了数据的填充
     *
     * @param rawargs
     */
    public CommandLineConfigSource(String rawargs[]) {
        this.args = new RawArgs();
        this.props = new Props();

        for (String arg : rawargs) {
            this.args.addArg(arg,ORIGIN_CMD_LINE);
            this.props.addPossibleProperty(arg,ORIGIN_CMD_LINE);
        }

        // Setup ${jetty.base} and ${jetty.home}
        this.homePath = findJettyHomePath().toAbsolutePath();
        this.basePath = findJettyBasePath().toAbsolutePath();

        // Update System Properties
        setSystemProperty(BaseHome.JETTY_HOME,homePath.toAbsolutePath().toString());
        setSystemProperty(BaseHome.JETTY_BASE,basePath.toAbsolutePath().toString());
    }

    /**
     * 获取basePath
     *
     * @return
     */
    private final Path findJettyBasePath() {
        // If a jetty property is defined, use it
        // 如果已经定义了，直接使用
        Prop prop = this.props.getProp(BaseHome.JETTY_BASE,false);
        if (prop != null && !isEmpty(prop.value)) {
            return FS.toPath(prop.value);
        }

        // If a system property is defined, use it
        // 使用系统定义的
        String val = System.getProperty(BaseHome.JETTY_BASE);
        if (!isEmpty(val)) {
            return FS.toPath(val);
        }

        // Lastly, fall back to base == ${user.dir}
        // 降级使用${user.dir}
        Path base = FS.toPath(this.props.getString("user.dir","."));
        setProperty(BaseHome.JETTY_BASE,base.toString(),ORIGIN_INTERNAL_FALLBACK);
        return base;
    }

    /**
     * 获取homePath
     *
     * @return
     */
    private final Path findJettyHomePath() {
        // If a jetty property is defined, use it
        // 如果已经定义了，直接使用
        Prop prop = this.props.getProp(BaseHome.JETTY_HOME,false);
        if (prop != null && !isEmpty(prop.value)) {
            return FS.toPath(prop.value);
        }

        // If a system property is defined, use it
        // 获取系统属性
        String val = System.getProperty(BaseHome.JETTY_HOME);
        if (!isEmpty(val)) {
            return FS.toPath(val);
        }

        // Attempt to find path relative to content in jetty's start.jar
        // based on lookup for the Main class (from jetty's start.jar)
        // 通过类加载器来找到这个路径
        String classRef = "org/eclipse/jetty/start/Main.class";
        URL jarfile = this.getClass().getClassLoader().getResource(classRef);
        if (jarfile != null) {
            Matcher m = Pattern.compile("jar:(file:.*)!/" + classRef).matcher(jarfile.toString());
            if (m.matches()) {
                // ${jetty.home} is relative to found BaseHome class
                try {
                    return new File(new URI(m.group(1))).getParentFile().toPath();
                } catch (URISyntaxException e) {
                    throw new UsageException(UsageException.ERR_UNKNOWN,e);
                }
            }
        }

        // Lastly, fall back to ${user.dir} default
        // 降级为使用${user.dir}
        Path home = FS.toPath(System.getProperty("user.dir","."));
        setProperty(BaseHome.JETTY_HOME,home.toString(),ORIGIN_INTERNAL_FALLBACK);
        return home;
    }

    /**
     * 判断字符串是否为空
     *
     * @param value
     * @return
     */
    private boolean isEmpty(String value) {
        if (value == null) {
            return true;
        }
        int len = value.length();
        for (int i = 0; i < len; i++) {
            int c = value.codePointAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否相等
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CommandLineConfigSource other = (CommandLineConfigSource)obj;
        if (args == null) {
            if (other.args != null) {
                return false;
            }
        } else if (!args.equals(other.args)) {
            return false;
        }
        return true;
    }

    /**
     * 获取原始参数
     *
     * @return
     */
    @Override
    public RawArgs getArgs() {
        return args;
    }

    /**
     * 获取基目录
     *
     * @return
     */
    public Path getBasePath() {
        return basePath;
    }

    /**
     * 获取家目录
     *
     * @return
     */
    public Path getHomePath() {
        return homePath;
    }

    /**
     * 获取id
     *
     * @return
     */
    @Override
    public String getId() {
        return ORIGIN_CMD_LINE;
    }

    /**
     * 获取单个属性
     *
     * @param key the key to lookup
     * @return
     */
    @Override
    public String getProperty(String key) {
        return props.getString(key);
    }

    /**
     * 获取所有属性
     *
     * @return
     */
    @Override
    public Props getProps() {
        return props;
    }

    /**
     * 命令行的默认权限
     *
     * @return
     */
    @Override
    public int getWeight() {
        return -1; // default value for command line
    }

    /**
     * 哈希值
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((args == null)?0:args.hashCode());
        return result;
    }

    /**
     * 设置属性
     *
     * @param key
     * @param value
     * @param origin
     */
    public void setProperty(String key, String value, String origin) {
        this.props.setProperty(key,value,origin);
    }

    /**
     * 设置系统属性
     *
     * @param key
     * @param value
     */
    public void setSystemProperty(String key, String value) {
        this.props.setSystemProperty(key,value);
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("%s[%s,args.length=%d]",this.getClass().getSimpleName(),getId(),getArgs().size());
    }
}
