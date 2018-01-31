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

package org.eclipse.jetty.webapp;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlParser;

/**
 * 描述符
 */
public abstract class Descriptor {
    /**
     * 描述符
     */
    protected Resource _xml;

    /**
     * 根节点
     */
    protected XmlParser.Node _root;

    /**
     * 解析器
     */
    protected XmlParser _parser;

    /**
     * 验证
     */
    protected boolean _validating;

    /**
     * 构造方法
     *
     * @param xml
     */
    public Descriptor (Resource xml) {
        _xml = xml;
    }

    /**
     * 确认解析器
     *
     * @throws ClassNotFoundException
     */
    public abstract void ensureParser() throws ClassNotFoundException;

    /**
     * 是否验证
     *
     * @param validating
     */
    public void setValidating (boolean validating) {
       _validating = validating;
    }

    /**
     * 解析
     * 一定要记得关闭资源
     *
     * @throws Exception
     */
    public void parse () throws Exception {
        if (_parser == null) {
            ensureParser();
        }
        
        if (_root == null) {
            try {
                _root = _parser.parse(_xml.getInputStream());
            } finally {
                _xml.close();
            }
        }
    }

    /**
     * 获取资源
     *
     * @return
     */
    public Resource getResource () {
        return _xml;
    }

    /**
     * 获取根节点
     *
     * @return
     */
    public XmlParser.Node getRoot () {
        return _root;
    }

    /**
     * 转换为字符串
     *
     * @return
     */
    public String toString() {
        return this.getClass().getSimpleName() + "(" + _xml + ")";
    }
}
