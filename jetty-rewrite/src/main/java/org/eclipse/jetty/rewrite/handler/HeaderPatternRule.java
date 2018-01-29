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
 * Sets the header in the response whenever the rule finds a match.
 *
 * 设置响应头
 */
public class HeaderPatternRule extends PatternRule {

    /**
     * 键
     */
    private String _name;

    /**
     * 值
     */
    private String _value;

    /**
     * 是否是添加
     */
    private boolean _add=false;

    /* ------------------------------------------------------------ */

    /**
     * 构造方法
     */
    public HeaderPatternRule() {
        _handling = false;
        _terminating = false;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the header name.
     *
     * 设置键
     * 
     * @param name name of the header field
     */
    public void setName(String name) {
        _name = name;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the header value. The value can be either a <code>String</code> or <code>int</code> value.
     *
     * 设置值
     *
     * @param value of the header field
     */
    public void setValue(String value) {
        _value = value;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the Add flag.
     *
     * 是否是添加模式
     *
     * @param add If true, the header is added to the response, otherwise the header it is set on the response.
     */
    public void setAdd(boolean add) {
        _add = add;
    }

    /* ------------------------------------------------------------ */
    /**
     * Invokes this method when a match found. If the header had already been set, 
     * the new value overwrites the previous one. Otherwise, it adds the new 
     * header name and value.
     *
     * 执行
     *
     *@see org.eclipse.jetty.rewrite.handler.Rule#matchAndApply(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // process header
        if (_add) {
            response.addHeader(_name, _value);
        } else {
            response.setHeader(_name, _value);
        }
        return target;
    }
    
    

    /* ------------------------------------------------------------ */
    /**
     * Returns the header name.
     *
     * 返回键
     *
     * @return the header name.
     */
    public String getName() {
        return _name;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the header value.
     *
     * 返回值
     *
     * @return the header value.
     */
    public String getValue() {
        return _value;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the add flag value.
     *
     * 是不是添加模式
     */
    public boolean isAdd() {
        return _add;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the header contents.
     *
     * 转换为字符串
     */
    @Override
    public String toString() {
        return super.toString()+"["+_name+","+_value+"]";
    }
}
