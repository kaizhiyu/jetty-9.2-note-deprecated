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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Sets the cookie in the response whenever the rule finds a match.
 *
 * cookie模式匹配规则
 * 当我们发现一个cookie的规则的时候，我们在响应中把处理后的cookie加上
 *
 * @see Cookie
 */
public class CookiePatternRule extends PatternRule {
    /**
     * 键
     */
    private String _name;

    /**
     * 值
     */
    private String _value;

    /* ------------------------------------------------------------ */

    /**
     * 构造方法
     */
    public CookiePatternRule() {
        _handling = false;
        _terminating = false;
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the cookie name.
     *
     * 设置键
     * 
     * @param name a <code>String</code> specifying the name of the cookie.
     */
    public void setName(String name) {
        _name = name;
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the cookie value.
     *
     * 设置值
     * 
     * @param value a <code>String</code> specifying the value of the cookie
     * @see Cookie#setValue(String)
     */
    public void setValue(String value) {
        _value = value;
    }

    /* ------------------------------------------------------------ */
    /**
     * 处理
     *
     */
    @Override
    public String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Check that cookie is not already set
        Cookie[] cookies = request.getCookies();
        if (cookies!=null) {
            for (Cookie cookie:cookies) {
                if (_name.equals(cookie.getName()) && _value.equals(cookie.getValue())) {
                    return target;
                }
            }
        }
        
        // set it
        // 设置
        response.addCookie(new Cookie(_name, _value));
        return target;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the cookie contents.
     *
     * 返回cookie内容
     */
    @Override
    public String toString() {
        return super.toString()+"["+_name+","+_value + "]";
    }
}
