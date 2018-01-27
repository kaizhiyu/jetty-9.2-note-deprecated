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
 * An non-recoverable error with Props usage
 *
 * 在属性使用上未处理的异常
 */
@SuppressWarnings("serial")
public class PropsException extends RuntimeException {

    /**
     * 构造方法
     *
     * @param message
     * @param cause
     */
    public PropsException(String message, Throwable cause) {
        super(message,cause);
    }

    /**
     * 构造方法
     *
     * @param message
     */
    public PropsException(String message) {
        super(message);
    }

    /**
     * 构造方法
     *
     * @param cause
     */
    public PropsException(Throwable cause) {
        super(cause);
    }
}
