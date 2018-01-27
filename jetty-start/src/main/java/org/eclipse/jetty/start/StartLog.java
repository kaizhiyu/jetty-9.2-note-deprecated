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
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import org.eclipse.jetty.start.config.CommandLineConfigSource;

/**
 * Centralized Place for logging.
 * <p>
 * Because startup cannot rely on Jetty's Logging, an alternative logging is established.
 * <p>
 * Optional behavior is to create a ${jetty.base}/logs/start.log with whatever output the startup process produces.
 *
 * 启动日志
 * 因为启动不能依赖jetty的日志，因此这里有一个替代的日志系统
 * 可选的内容是创建一个 ${jetty.base}/logs/start.log来输出启动日志
 */
public class StartLog {

    /**
     * 标准普通输出
     */
    private final static PrintStream stdout = System.out;

    /**
     * 标准错误输出
     */
    private final static PrintStream stderr = System.err;

    /**
     * 普通输出
     */
    private static volatile PrintStream out = System.out;

    /**
     * 错误输出
     */
    private static volatile PrintStream err = System.err;

    /**
     * 实例
     */
    private final static StartLog INSTANCE = new StartLog();

    /**
     * 写入调试信息
     *
     * @param format
     * @param args
     */
    public static void debug(String format, Object... args) {
        if (INSTANCE.debug) {
            out.printf(format + "%n",args);
        }
    }

    /**
     * 写入跟踪信息
     *
     * @param format
     * @param args
     */
    public static void trace(String format, Object... args) {
        if (INSTANCE.trace) {
            out.printf("TRACE: " + format + "%n",args);
        }
    }

    /**
     * 调试时写入异常
     *
     * @param t
     */
    public static void debug(Throwable t) {
        if (INSTANCE.debug) {
            t.printStackTrace(out);
        }
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static StartLog getInstance() {
        return INSTANCE;
    }

    /**
     * 记录日志
     *
     * @param type
     * @param msg
     */
    public static void log(String type, String msg) {
        err.println(type + ": " + msg);
    }

    /**
     * 记录日志
     *
     * @param type
     * @param format
     * @param args
     */
    public static void log(String type, String format, Object... args) {
        err.printf(type + ": " + format + "%n",args);
    }

    /**
     * 通知信息
     *
     * @param format
     * @param args
     */
    public static void info(String format, Object... args) {
        log("INFO",format,args);
    }

    /**
     * 警告
     *
     * @param format
     * @param args
     */
    public static void warn(String format, Object... args) {
        log("WARNING",format,args);
    }

    /**
     * 警告
     *
     * @param t
     */
    public static void warn(Throwable t) {
        t.printStackTrace(err);
    }

    /**
     * 是否开启调试
     *
     * @return
     */
    public static boolean isDebugEnabled() {
        return INSTANCE.debug;
    }

    /**
     * 默认跟踪为false
     */
    private boolean trace = false;

    /**
     * 默认调试为false
     */
    private boolean debug = false;

    /**
     * 初始化
     * 它的主要作用就是确保
     *
     * @param baseHome
     * @param cmdLineSource
     * @throws IOException
     */
    public void initialize(BaseHome baseHome, CommandLineConfigSource cmdLineSource) throws IOException {

        // 首先确定是不是调试模式
        String dbgProp = cmdLineSource.getProperty("debug");
        if (dbgProp != null) {
            debug = Boolean.parseBoolean(dbgProp);
        }

        // 获取start-log-file属性
        String logFileName = cmdLineSource.getProperty("start-log-file");

        for (RawArgs.Entry arg : cmdLineSource.getArgs()) {
            if ("--debug".equals(arg.getLine())) {
                debug = true;
                continue;
            }

            if (arg.startsWith("--start-log-file")) {
                logFileName = Props.getValue(arg.getLine());
                continue;
            }
        }

        if (logFileName != null) {
            Path logfile = baseHome.getPath(logFileName);
            logfile = logfile.toAbsolutePath();
            initLogFile(logfile);
        }
    }

    /**
     * 初始化日志文件
     *
     * @param logfile
     * @throws IOException
     */
    public void initLogFile(Path logfile) throws IOException {
        if (logfile != null) {
            try {
                // 确保父目录具有写权限
                Path logDir = logfile.getParent();
                FS.ensureDirectoryWritable(logDir);

                Path startLog = logfile;

                // 如果该文件不存在，并且创建失败
                if (!FS.exists(startLog) && !FS.createNewFile(startLog)) {
                    // Output about error is lost in majority of cases.
                    throw new UsageException(UsageException.ERR_LOGGING,new IOException("Unable to create: " + startLog.toAbsolutePath()));
                }

                // 如果没有该文件的写权限
                if (!FS.canWrite(startLog)) {
                    // Output about error is lost in majority of cases.
                    throw new UsageException(UsageException.ERR_LOGGING,new IOException("Unable to write to: " + startLog.toAbsolutePath()));
                }

                err.println("StartLog to " + logfile);
                OutputStream fileout = Files.newOutputStream(startLog,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                PrintStream logger = new PrintStream(fileout);
                out=logger;
                err=logger;
                System.setErr(logger);
                System.setOut(logger);
                err.println("StartLog Establishing " + logfile + " on " + new Date());
            } catch (IOException e) {
                throw new UsageException(UsageException.ERR_LOGGING,e);
            }
        }
    }

    /**
     * 开启调试
     */
    public static void enableDebug() {
        getInstance().debug = true;
    }

    /**
     * 关闭日志文件
     */
    public static void endStartLog() {
        if (stderr!=err && getInstance().debug) {
            err.println("StartLog ended");
            stderr.println("StartLog ended");
        }
        System.setErr(stderr);
        System.setOut(stdout);
    }
}
