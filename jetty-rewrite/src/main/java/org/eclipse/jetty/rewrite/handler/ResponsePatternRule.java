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

package org.eclipse.jetty.rewrite.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sends the response code whenever the rule finds a match.
 *
 * 响应码匹配规则
 */
public class ResponsePatternRule extends PatternRule {

    /**
     * 响应码
     */
    private String _code;

    /**
     * 原因
     */
    private String _reason = "";

    /* ------------------------------------------------------------ */

    /**
     * 构造方法
     */
    public ResponsePatternRule() {
        _handling = true;
        _terminating = true;
    }

    /* ------------------------------------------------------------ */
    /**
     * 设置响应码
     *
     * Sets the response status code. 
     * @param code response code
     */
    public void setCode(String code) {
        _code = code;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the reason for the response status code. Reasons will only reflect
     * if the code value is greater or equal to 400.
     *
     * 设置原因
     *
     * @param reason
     */
    public void setReason(String reason) {
        _reason = reason;
    }

    /* ------------------------------------------------------------ */
    /**
     * 处理
     */
    @Override
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int code = Integer.parseInt(_code);

        // status code 400 and up are error codes
        if (code >= 400) {
            response.sendError(code, _reason);
        } else {
            response.setStatus(code);
        }
        return target;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the code and reason string.
     *
     * 返回错误码和原因
     */
    @Override
    public String toString() {
        return super.toString()+"["+_code+","+_reason+"]";
    }
}
