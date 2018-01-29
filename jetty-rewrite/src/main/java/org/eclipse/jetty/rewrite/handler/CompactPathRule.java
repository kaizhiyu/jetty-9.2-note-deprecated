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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.URIUtil;

/**
 * Rewrite the URI by compacting to remove //
 *
 * 兼容处理
 * 它可以移除多于的末尾的斜线
 */
public class CompactPathRule extends Rule implements Rule.ApplyURI {

    /**
     * 构造方法
     */
    public CompactPathRule() {
        _handling = false;
        _terminating = false;
    }

    /**
     * 处理URI
     *
     * @param request
     * @param oldURI
     * @param newURI
     * @throws IOException
     */
    @Override
    public void applyURI(Request request, String oldURI, String newURI) throws IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/")) {
            uri = URIUtil.compactPath(uri);
        }
        request.setRequestURI(uri);
    }

    /**
     * 匹配并且处理
     *
     * @param target The target of the request
     * @param request
     * @param response
     *
     * @return
     * @throws IOException
     */
    @Override
    public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (target.startsWith("/")) {
            return URIUtil.compactPath(target);
        }
        return target;
    }
}
