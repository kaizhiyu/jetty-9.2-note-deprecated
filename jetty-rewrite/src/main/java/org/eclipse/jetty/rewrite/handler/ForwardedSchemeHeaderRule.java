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


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;


/**
 * Set the scheme for the request 
 *
 * 
 * 转到另一个模式的请求头处理规则
 */
public class ForwardedSchemeHeaderRule extends HeaderRule {

    /**
     * 默认为https
     */
    private String _scheme="https";

    /* ------------------------------------------------------------ */

    /**
     * 获取协议
     *
     * @return
     */
    public String getScheme() {
        return _scheme;
    }

    /* ------------------------------------------------------------ */
    /**
     * 设置协议
     *
     * @param scheme the scheme to set on the request. Defaults to "https"
     */
    public void setScheme(String scheme) {
        _scheme = scheme;
    }
    
    /* ------------------------------------------------------------ */

    /**
     * 处理
     *
     * @param target
     *                field to attempt match
     * @param value
     *                header value found
     * @param request
     *                request object
     * @param response
     *                response object
     * @return
     */
    @Override
    protected String apply(String target, String value, HttpServletRequest request, HttpServletResponse response) {
        ((Request) request).setScheme(_scheme);
        return target;
    }    
}
