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

/**
 * A Usage Error has occured. Print the usage and exit with the appropriate exit code.
 *
 * 使用异常
 */
@SuppressWarnings("serial")
public class UsageException extends RuntimeException {
    /**
     * 日志错误
     * 它都是在StartLog类中抛出的
     */
    public static final int ERR_LOGGING = -1;

    /**
     * 调用主类错误
     */
    public static final int ERR_INVOKE_MAIN = -2;

    /**
     * 退出错误
     */
    public static final int ERR_NOT_STOPPED = -4;

    /**
     * 未知错误
     */
    public static final int ERR_UNKNOWN = -5;

    /**
     * 参数错误
     */
    public static final int ERR_BAD_ARG = -6;

    /**
     * 退出码
     */
    private int exitCode;

    /**
     * 构造方法
     *
     * @param exitCode
     * @param format
     * @param objs
     */
    public UsageException(int exitCode, String format, Object... objs) {
        super(String.format(format,objs));
        this.exitCode = exitCode;
    }

    /**
     * 构造方法
     *
     * @param exitCode
     * @param cause
     */
    public UsageException(int exitCode, Throwable cause) {
        super(cause);
        this.exitCode = exitCode;
    }

    /**
     * 构造方法
     *
     * @return
     */
    public int getExitCode() {
        return exitCode;
    }
}
