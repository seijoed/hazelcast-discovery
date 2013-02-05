hazelcast-discovery
===================

ActiveMQ : Hazelcast discovery module

Drop the hazelcast discovery jar in the lib dir.

configure a network connector.

On the "master" <-- The master could be anything running HazelCast or execute the JAR file directly for a stand-alone master.

    Configuration can be provided in cluster.properties.

        port=5009
        address=0.0.0.0
        clusterName=AMQ
        members= <Comma separated list>

            <networkConnectors>
              <networkConnector uri="hazelcast://localhost?port=5009&amp;service=tcp://127.0.0.1:61616"
                dynamicOnly="false"
                networkTTL="3"
                prefetchSize="1"
                decreaseNetworkConsumerPriority="true" />
            </networkConnectors>

On the other nodes (The parent is the "creator" of the HazelCast instance)

            <networkConnectors>
              <networkConnector uri="hazelcast://localhost?port=5010&amp;parents=localhost:5009&amp;service=tcp://127.0.0.1:61617"
                dynamicOnly="false"
                networkTTL="3"
                prefetchSize="1"
                decreaseNetworkConsumerPriority="true" />
            </networkConnectors>