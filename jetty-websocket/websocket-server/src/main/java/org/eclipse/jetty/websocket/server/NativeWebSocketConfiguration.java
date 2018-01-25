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

package org.eclipse.jetty.websocket.server;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.http.pathmap.PathMappings;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http.pathmap.RegexPathSpec;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

/**
 * Interface for Configuring Jetty Server Native WebSockets
 * <p>
 * Only applicable if using {@link WebSocketUpgradeFilter}
 * </p>
 */
public class NativeWebSocketConfiguration extends ContainerLifeCycle implements MappedWebSocketCreator, Dumpable
{
    private final WebSocketServerFactory factory;
    private final PathMappings<WebSocketCreator> mappings = new PathMappings<>();
    
    public NativeWebSocketConfiguration()
    {
        this(new WebSocketServerFactory());
    }
    
    public NativeWebSocketConfiguration(WebSocketServerFactory webSocketServerFactory)
    {
        this.factory = webSocketServerFactory;
        addBean(this.factory);
    }
    
    @Override
    public void doStop() throws Exception
    {
        mappings.reset();
        super.doStop();
    }
    
    @Override
    public String dump()
    {
        return ContainerLifeCycle.dump(this);
    }
    
    @Override
    public void dump(Appendable out, String indent) throws IOException
    {
        // TODO: show factory/mappings ?
        mappings.dump(out, indent);
    }
    
    /**
     * Get WebSocketServerFactory being used.
     *
     * @return the WebSocketServerFactory being used.
     */
    public WebSocketServerFactory getFactory()
    {
        return this.factory;
    }
    
    /**
     * Get the matching {@link MappedResource} for the provided target.
     *
     * @param target the target path
     * @return the matching resource, or null if no match.
     */
    public MappedResource<WebSocketCreator> getMatch(String target)
    {
        return this.mappings.getMatch(target);
    }
    
    /**
     * Used to configure the Default {@link WebSocketPolicy} used by all endpoints that
     * don't redeclare the values.
     *
     * @return the default policy for all WebSockets
     */
    public WebSocketPolicy getPolicy()
    {
        return this.factory.getPolicy();
    }
    
    /**
     * Manually add a WebSocket mapping.
     *
     * @param pathSpec the pathspec to respond on
     * @param creator the websocket creator to activate on the provided mapping.
     */
    public void addMapping(PathSpec pathSpec, WebSocketCreator creator)
    {
        mappings.put(pathSpec, creator);
    }
    
    /**
     * Manually add a WebSocket mapping.
     *
     * @param spec the pathspec to respond on
     * @param creator the websocket creator to activate on the provided mapping
     * @deprecated use {@link #addMapping(PathSpec, WebSocketCreator)} instead.
     */
    @Deprecated
    public void addMapping(org.eclipse.jetty.websocket.server.pathmap.PathSpec spec, WebSocketCreator creator)
    {
        if (spec instanceof org.eclipse.jetty.websocket.server.pathmap.ServletPathSpec)
        {
            addMapping(new ServletPathSpec(spec.getPathSpec()), creator);
        }
        else if (spec instanceof org.eclipse.jetty.websocket.server.pathmap.RegexPathSpec)
        {
            addMapping(new RegexPathSpec(spec.getPathSpec()), creator);
        }
        else
        {
            throw new RuntimeException("Unsupported (Deprecated) PathSpec implementation type: " + spec.getClass().getName());
        }
    }
    
    /**
     * Manually add a WebSocket mapping.
     *
     * @param pathSpec the pathspec to respond on
     * @param endpointClass the endpoint class to use for new upgrade requests on the provided
     * pathspec (can be an {@link org.eclipse.jetty.websocket.api.annotations.WebSocket} annotated
     * POJO, or implementing {@link org.eclipse.jetty.websocket.api.WebSocketListener})
     */
    public void addMapping(PathSpec pathSpec, final Class<?> endpointClass)
    {
        mappings.put(pathSpec, new WebSocketCreator()
        {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
            {
                try
                {
                    return endpointClass.newInstance();
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    throw new WebSocketException("Unable to create instance of " + endpointClass.getName(), e);
                }
            }
        });
    }

    @Override
    public void addMapping(String rawspec, WebSocketCreator creator)
    {
        PathSpec spec = toPathSpec(rawspec);
        addMapping(spec, creator);
    }

    private PathSpec toPathSpec(String rawspec)
    {
        // Determine what kind of path spec we are working with
        if (rawspec.charAt(0) == '/' || rawspec.startsWith("*.") || rawspec.startsWith("servlet|"))
        {
            return new ServletPathSpec(rawspec);
        }
        else if (rawspec.charAt(0) == '^' || rawspec.startsWith("regex|"))
        {
            return new RegexPathSpec(rawspec);
        }
        else if (rawspec.startsWith("uri-template|"))
        {
            return new UriTemplatePathSpec(rawspec.substring("uri-template|".length()));
        }

        // TODO: add ability to load arbitrary jetty-http PathSpec implementation
        // TODO: perhaps via "fully.qualified.class.name|spec" style syntax

        throw new IllegalArgumentException("Unrecognized path spec syntax [" + rawspec + "]");
    }

    @Override
    public WebSocketCreator getMapping(String rawspec)
    {
        PathSpec pathSpec = toPathSpec(rawspec);
        for (MappedResource<WebSocketCreator> mapping : mappings)
        {
            if (mapping.getPathSpec().equals(pathSpec))
                return mapping.getResource();
        }
        return null;
    }

    @Override
    public boolean removeMapping(String rawspec)
    {
        PathSpec pathSpec = toPathSpec(rawspec);
        boolean removed = false;
        for (Iterator<MappedResource<WebSocketCreator>> iterator = mappings.iterator(); iterator.hasNext(); )
        {
            MappedResource<WebSocketCreator> mapping = iterator.next();
            if (mapping.getPathSpec().equals(pathSpec))
            {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Manually add a WebSocket mapping.
     *
     * @param rawspec the pathspec to map to (see {@link MappedWebSocketCreator#addMapping(String, WebSocketCreator)} for syntax details)
     * @param endpointClass the endpoint class to use for new upgrade requests on the provided
     * pathspec (can be an {@link org.eclipse.jetty.websocket.api.annotations.WebSocket} annotated
     * POJO, or implementing {@link org.eclipse.jetty.websocket.api.WebSocketListener})
     */
    public void addMapping(String rawspec, final Class<?> endpointClass)
    {
        PathSpec pathSpec = toPathSpec(rawspec);
        addMapping(pathSpec, endpointClass);
    }

    @Override
    public org.eclipse.jetty.websocket.server.pathmap.PathMappings<WebSocketCreator> getMappings()
    {
        throw new IllegalStateException("Access to PathMappings cannot be supported. See alternative API in javadoc for "
                + MappedWebSocketCreator.class.getName());
    }
}
