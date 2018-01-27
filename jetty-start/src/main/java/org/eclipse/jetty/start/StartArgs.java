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

import static org.eclipse.jetty.start.UsageException.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jetty.start.Props.Prop;
import org.eclipse.jetty.start.config.ConfigSource;
import org.eclipse.jetty.start.config.ConfigSources;
import org.eclipse.jetty.start.config.DirConfigSource;

/**
 * The Arguments required to start Jetty.
 *
 * 启动参数
 */
public class StartArgs {
    /**
     * 版本
     */
    public static final String VERSION;

    /**
     * 静态代码块唯一要做的就是版本
     */
    static {
        String ver = System.getProperty("jetty.version",null);

        if (ver == null) {
            Package pkg = StartArgs.class.getPackage();
            if ((pkg != null) && "Eclipse.org - Jetty".equals(pkg.getImplementationVendor()) && (pkg.getImplementationVersion() != null)) {
                ver = pkg.getImplementationVersion();
            }
        }

        if (ver == null) {
            ver = "TEST";
        }

        VERSION = ver;
        System.setProperty("jetty.version",VERSION);
    }

    /**
     * xml配置类
     */
    private static final String SERVER_MAIN = "org.eclipse.jetty.xml.XmlConfiguration";

    /**
     * List of enabled modules
     *
     * 开启的模块
     */
    private Set<String> modules = new HashSet<>();

    /**
     * Map of enabled modules to the source of where that activation occurred
     *
     * 开启模块及其来源
     */
    private Map<String, List<String>> sources = new HashMap<>();

    /**
     * Map of properties to where that property was declared
     *
     * 属性配置及其来源
     */
    private Map<String, String> propertySource = new HashMap<>();

    /**
     * List of all active [files] sections from enabled modules
     *
     * 所有激活的文件部分
     */
    private List<FileArg> files = new ArrayList<>();

    /**
     * List of all active [lib] sections from enabled modules
     *
     * 所有的lib部分
     */
    private Classpath classpath;

    /**
     * List of all active [xml] sections from enabled modules
     *
     * 所有的xml部分
     */
    private List<Path> xmls = new ArrayList<>();

    /**
     * JVM arguments, found via commmand line and in all active [exec] sections from enabled modules
     *
     * jvm参数
     */
    private List<String> jvmArgs = new ArrayList<>();

    /**
     * List of all xml references found directly on command line or start.ini
     *
     * 所有的xml引用
     */
    private List<String> xmlRefs = new ArrayList<>();

    /**
     * List of all property references found directly on command line or start.ini
     *
     * 属性引用
     */
    private List<String> propertyFileRefs = new ArrayList<>();
    
    /**
     * List of all property files
     *
     * 所有的属性文件
     */
    private List<Path> propertyFiles = new ArrayList<>();

    /**
     * 属性
     */
    private Props properties = new Props();

    /**
     * 系统属性
     */
    private Set<String> systemPropertyKeys = new HashSet<>();

    /**
     * 原始库
     */
    private List<String> rawLibs = new ArrayList<>();

    // jetty.base - build out commands
    /**
     * --add-to-startd=[module,[module]]
     *
     * 添加到配置目录中的启动模块
     */
    private List<String> addToStartdIni = new ArrayList<>();

    /**
     * --add-to-start=[module,[module]]
     *
     * 添加到start.ini中的启动模块
     */
    private List<String> addToStartIni = new ArrayList<>();

    // module inspection commands
    /**
     * --write-module-graph=[filename]
     */
    private String moduleGraphFilename;

    /**
     * Collection of all modules
     *
     * 所有模块的集合
     */
    private Modules allModules;


    /**
     * Should the server be run?
     *
     * 是否应该启动服务器
     */
    private boolean run = true;

    /**
     * 是否是下载
     */
    private boolean download = false;

    /**
     * 是否显示帮助信息
     */
    private boolean help = false;

    /**
     * 是否是结束命令
     */
    private boolean stopCommand = false;

    /**
     * 是否显示所有模块
     */
    private boolean listModules = false;

    /**
     * 是否列举类路径
     */
    private boolean listClasspath = false;

    /**
     * 是否列举配置
     */
    private boolean listConfig = false;

    /**
     * 是否显示版本信息
     */
    private boolean version = false;

    /**
     * 是否测试配置
     */
    private boolean dryRun = false;

    /**
     * 是否exec
     */
    private boolean exec = false;

    /**
     * 是否同意所有声明
     */
    private boolean approveAllLicenses = false;

    /**
     * 测试模式
     */
    private boolean testingMode = false;

    /**
     * 构造方法
     */
    public StartArgs() {
        classpath = new Classpath();
    }

    /**
     * 添加文件
     *
     * @param module
     * @param uriLocation
     */
    private void addFile(Module module, String uriLocation) {
        FileArg arg = new FileArg(module, uriLocation);
        if (!files.contains(arg)) {
            files.add(arg);
        }
    }

    /**
     * 添加系统配置项
     *
     * @param key
     * @param value
     */
    public void addSystemProperty(String key, String value) {
        this.systemPropertyKeys.add(key);
        System.setProperty(key,value);
    }

    /**
     * 添加不同的xml文件
     *
     * @param xmlRef
     * @param xmlfile
     * @throws IOException
     */
    private void addUniqueXmlFile(String xmlRef, Path xmlfile) throws IOException {
        if (!FS.canReadFile(xmlfile)) {
            throw new IOException("Cannot read file: " + xmlRef);
        }
        xmlfile = FS.toRealPath(xmlfile);
        if (!xmls.contains(xmlfile)) {
            xmls.add(xmlfile);
        }
    }

    /**
     * 添加不同的property文件
     *
     * @param propertyFileRef
     * @param propertyFile
     * @throws IOException
     */
    private void addUniquePropertyFile(String propertyFileRef, Path propertyFile) throws IOException {
        if (!FS.canReadFile(propertyFile)) {
            throw new IOException("Cannot read file: " + propertyFileRef);
        }
        propertyFile = FS.toRealPath(propertyFile);
        if (!propertyFiles.contains(propertyFile)) {
            propertyFiles.add(propertyFile);
        }
    }

    /**
     * 打印所有激活的xml文件
     *
     * @param baseHome
     */
    public void dumpActiveXmls(BaseHome baseHome) {
        System.out.println();
        System.out.println("Jetty Active XMLs:");
        System.out.println("------------------");
        if (xmls.isEmpty()) {
            System.out.println(" (no xml files specified)");
            return;
        }

        for (Path xml : xmls) {
            System.out.printf(" %s%n",baseHome.toShortForm(xml.toAbsolutePath()));
        }
    }

    /**
     * 查看环境信息
     * 主要包含:
     * 1.Java环境参数
     * 2.Jetty环境参数
     *
     * @param baseHome
     */
    public void dumpEnvironment(BaseHome baseHome) {
        // Java Details
        System.out.println();
        System.out.println("Java Environment:");
        System.out.println("-----------------");
        dumpSystemProperty("java.home");
        dumpSystemProperty("java.vm.vendor");
        dumpSystemProperty("java.vm.version");
        dumpSystemProperty("java.vm.name");
        dumpSystemProperty("java.vm.info");
        dumpSystemProperty("java.runtime.name");
        dumpSystemProperty("java.runtime.version");
        dumpSystemProperty("java.io.tmpdir");
        dumpSystemProperty("user.dir");
        dumpSystemProperty("user.language");
        dumpSystemProperty("user.country");

        // Jetty Environment
        System.out.println();
        System.out.println("Jetty Environment:");
        System.out.println("-----------------");
        dumpProperty("jetty.version");
        dumpProperty("jetty.home");
        dumpProperty("jetty.base");
        
        // Jetty Configuration Environment
        System.out.println();
        System.out.println("Config Search Order:");
        System.out.println("--------------------");
        for (ConfigSource config : baseHome.getConfigSources()) {
            System.out.printf(" %s",config.getId());
            if (config instanceof DirConfigSource) {
                DirConfigSource dirsource = (DirConfigSource)config;
                if (dirsource.isPropertyBased()) {
                    System.out.printf(" -> %s",dirsource.getDir());
                }
            }
            System.out.println();
        }
        
        // Jetty Se
        System.out.println();
    }

    /**
     * 打印JVM参数
     */
    public void dumpJvmArgs() {
        System.out.println();
        System.out.println("JVM Arguments:");
        System.out.println("--------------");
        if (jvmArgs.isEmpty()) {
            System.out.println(" (no jvm args specified)");
            return;
        }

        for (String jvmArgKey : jvmArgs) {
            String value = System.getProperty(jvmArgKey);
            if (value != null) {
                System.out.printf(" %s = %s%n",jvmArgKey,value);
            } else {
                System.out.printf(" %s%n",jvmArgKey);
            }
        }
    }

    /**
     * 打印属性
     */
    public void dumpProperties() {
        System.out.println();
        System.out.println("Properties:");
        System.out.println("-----------");

        List<String> sortedKeys = new ArrayList<>();
        for (Prop prop : properties) {
            if (prop.origin.equals(Props.ORIGIN_SYSPROP)) {
                continue; // skip
            }
            sortedKeys.add(prop.key);
        }

        if (sortedKeys.isEmpty()) {
            System.out.println(" (no properties specified)");
            return;
        }

        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            dumpProperty(key);
        }
    }

    /**
     * 打印属性信息
     *
     * @param key
     */
    private void dumpProperty(String key) {
        Prop prop = properties.getProp(key);
        if (prop == null) {
            System.out.printf(" %s (not defined)%n",key);
        } else {
            System.out.printf(" %s = %s%n",key,properties.expand(prop.value));
            if (StartLog.isDebugEnabled()) {
                System.out.printf("   origin: %s%n",prop.origin);
                while (prop.overrides != null) {
                    prop = prop.overrides;
                    System.out.printf("   (overrides)%n");
                    System.out.printf("     %s = %s%n",key,properties.expand(prop.value));
                    System.out.printf("     origin: %s%n",prop.origin);
                }
            }
        }
    }

    /**
     * 打印系统属性
     */
    public void dumpSystemProperties() {
        System.out.println();
        System.out.println("System Properties:");
        System.out.println("------------------");

        if (systemPropertyKeys.isEmpty()) {
            System.out.println(" (no system properties specified)");
            return;
        }

        List<String> sortedKeys = new ArrayList<>();
        sortedKeys.addAll(systemPropertyKeys);
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            String value = System.getProperty(key);
            System.out.printf(" %s = %s%n",key,properties.expand(value));
        }
    }

    /**
     * 打印单个系统属性
     *
     * @param key
     */
    private void dumpSystemProperty(String key) {
        System.out.printf(" %s = %s%n",key,System.getProperty(key));
    }

    /**
     * Ensure that the System Properties are set (if defined as a System property, or start.config property, or start.ini property)
     *
     * 确保某个属性被设置
     *
     * @param key
     *            the key to be sure of
     */
    private void ensureSystemPropertySet(String key) {
        if (systemPropertyKeys.contains(key)) {
            return; // done
        }

        if (properties.containsKey(key)) {
            String val = properties.expand(properties.getString(key));
            if (val == null) {
                return; // no value to set
            }
            // setup system property
            systemPropertyKeys.add(key);
            System.setProperty(key,val);
        }
    }

    /**
     * Expand any command line added <code>--lib</code> lib references.
     *
     * 扩展库
     *
     * @param baseHome
     * @throws IOException
     */
    public void expandLibs(BaseHome baseHome) throws IOException {
        StartLog.debug("Expanding Libs");
        for (String rawlibref : rawLibs) {
            StartLog.debug("rawlibref = " + rawlibref);
            String libref = properties.expand(rawlibref);
            StartLog.debug("expanded = " + libref);
            
            // perform path escaping (needed by windows)
            libref = libref.replaceAll("\\\\([^\\\\])","\\\\\\\\$1");
            
            for (Path libpath : baseHome.getPaths(libref)) {
                classpath.addComponent(libpath.toFile());
            }
        }
    }

    /**
     * Build up the Classpath and XML file references based on enabled Module list.
     *
     * 扩展模块
     *
     * @param baseHome
     * @param activeModules
     * @throws IOException
     */
    public void expandModules(BaseHome baseHome, List<Module> activeModules) throws IOException {
        StartLog.debug("Expanding Modules");
        for (Module module : activeModules) {
            // Find and Expand Libraries
            for (String rawlibref : module.getLibs()) {
                StartLog.debug("rawlibref = " + rawlibref);
                String libref = properties.expand(rawlibref);
                StartLog.debug("expanded = " + libref);

                for (Path libpath : baseHome.getPaths(libref)) {
                    classpath.addComponent(libpath.toFile());
                }
            }

            for (String jvmArg : module.getJvmArgs()) {
                exec = true;
                jvmArgs.add(jvmArg);
            }

            // Find and Expand XML files
            for (String xmlRef : module.getXmls()) {
                // Straight Reference
                Path xmlfile = baseHome.getPath(xmlRef);
                addUniqueXmlFile(xmlRef,xmlfile);
            }

            // Register Download operations
            for (String file : module.getFiles()) {
                StartLog.debug("Adding module specified file: %s",file);
                addFile(module,file);
            }
        }
    }

    /**
     * 获取添加到start.d下面的配置项
     *
     * @return
     */
    public List<String> getAddToStartdIni() {
        return addToStartdIni;
    }

    /**
     * 获取添加到start.ini中的配置项
     *
     * @return
     */
    public List<String> getAddToStartIni() {
        return addToStartIni;
    }

    /**
     * 获取所有的模块
     *
     * @return
     */
    public Modules getAllModules() {
        return allModules;
    }

    /**
     * 获取类路径
     *
     * @return
     */
    public Classpath getClasspath() {
        return classpath;
    }

    /**
     * 获取启用的模块
     *
     * @return
     */
    public Set<String> getEnabledModules() {
        return this.modules;
    }

    /**
     * 获取所有文件
     *
     * @return
     */
    public List<FileArg> getFiles() {
        return files;
    }

    /**
     * 获取jvm启动参数
     *
     * @return
     */
    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    /**
     * 获取主要参数
     *
     * @param baseHome
     * @param addJavaInit
     * @return
     * @throws IOException
     */
    public CommandLineBuilder getMainArgs(BaseHome baseHome, boolean addJavaInit) throws IOException {
        CommandLineBuilder cmd = new CommandLineBuilder();

        if (addJavaInit) {
            cmd.addRawArg(CommandLineBuilder.findJavaBin());

            for (String x : jvmArgs) {
                cmd.addRawArg(x);
            }

            cmd.addRawArg("-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"));
            cmd.addRawArg("-Djetty.home=" + baseHome.getHome());
            cmd.addRawArg("-Djetty.base=" + baseHome.getBase());

            // System Properties
            for (String propKey : systemPropertyKeys) {
                String value = System.getProperty(propKey);
                cmd.addEqualsArg("-D" + propKey,value);
            }

            cmd.addRawArg("-cp");
            cmd.addRawArg(classpath.toString());
            cmd.addRawArg(getMainClassname());
        }

        // Special Stop/Shutdown properties
        // 特定的关闭和结束属性
        ensureSystemPropertySet("STOP.PORT");
        ensureSystemPropertySet("STOP.KEY");
        ensureSystemPropertySet("STOP.WAIT");

        // pass properties as args or as a file
        if (dryRun || isExec()) {
            for (Prop p : properties) {
                cmd.addRawArg(CommandLineBuilder.quote(p.key)+"="+CommandLineBuilder.quote(p.value));
            }
        } else if (properties.size() > 0) {
            File prop_file = File.createTempFile("start",".properties");
            prop_file.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(prop_file)) {
                properties.store(out,"start.jar properties");
            }
            cmd.addRawArg(prop_file.getAbsolutePath());
        }

        for (Path xml : xmls) {
            cmd.addRawArg(xml.toAbsolutePath().toString());
        }
        
        for (Path propertyFile : propertyFiles) {
            cmd.addRawArg(propertyFile.toAbsolutePath().toString());
        }

        return cmd;
    }

    /**
     * 获取启动类的类名
     *
     * @return
     */
    public String getMainClassname() {
        String mainclass = System.getProperty("jetty.server",SERVER_MAIN);
        return System.getProperty("main.class",mainclass);
    }

    /**
     *
     *
     * @return
     */
    public String getModuleGraphFilename() {
        return moduleGraphFilename;
    }

    /**
     * 获取所有属性
     *
     * @return
     */
    public Props getProperties() {
        return properties;
    }

    /**
     * 获取源
     *
     * @param module
     * @return
     */
    public List<String> getSources(String module) {
        return sources.get(module);
    }

    /**
     * 获取xml文件列表
     *
     * @return
     */
    public List<Path> getXmlFiles() {
        return xmls;
    }

    /**
     * 是否有JVM参数
     *
     * @return
     */
    public boolean hasJvmArgs() {
        return jvmArgs.size() > 0;
    }

    /**
     * 是否有系统属性
     *
     * @return
     */
    public boolean hasSystemProperties() {
        for (String key : systemPropertyKeys) {
            // ignored keys
            if ("jetty.home".equals(key) || "jetty.base".equals(key) || "main.class".equals(key)) {
                // skip
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 是否同意了所有协议
     *
     * @return
     */
    public boolean isApproveAllLicenses() {
        return approveAllLicenses;
    }

    /**
     * 是否是下载任务
     *
     * @return
     */
    public boolean isDownload() {
        return download;
    }

    /**
     * 是否检测配置项
     *
     * @return
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * 获取exec
     *
     * @return
     */
    public boolean isExec() {
        return exec;
    }

    /**
     * 是不是正常的main类
     *
     * @return
     */
    public boolean isNormalMainClass() {
        return SERVER_MAIN.equals(getMainClassname());
    }

    /**
     * 是否列举帮助信息
     *
     * @return
     */
    public boolean isHelp() {
        return help;
    }

    /**
     * 是否列举类路径
     *
     * @return
     */
    public boolean isListClasspath() {
        return listClasspath;
    }

    /**
     * 是否列举配置
     *
     * @return
     */
    public boolean isListConfig() {
        return listConfig;
    }

    /**
     * 是否列举版本
     *
     * @return
     */
    public boolean isListModules() {
        return listModules;
    }

    /**
     * 是否运行
     *
     * @return
     */
    public boolean isRun() {
        return run;
    }

    /**
     * 是不是关闭命令
     *
     * @return
     */
    public boolean isStopCommand() {
        return stopCommand;
    }

    /**
     * 是否开启测试模式
     *
     * @return
     */
    public boolean isTestingModeEnabled() {
        return testingMode;
    }

    /**
     * 是否只列举版本信息
     *
     * @return
     */
    public boolean isVersion() {
        return version;
    }

    /**
     * 解析配置源
     *
     * @param sources
     */
    public void parse(ConfigSources sources) {
        ListIterator<ConfigSource> iter = sources.reverseListIterator();
        while (iter.hasPrevious()) {
            ConfigSource source = iter.previous();
            for (RawArgs.Entry arg : source.getArgs()) {
                parse(arg.getLine(),arg.getOrigin());
            }
        }
    }

    /**
     * 解析启动参数
     *
     * @param rawarg
     * @param source
     */
    public void parse(final String rawarg, String source) {
        parse(rawarg,source,true);
    }

    /**
     * Parse a single line of argument.
     *
     * 解析一行参数
     * 
     * @param rawarg the raw argument to parse
     * @param source the origin of this line of argument
     * @param replaceProps true if properties in this parse replace previous ones, false to not replace.
     */
    private void parse(final String rawarg, String source, boolean replaceProps) {
        if (rawarg == null) {
            return;
        }
        
        StartLog.debug("parse(\"%s\", \"%s\", %b)",rawarg,source,replaceProps);

        final String arg = rawarg.trim();

        // 如果没有内容，跳过
        if (arg.length() <= 0) {
            return;
        }

        // #号开头为注释，跳过
        if (arg.startsWith("#")) {
            return;
        }

        // 打印帮助信息
        if ("--help".equals(arg) || "-?".equals(arg)) {
            help = true;
            run = false;
            return;
        }

        // 合法，但是不在这里处理
        if ("--debug".equals(arg) || arg.startsWith("--start-log-file")) {
            // valid, but handled in StartLog instead
            return;
        }

        /**
         * 测试模式
         */
        if ("--testing-mode".equals(arg)) {
            System.setProperty("org.eclipse.jetty.start.testing","true");
            testingMode = true;
            return;
        }

        // 在配置源中处理
        if (arg.startsWith("--include-jetty-dir=")) {
            // valid, but handled in ConfigSources instead
            return;
        }

        // 关闭
        if ("--stop".equals(arg)) {
            stopCommand = true;
            run = false;
            return;
        }

        // 下载
        if (arg.startsWith("--download=")) {
            addFile(null,Props.getValue(arg));
            run = false;
            download = true;
            return;
        }

        // 不运行，创建文件
        if (arg.equals("--create-files")) {
            run = false;
            download = true;
            return;
        }

        // 列举类路径，获取版本，其实都是一样的
        if ("--list-classpath".equals(arg) || "--version".equals(arg) || "-v".equals(arg) || "--info".equals(arg)) {
            listClasspath = true;
            run = false;
            return;
        }

        // 获取配置
        if ("--list-config".equals(arg)) {
            listConfig = true;
            run = false;
            return;
        }

        // 测试配置项
        if ("--dry-run".equals(arg) || "--exec-print".equals(arg)) {
            dryRun = true;
            run = false;
            return;
        }

        // Enable forked execution of Jetty server
        //
        if ("--exec".equals(arg)) {
            exec = true;
            return;
        }

        // Enable forked execution of Jetty server
        // 允许所有的协议
        if ("--approve-all-licenses".equals(arg)) {
            approveAllLicenses = true;
            return;
        }

        // Arbitrary Libraries
        // 类库
        if (arg.startsWith("--lib=")) {
            String cp = Props.getValue(arg);

            if (cp != null) {
                StringTokenizer t = new StringTokenizer(cp,File.pathSeparator);
                while (t.hasMoreTokens()) {
                    rawLibs.add(t.nextToken());
                }
            }
            return;
        }

        // Module Management
        // 列举所有的模块
        if ("--list-modules".equals(arg)) {
            listModules = true;
            run = false;
            return;
        }

        // jetty.base build-out : add to ${jetty.base}/start.d/
        // 添加到配置项目录中
        if (arg.startsWith("--add-to-startd=")) {
            List<String> moduleNames = Props.getValues(arg);
            addToStartdIni.addAll(moduleNames);
            run = false;
            download = true;
            return;
        }

        // jetty.base build-out : add to ${jetty.base}/start.ini
        // 添加到配置项中
        if (arg.startsWith("--add-to-start=")) {
            List<String> moduleNames = Props.getValues(arg);
            addToStartIni.addAll(moduleNames);
            run = false;
            download = true;
            return;
        }

        // Enable a module
        // 启动模块
        if (arg.startsWith("--module=")) {
            List<String> moduleNames = Props.getValues(arg);
            enableModules(source,moduleNames);
            return;
        }

        // Create graphviz output of module graph
        //
        if (arg.startsWith("--write-module-graph=")) {
            this.moduleGraphFilename = Props.getValue(arg);
            run = false;
            return;
        }

        // Start property (syntax similar to System property)
        // 启动属性
        if (arg.startsWith("-D")) {
            String[] assign = arg.substring(2).split("=",2);
            systemPropertyKeys.add(assign[0]);
            switch (assign.length) {
                case 2:
                    System.setProperty(assign[0],assign[1]);
                    setProperty(assign[0],assign[1],source,replaceProps);
                    break;
                case 1:
                    System.setProperty(assign[0],"");
                    setProperty(assign[0],"",source,replaceProps);
                    break;
                default:
                    break;
            }
            return;
        }

        // Anything else with a "-" is considered a JVM argument
        // JVM启动参数
        if (arg.startsWith("-")) {
            // Only add non-duplicates
            // 不会重复添加
            if (!jvmArgs.contains(arg)) {
                jvmArgs.add(arg);
            }
            return;
        }

        // Is this a raw property declaration?
        // 原始的属性声明
        int idx = arg.indexOf('=');
        if (idx >= 0) {
            String key = arg.substring(0,idx);
            String value = arg.substring(idx + 1);

            // 是否允许覆盖
            if (replaceProps) {
                if (propertySource.containsKey(key)) {
                    StartLog.warn("Property %s in %s already set in %s",key,source,propertySource.get(key));
                }
                propertySource.put(key,source);
            }

            if ("OPTION".equals(key) || "OPTIONS".equals(key)) {
                StringBuilder warn = new StringBuilder();
                warn.append("The behavior of the argument ");
                warn.append(arg).append(" (seen in ").append(source);
                warn.append(") has changed, and is now considered a normal property.  ");
                warn.append(key).append(" no longer controls what libraries are on your classpath,");
                warn.append(" use --module instead. See --help for details.");
                StartLog.warn(warn.toString());
            }

            setProperty(key,value,source,replaceProps);
            return;
        }

        // Is this an xml file?
        // 是不是xml文件
        if (FS.isXml(arg)) {
            // only add non-duplicates
            if (!xmlRefs.contains(arg)) {
                xmlRefs.add(arg);
            }
            return;
        }

        // 是不是属性文件
        if (FS.isPropertyFile(arg)) {
            // only add non-duplicates
            if (!propertyFileRefs.contains(arg)) {
                propertyFileRefs.add(arg);
            }
                return;
        }

        // Anything else is unrecognized
        // 无法识别的属性
        throw new UsageException(ERR_BAD_ARG,"Unrecognized argument: \"%s\" in %s",arg,source);
    }

    /**
     * 启用模块
     *
     * @param source
     * @param moduleNames
     */
    private void enableModules(String source, List<String> moduleNames) {
        for (String moduleName : moduleNames) {
            modules.add(moduleName);
            List<String> list = sources.get(moduleName);
            if (list == null) {
                list = new ArrayList<String>();
                sources.put(moduleName,list);
            }
            list.add(source);
        }
    }

    /**
     * 解析模块
     *
     * @param module
     */
    public void parseModule(Module module) {
        if(module.hasDefaultConfig()) {
            for(String line: module.getDefaultConfig()) {
                parse(line,module.getFilesystemRef(),false);
            }
        }
    }

    /**
     * 解析额外的xml文件
     *
     * @param baseHome
     * @throws IOException
     */
    public void resolveExtraXmls(BaseHome baseHome) throws IOException {
        // Find and Expand XML files
        for (String xmlRef : xmlRefs) {
            // Straight Reference
            Path xmlfile = baseHome.getPath(xmlRef);
            if (!FS.exists(xmlfile)) {
                xmlfile = baseHome.getPath("etc/" + xmlRef);
            }
            addUniqueXmlFile(xmlRef,xmlfile);
        }
    }

    /**
     * 解析属性文件
     *
     * @param baseHome
     * @throws IOException
     */
    public void resolvePropertyFiles(BaseHome baseHome) throws IOException {
        // Find and Expand property files
        for (String propertyFileRef : propertyFileRefs) {
            // Straight Reference
            Path propertyFile = baseHome.getPath(propertyFileRef);
            if (!FS.exists(propertyFile)) {
                propertyFile = baseHome.getPath("etc/" + propertyFileRef);
            }
            addUniquePropertyFile(propertyFileRef,propertyFile);
        }
    }

    /**
     * 允许的模块
     *
     * @param allModules
     */
    public void setAllModules(Modules allModules) {
        this.allModules = allModules;
    }

    /**
     * 设置属性
     *
     * @param key
     * @param value
     * @param source
     * @param replaceProp
     */
    private void setProperty(String key, String value, String source, boolean replaceProp) {
        // Special / Prevent override from start.ini's
        // 覆盖start.ini中的配置
        if (key.equals("jetty.home")) {
            properties.setProperty("jetty.home",System.getProperty("jetty.home"),source);
            return;
        }

        // Special / Prevent override from start.ini's
        // 覆盖start.ini中的配置
        if (key.equals("jetty.base")) {
            properties.setProperty("jetty.base",System.getProperty("jetty.base"),source);
            return;
        }

        // Normal
        // 其他正常参数
        if (replaceProp) {
            // always override
            properties.setProperty(key,value,source);
        } else {
            // only set if unset
            if (!properties.containsKey(key)) {
                properties.setProperty(key,value,source);
            }
        }
    }

    /**
     * 设置为启动
     *
     * @param run
     */
    public void setRun(boolean run) {
        this.run = run;
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StartArgs [enabledModules=");
        builder.append(modules);
        builder.append(", xmlRefs=");
        builder.append(xmlRefs);
        builder.append(", properties=");
        builder.append(properties);
        builder.append(", jvmArgs=");
        builder.append(jvmArgs);
        builder.append("]");
        return builder.toString();
    }

}
