package com.savoirtech.activemq.discovery;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelCastDiscoveryParent implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(HazelCastDiscoveryParent.class);
    private final static String CONFIG_FILE = "cluster.properties";

    Properties prop;
    String port = "5009";
    String inetAddress = "0.0.0.0";
    String clustername = "AMQ";

    private final String service = "hazelcast-parent";

    List<String> members;

    private Config cfg;
    private NetworkConfig network;

    private IMap<String, String> brokerData;

    public static void main(String[] args) {

        HazelCastDiscoveryParent disc = new HazelCastDiscoveryParent();
        disc.startHazelCastCluster();
    }

    private void startHazelCastCluster() {
        prop = new Properties();
        try {
            //load a properties file
            prop.load(new FileInputStream(CONFIG_FILE));

            //get the property value and print it out

            if (prop.getProperty("port") != null && !"".equals(prop.getProperty("port"))) {
                port = prop.getProperty("port");
            }

            if (prop.getProperty("address") != null && !"".equals(prop.getProperty("address"))) {
                inetAddress = prop.getProperty("address");
            }

            if (prop.getProperty("clusterName") != null && !"".equals(prop.getProperty("clusterName"))) {
                clustername = prop.getProperty("clusterName");
            }

            if (prop.getProperty("members") != null && !"".equals(prop.getProperty("members"))) {

                members = Arrays.asList(prop.getProperty("members").split(","));
            }



            //Now we wire up the cluster parent.

        } catch (IOException ex) {
            //No props, using defaults.
            LOG.info("No " + CONFIG_FILE + " found, loading with defaults");
        }

        LOG.info("Starting the Hazelcast discovery agent");
        cfg = new Config();
        cfg.setPort(Integer.parseInt(port));
        cfg.setPortAutoIncrement(false);

        network = cfg.getNetworkConfig();

        Join join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);

        //Iterate over the options you have for ip-address parents.

        if (members != null) {
            for (String member : members) {
                LOG.info("Adding member " + member + " to the HazelCastCluster");
                join.getTcpIpConfig().addMember(member).setRequiredMember(member);
            }
        }

        TcpIpConfig tcpIpConfig = join.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);

        LOG.debug("Listening to : " + tcpIpConfig.getAddresses());

        HazelcastInstance instance = Hazelcast.init(cfg);

        brokerData = Hazelcast.getMap(clustername);

        // brokerData.addEntryListener(this, true);
         //instance.getCluster().addMembershipListener(this);

        brokerData.put(instance.getCluster().getLocalMember().getInetSocketAddress().getAddress() + ":" + instance.getCluster().getLocalMember()
                                                                                                                  .getInetSocketAddress().getPort(),
            service);
        LOG.debug("Values " + instance.getCluster().getMembers());
        LOG.debug("Values " + brokerData.values());
    }

    @Override
    public void onMessage(Message message) {
        LOG.info(message.toString());
    }
}
