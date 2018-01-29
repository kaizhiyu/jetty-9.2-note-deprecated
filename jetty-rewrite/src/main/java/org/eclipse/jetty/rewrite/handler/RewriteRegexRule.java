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

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;

/**
 * Rewrite the URI by matching with a regular expression. 
 * The replacement string may use $n" to replace the nth capture group.
 * If the replacement string contains ? character, then it is split into a path
 * and query string component.  The replacement query string may also contain $Q, which 
 * is replaced with the original query string. 
 * The returned target contains only the path.
 *
 * 通过正则表达式来重写URI
 * 它可以通过$n来替换第n个组
 * 如果替换的字符串包含?这个字符，它会被分割为path和query两部分
 * 我们可以用$Q来替代query部分
 * 返回的对象只包含path部分
 *
 */
public class RewriteRegexRule extends RegexRule  implements Rule.ApplyURI {

    /**
     * 替换
     */
    private String _replacement;

    /**
     * 查询字符串部分
     */
    private String _query;

    /**
     * 是否有查询内容
     */
    private boolean _queryGroup;

    /* ------------------------------------------------------------ */

    /**
     * 构造方法
     */
    public RewriteRegexRule() {
        _handling = false;
        _terminating = false;
    }

    /* ------------------------------------------------------------ */
    /**
     * Whenever a match is found, it replaces with this value.
     *
     * 当发现匹配的内容时，它用该值去替换
     * 
     * @param replacement the replacement string.
     */
    public void setReplacement(String replacement) {
        String[] split = replacement.split("\\?",2);
        _replacement = split[0];
        _query = split.length == 2 ? split[1] : null;
        _queryGroup = _query != null && _query.contains("$Q");
    }


    /* ------------------------------------------------------------ */
    /**
     * 执行
     *
     */
    @Override
    public String apply(String target, HttpServletRequest request, HttpServletResponse response, Matcher matcher) throws IOException {
        target=_replacement;
        String query=_query;
        for (int g = 1; g <= matcher.groupCount(); g++) {
            String group = matcher.group(g);
            if (group == null) {
                group = "";
            } else {
                group = Matcher.quoteReplacement(group);
            }
            target = target.replaceAll("\\$" + g, group);
            if (query!=null) {
                query=query.replaceAll("\\$" + g, group);
            }
        }

        if (query!=null) {
            if (_queryGroup) {
                query=query.replace("$Q", request.getQueryString() == null ? "" : request.getQueryString());
            }
            request.setAttribute("org.eclipse.jetty.rewrite.handler.RewriteRegexRule.Q",query);
        }
        
        return target;
    }

    /* ------------------------------------------------------------ */

    /**
     * 匹配URI
     * 该方法在RuleContainer中被调用
     *
     * @param request
     * @param oldURI
     * @param newURI
     * @throws IOException
     */
    @Override
    public void applyURI(Request request, String oldURI, String newURI) throws IOException {
        if (_query == null) {
            request.setRequestURI(newURI);
        } else {
            String query = (String)request.getAttribute("org.eclipse.jetty.rewrite.handler.RewriteRegexRule.Q");
            
            if (!_queryGroup && request.getQueryString()!=null) {
                query = request.getQueryString()+"&"+query;
            }
            HttpURI uri = new HttpURI(newURI+"?"+query);
            request.setUri(uri);
            request.setRequestURI(newURI);
            request.setQueryString(query);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the replacement string.
     *
     * 返回替换字符串
     */
    @Override
    public String toString() {
        return super.toString()+"["+_replacement+"]";
    }
}
