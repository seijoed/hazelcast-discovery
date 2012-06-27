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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastDiscoveryAgent implements DiscoveryAgent, EntryListener, MembershipListener {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastDiscoveryAgent.class);

    private URI discoveryURI;
    private String service;

    private DiscoveryListener discoveryListener;
    private Config cfg;
    private NetworkConfig network;
    private String multicast;
    private final static String clusterName = "AMQ";
    private String parents;

    private AtomicBoolean started = new AtomicBoolean(false);

    private IMap<String, String> brokerData;

    private Set<String> sentEvents = new HashSet<String>();

    String port = "5009";

    private List<String> originalMembers = new ArrayList<String>();

    @Override
    public void setDiscoveryListener(DiscoveryListener discoveryListener) {
        this.discoveryListener = discoveryListener;
    }

    @Override
    public void registerService(String s) throws IOException {
        LOG.info("Update " + s);
    }

    @Override
    public void serviceFailed(DiscoveryEvent discoveryEvent) throws IOException {
        LOG.info("Update " + discoveryEvent);
    }

    @Override
    public void start() throws Exception {

        if (started.compareAndSet(false, true)) {
            LOG.info("Starting the Hazelcast discovery agent");
            cfg = new Config();
            cfg.setPort(Integer.parseInt(port));
            cfg.setPortAutoIncrement(false);

            network = cfg.getNetworkConfig();

            Join join = network.getJoin();
            LOG.info("Setting multicast to " + Boolean.parseBoolean(multicast));
            join.getMulticastConfig().setEnabled(Boolean.parseBoolean(multicast));

            //Iterate over the options you have for ip-address parents.

            for (String member : originalMembers) {
                LOG.info("Adding member " + member + " to the HazelCastCluster");
                join.getTcpIpConfig().addMember(member).setRequiredMember(member);
            }

            TcpIpConfig tcpIpConfig = join.getTcpIpConfig();
            tcpIpConfig.setEnabled(true);

            LOG.debug("Listening to : " + tcpIpConfig.getAddresses());

            HazelcastInstance instance = Hazelcast.init(cfg);

            brokerData = Hazelcast.getMap(clusterName);

            brokerData.addEntryListener(this, true);
            instance.getCluster().addMembershipListener(this);

            brokerData.put(instance.getCluster().getLocalMember().getInetSocketAddress().getAddress() + ":" + instance.getCluster().getLocalMember()
                                                                                                                      .getInetSocketAddress()
                                                                                                                      .getPort(), service);
            LOG.debug("Values " + instance.getCluster().getMembers());
            LOG.debug("Values " + brokerData.values());
        }
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public void stop() throws Exception {
        //TODO
    }

    public void setDiscoveryURI(URI discoveryURI) {
        this.discoveryURI = discoveryURI;
    }

    public void setParents(String parents) {
        this.parents = parents;

        originalMembers = Arrays.asList(parents.split(","));
    }

    public void setMulticast(String multicast) {
        this.multicast = multicast;
    }

    //Hazelcast specific
    @Override
    public void entryAdded(EntryEvent entryEvent) {
        addServices(entryEvent);
    }

    private void addServices(EntryEvent entryEvent) {
        LOG.debug("Addition " + entryEvent);
        if (!entryEvent.getValue().equals(service)) {
            this.discoveryListener.onServiceAdd(new DiscoveryEvent(entryEvent.getValue().toString()));
            sentEvents.add(entryEvent.getValue().toString());
        }
        // When we add ourselves, lets add the others as well.

        for (String remotes : brokerData.values()) {
            if (!remotes.equals(service) && !sentEvents.contains(remotes)) {
                LOG.debug("Adding connectors for " + remotes);
                this.discoveryListener.onServiceAdd(new DiscoveryEvent(remotes));
            }
        }
    }

    @Override
    public void entryRemoved(EntryEvent entryEvent) {
        LOG.debug("Removal " + entryEvent);
        if (parents != null && (parents).contains(entryEvent.getValue().toString())) {
            LOG.warn("You have lost connection to one of the master nodes, you'll have to restart the cluster for full discovery.");
        }
        if (!entryEvent.getValue().equals(service)) {
            this.discoveryListener.onServiceRemove(new DiscoveryEvent(entryEvent.getValue().toString()));
            sentEvents.remove(entryEvent.getValue().toString());
        }
    }

    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        LOG.debug("Update " + entryEvent);
        addServices(entryEvent);
    }

    @Override
    public void entryEvicted(EntryEvent entryEvent) {
        LOG.debug("Eviction " + entryEvent);
        //TODO
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        LOG.debug("Member added to cluster " + membershipEvent);

        for (String remotes : brokerData.values()) {
            if (!remotes.equals(service) && !sentEvents.contains(remotes)) {
                LOG.debug("Adding connectors for " + remotes);
                this.discoveryListener.onServiceAdd(new DiscoveryEvent(remotes));
            }
        }
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        LOG.debug("Member removed from cluster " + membershipEvent);
    }

    public void setService(String service) {
        this.service = service;
    }
}
