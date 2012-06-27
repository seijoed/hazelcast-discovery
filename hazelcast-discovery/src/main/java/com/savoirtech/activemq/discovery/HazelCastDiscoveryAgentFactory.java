
/*
 * Copyright (C) 2012  Savoir Technologies, Inc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.savoirtech.activemq.discovery;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryAgentFactory;
import org.apache.activemq.util.IOExceptionSupport;
import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelCastDiscoveryAgentFactory extends DiscoveryAgentFactory {

    private static final Logger LOG = LoggerFactory.getLogger(HazelCastDiscoveryAgentFactory.class);

    @Override
    protected DiscoveryAgent doCreateDiscoveryAgent(URI uri) throws IOException {
        try {

            if (LOG.isTraceEnabled()) {
                LOG.trace("doCreateDiscoveryAgent: uri = " + uri.toString());
            }

            HazelcastDiscoveryAgent hazelcastDiscoveryAgent = new HazelcastDiscoveryAgent();

            hazelcastDiscoveryAgent.setDiscoveryURI(uri);

            // allow MDA's params to be set via query arguments
            // (e.g., multicast://default?group=foo
            Map options = URISupport.parseParameters(uri);
            IntrospectionSupport.setProperties(hazelcastDiscoveryAgent, options);

            return hazelcastDiscoveryAgent;
        } catch (Throwable e) {
            throw IOExceptionSupport.create("Could not create discovery agent: " + uri, e);
        }
    }
}
