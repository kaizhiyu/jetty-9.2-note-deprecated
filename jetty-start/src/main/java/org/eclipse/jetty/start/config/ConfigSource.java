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

import org.eclipse.jetty.start.Props;
import org.eclipse.jetty.start.RawArgs;

/**
 * A Configuration Source
 *
 * 配置源
 */
public interface ConfigSource {
    /**
     * The identifier for this source.
     * <p>
     * Used in end-user display of the source.
     *
     * 当前配置源的ID
     * 终端用户可以看到它
     *
     * @return the configuration source identifier.
     */
    public String getId();

    /**
     * The weight of this source, used for proper ordering of the config source search order.
     * <p>
     * Recommended Weights:
     * <pre>
     *           -1 = the command line
     *            0 = the ${jetty.base} source
     *       [1..n] = include-jetty-dir entries from command line
     *     [n+1..n] = include-jetty-dir entries from start.ini (or start.d/*.ini) 
     *      9999999 = the ${jetty.home} source
     * </pre>
     *
     * 当前配置源的权重
     * 常见的权重有:
     * 1).命令行的权重最高，为-1
     * 2).${jetty.base} 配置源中的配置，为0
     * 3).include-jetty-dir从命令行得到的实体，次之
     * 4).include-jetty-dir从start.ini中或者start.d/*.ini中读取到的值，再次之
     * 5).从${jetty.home}中读取到的值，最低
     * 
     * @return the weight of the config source. (lower value is more important)
     */
    public int getWeight();

    /**
     * The list of Arguments for this ConfigSource
     *
     * 获取原始参数
     * 
     * @return the list of Arguments for this ConfigSource
     */
    public RawArgs getArgs();

    /**
     * The properties for this ConfigSource
     *
     * 当前配置源的属性
     *
     * @return the properties for this ConfigSource
     */
    public Props getProps();
    
    /**
     * Return the value of the specified property.
     *
     * 获取具体的配置
     * 
     * @param key the key to lookup
     * @return the value of the property, or null if not found
     */
    public String getProperty(String key);
}
