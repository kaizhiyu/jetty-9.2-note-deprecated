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
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects the response by matching with a regular expression.
 * The replacement string may use $n" to replace the nth capture group.
 *
 * 重定向正则处理
 */
public class RedirectRegexRule extends RegexRule {

    /**
     * 要重定向到的位置
     */
    private String _replacement;

    /**
     * 构造方法
     */
    public RedirectRegexRule() {
        _handling = true;
        _terminating = true;
    }

    /**
     * Whenever a match is found, it replaces with this value.
     *
     * 设置替换内容
     *
     * @param replacement the replacement string.
     */
    public void setReplacement(String replacement) {
        _replacement = replacement;
    }

    /**
     * 执行
     *
     * @param target field to attempt match
     * @param request request object
     * @param response response object
     * @param matcher The Regex matcher that matched the request (with capture groups available for replacement).
     * @return
     * @throws IOException
     */
    @Override
    protected String apply(String target, HttpServletRequest request, HttpServletResponse response, Matcher matcher) throws IOException {
        target=_replacement;
        for (int g=1;g<=matcher.groupCount();g++) {
            String group = matcher.group(g);
            target = target.replaceAll("\\$"+g,group);
        }

        response.sendRedirect(response.encodeRedirectURL(target));
        return target;
    }
}
