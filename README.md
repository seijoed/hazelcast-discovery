hazelcast-discovery
===================

ActiveMQ : Hazelcast discovery module

Drop a Hazelcast jar and the hazelcast discovery jar in the lib dir.

configure a network connector.

On the "master" <-- The master could be anything running HazelCast

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