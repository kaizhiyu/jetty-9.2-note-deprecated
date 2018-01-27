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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Module metadata, as defined in Jetty.
 *
 * 表示一个模块
 */
public class Module {

    /**
     * 深度比较器
     */
    public static class DepthComparator implements Comparator<Module> {
        private Collator collator = Collator.getInstance();

        @Override
        public int compare(Module o1, Module o2) {
            // order by depth first.
            int diff = o1.depth - o2.depth;
            if (diff != 0) {
                return diff;
            }
            // then by name (not really needed, but makes for predictable test cases)
            CollationKey k1 = collator.getCollationKey(o1.fileRef);
            CollationKey k2 = collator.getCollationKey(o2.fileRef);
            return k1.compareTo(k2);
        }
    }

    /**
     * 名称比较器
     */
    public static class NameComparator implements Comparator<Module> {
        private Collator collator = Collator.getInstance();

        @Override
        public int compare(Module o1, Module o2) {
            // by name (not really needed, but makes for predictable test cases)
            CollationKey k1 = collator.getCollationKey(o1.fileRef);
            CollationKey k2 = collator.getCollationKey(o2.fileRef);
            return k1.compareTo(k2);
        }
    }

    /**
     * The file of the module
     *
     * 指向模块的文件
     */
    private Path file;

    /**
     * The name of this Module (as a filesystem reference)
     *
     * 模块名
     */
    private String fileRef;

    /**
     * The logical name of this module (for property selected references), And to aid in duplicate detection.
     *
     * 逻辑名称
     */
    private String logicalName;


    /**
     * The depth of the module in the tree
     *
     * 模块的深度
     */
    private int depth = 0;

    /**
     * Set of Modules, by name, that this Module depends on
     *
     * 父模块的列表
     */
    private Set<String> parentNames;


    /**
     * Set of Modules, by name, that this Module optionally depend on
     *
     * 可选的父模块
     */
    private Set<String> optionalParentNames;

    /**
     * The Edges to parent modules
     *
     * 链接到父模块的边的集合
     */
    private Set<Module> parentEdges;

    /**
     * The Edges to child modules
     *
     * 连接到子模块的边
     */
    private Set<Module> childEdges;

    /**
     * List of xml configurations for this Module
     *
     * 当前模块的xml配置列表
     */
    private List<String> xmls;

    /**
     * List of ini template lines
     *
     * 默认配置列表
     */
    private List<String> defaultConfig;

    /**
     * 是否有默认配置，默认为false
     */
    private boolean hasDefaultConfig = false;

    /**
     * List of library options for this Module
     *
     * 当前模块的库列表
     */
    private List<String> libs;

    /**
     * List of files for this Module
     *
     * 当前模块的文件列表
     */
    private List<String> files;

    /**
     * List of jvm Args
     *
     * JVM参数列表
     */
    private List<String> jvmArgs;

    /**
     * License lines
     *
     * 许可证列表
     */
    private List<String> license;

    /**
     * Is this Module enabled via start.jar command line, start.ini, or start.d/*.ini ?
     *
     * 当前模块是否是内置的，有三条:
     * 1.在start.jar的命令行参数中
     * 2.在start.ini中
     * 3.在start.d目录下的xxx.ini中
     */
    private boolean enabled = false;

    /**
     * List of sources that enabled this module
     *
     * 启动该模块的配置源列表
     */
    private final Set<String> sources = new HashSet<>();

    /**
     * 是否同意协议
     */
    private boolean licenseAck = false;

    /**
     * 构造方法
     *
     * @param basehome
     * @param file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Module(BaseHome basehome, Path file) throws FileNotFoundException, IOException {
        this.file = file;

        // Strip .mod
        // 跳过.mod这个名称
        this.fileRef = Pattern.compile(".mod$",Pattern.CASE_INSENSITIVE).matcher(file.getFileName().toString()).replaceFirst("");
        this.logicalName = fileRef;

        init(basehome);
        process(basehome);
    }

    /**
     * 添加一个子模块的边
     *
     * @param child
     */
    public void addChildEdge(Module child) {
        if (childEdges.contains(child)) {
            // already present, skip
            return;
        }
        this.childEdges.add(child);
    }

    /**
     * 添加指向父模块的边
     *
     * @param parent
     */
    public void addParentEdge(Module parent) {
        if (parentEdges.contains(parent)) {
            // already present, skip
            return;
        }
        this.parentEdges.add(parent);
    }

    /**
     * 添加配置源
     *
     * @param sources
     */
    public void addSources(List<String> sources) {
        this.sources.addAll(sources);
    }

    /**
     * 清理配置源
     */
    public void clearSources() {
        this.sources.clear();
    }

    /**
     * 判断两个模块是否相等
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
        Module other = (Module)obj;
        if (fileRef == null) {
            if (other.fileRef != null) {
                return false;
            }
        } else if (!fileRef.equals(other.fileRef)) {
            return false;
        }
        return true;
    }

    /**
     * 扩展属性信息
     *
     * @param props
     */
    public void expandProperties(Props props) {
        // Expand Parents
        Set<String> parents = new HashSet<>();
        for (String parent : parentNames) {
            parents.add(props.expand(parent));
        }
        parentNames.clear();
        parentNames.addAll(parents);
    }

    public Set<Module> getChildEdges() {
        return childEdges;
    }

    /**
     * 获取深度
     *
     * @return
     */
    public int getDepth() {
        return depth;
    }

    /**
     * 获取文件列表
     *
     * @return
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * 获取文件引用
     *
     * @return
     */
    public String getFilesystemRef() {
        return fileRef;
    }

    /**
     * 获取默认配置
     *
     * @return
     */
    public List<String> getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 获取是否有默认配置
     *
     * @return
     */
    public boolean hasDefaultConfig() {
        return hasDefaultConfig;
    }

    /**
     * 获取库列表
     *
     * @return
     */
    public List<String> getLibs() {
        return libs;
    }

    /**
     * 获取逻辑名称
     *
     * @return
     */
    public String getName() {
        return logicalName;
    }

    /**
     * 可选的父模块名称列表
     *
     * @return
     */
    public Set<String> getOptionalParentNames() {
        return optionalParentNames;
    }

    /**
     * 获取所有的父模块的边
     * @return
     */
    public Set<Module> getParentEdges() {
        return parentEdges;
    }

    /**
     * 获取父模块名称
     *
     * @return
     */
    public Set<String> getParentNames() {
        return parentNames;
    }

    /**
     * 获取配置源列表
     *
     * @return
     */
    public Set<String> getSources() {
        return Collections.unmodifiableSet(sources);
    }

    /**
     * 获取包含的xml列表
     *
     * @return
     */
    public List<String> getXmls() {
        return xmls;
    }

    /**
     * 获取jvm参数
     *
     * @return
     */
    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    /**
     * 是否有协议
     *
     * @return
     */
    public boolean hasLicense() {
        return license != null && license.size() > 0;
    }

    /**
     * 是否同意了协议
     *
     * @return
     * @throws IOException
     */
    public boolean acknowledgeLicense() throws IOException {
        if (!hasLicense() || licenseAck) {
            return true;
        }

        System.err.printf("%nModule %s:%n",getName());
        System.err.printf(" + contains software not provided by the Eclipse Foundation!%n");
        System.err.printf(" + contains software not covered by the Eclipse Public License!%n");
        System.err.printf(" + has not been audited for compliance with its license%n");
        System.err.printf("%n");
        for (String l : getLicense()) {
            System.err.printf("    %s%n",l);
        }

        String propBasedAckName = "org.eclipse.jetty.start.ack.license." + getName();
        String propBasedAckValue = System.getProperty(propBasedAckName);
        if (propBasedAckValue != null) {
            StartLog.log("TESTING MODE", "Programmatic ACK - %s=%s",propBasedAckName,propBasedAckValue);
            licenseAck = Boolean.parseBoolean(propBasedAckValue);
        } else {
            if (Boolean.getBoolean("org.eclipse.jetty.start.testing")) {
                throw new RuntimeException("Test Configuration Missing - Pre-specify answer to (" + propBasedAckName + ") in test case");
            }

            try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
                System.err.printf("%nProceed (y/N)? ");
                String line = input.readLine();

                licenseAck = !(line == null || line.length() == 0 || !line.toLowerCase(Locale.ENGLISH).startsWith("y"));
            }
        }

        return licenseAck;
    }

    /**
     * 获取协议列表
     *
     * @return
     */
    public List<String> getLicense() {
        return license;
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
        result = (prime * result) + ((fileRef == null)?0:fileRef.hashCode());
        return result;
    }

    /**
     * 初始化
     * @param basehome
     */
    private void init(BaseHome basehome) {
        parentNames = new HashSet<>();
        optionalParentNames = new HashSet<>();
        parentEdges = new HashSet<>();
        childEdges = new HashSet<>();
        xmls = new ArrayList<>();
        defaultConfig = new ArrayList<>();
        libs = new ArrayList<>();
        files = new ArrayList<>();
        jvmArgs = new ArrayList<>();
        license = new ArrayList<>();

        String name = basehome.toShortForm(file);

        // Find module system name (usually in the form of a filesystem reference)
        Pattern pat = Pattern.compile("^.*[/\\\\]{1}modules[/\\\\]{1}(.*).mod$",Pattern.CASE_INSENSITIVE);
        Matcher mat = pat.matcher(name);
        if (!mat.find()) {
            throw new RuntimeException("Invalid Module location (must be located under /modules/ directory): " + name);
        }
        this.fileRef = mat.group(1).replace('\\','/');
        this.logicalName = this.fileRef;
    }

    /**
     * 是否启用该模块
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断是否有文件
     *
     * @param baseHome
     * @return
     */
    public boolean hasFiles(BaseHome baseHome) {
        for (String ref : getFiles()) {
            FileArg farg = new FileArg(this,ref);
            Path refPath = baseHome.getBasePath(farg.location);
            if (!Files.exists(refPath)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行模块
     *
     * @param basehome
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void process(BaseHome basehome) throws FileNotFoundException, IOException {
        Pattern section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");

        if (!FS.canReadFile(file)) {
            StartLog.debug("Skipping read of missing file: %s",basehome.toShortForm(file));
            return;
        }

        try (BufferedReader buf = Files.newBufferedReader(file,StandardCharsets.UTF_8)) {
            String sectionType = "";
            String line;
            while ((line = buf.readLine()) != null) {
                line = line.trim();

                Matcher sectionMatcher = section.matcher(line);

                if (sectionMatcher.matches()) {
                    sectionType = sectionMatcher.group(1).trim().toUpperCase(Locale.ENGLISH);
                } else {
                    // blank lines and comments are valid for ini-template section
                    // 空行和被注释的行也是合法的
                    if ((line.length() == 0) || line.startsWith("#")) {
                        if ("INI-TEMPLATE".equals(sectionType)) {
                            defaultConfig.add(line);
                        }
                    } else {
                        switch (sectionType) {
                            case "":
                                // ignore (this would be entries before first section)
                                // 忽略
                                break;
                            case "DEPEND":
                                parentNames.add(line);
                                break;
                            case "FILES":
                                files.add(line);
                                break;
                            case "DEFAULTS":
                            case "INI-TEMPLATE":
                                defaultConfig.add(line);
                                hasDefaultConfig = true;
                                break;
                            case "LIB":
                                libs.add(line);
                                break;
                            case "LICENSE":
                            case "LICENCE":
                                license.add(line);
                                break;
                            case "NAME":
                                logicalName = line;
                                break;
                            case "OPTIONAL":
                                optionalParentNames.add(line);
                                break;
                            case "EXEC":
                                jvmArgs.add(line);
                                break;
                            case "XML":
                                xmls.add(line);
                                break;
                            default:
                                throw new IOException("Unrecognized Module section: [" + sectionType + "]");
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置深度
     *
     * @param depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * 是否启动
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 设置父模块
     *
     * @param parents
     */
    public void setParentNames(Set<String> parents) {
        this.parentNames.clear();
        this.parentEdges.clear();
        if (parents != null) {
            this.parentNames.addAll(parents);
        }
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Module[").append(logicalName);
        if (!logicalName.equals(fileRef)) {
            str.append(",file=").append(fileRef);
        }
        if (enabled) {
            str.append(",enabled");
        }
        str.append(']');
        return str.toString();
    }
}
