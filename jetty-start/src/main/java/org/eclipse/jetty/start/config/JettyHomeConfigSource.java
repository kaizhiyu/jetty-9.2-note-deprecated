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

package org.eclipse.jetty.start.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * ${jetty.home} specific ConfigSource
 *
 * 特定的${jetty.home}配置
 */
public class JettyHomeConfigSource extends DirConfigSource {

    /**
     * Standard weight for ${jetty.home}, so that it comes after everything else
     *
     * 对于${jetty.home}的标准权重
     * 它在所有其他的配置之后
     */
    private final static int WEIGHT = 9999999;

    /**
     * 构造方法
     *
     * @param dir
     * @throws IOException
     */
    public JettyHomeConfigSource(Path dir) throws IOException {
        super("${jetty.home}",dir,WEIGHT,false);
    }
}
