package server_client.client;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.profile.ConsensusProfile;
import io.atomix.utils.net.Address;

import java.util.LinkedList;
import java.util.List;

public class DistributedMapClient {
    public static void main(String[] args) {
        int myId = Integer.parseInt(args[0]);
        List<Address> addresses = new LinkedList<>();

        for(int i = 1; i <args.length; i+=2)
        {
            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
            addresses.add(address);
        }

        AtomixBuilder builder = Atomix.builder();
        Atomix atomix = builder.withMemberId("client-"+myId)
                .withAddress(new Address("127.0.0.1", 6000))
                .withMembershipProvider(BootstrapDiscoveryProvider.builder()
                        .withNodes( Node.builder()
                                        .withId("member-0")
                                        .withAddress(addresses.get(0))
                                        .build(),
                                Node.builder()
                                        .withId("member-1")
                                        .withAddress(addresses.get(1))
                                        .build(),
                                Node.builder()
                                        .withId("member-2")
                                        .withAddress(addresses.get(2))
                                        .build())
                        .build())
                .build();

        atomix.start().join();
        System.out.println("Cluster joined");

        DistributedMap<Object, Object> map = atomix.mapBuilder("my-map")
                .withCacheEnabled()
                .build();

        map.put("foo", "Hello world!");

        System.out.println(map.get("foo"));
        System.out.println(map.get("bar"));

        String value = (String) map.get("foo");

        if (map.replace("foo", value, "Hello world again!")) {
            System.out.println();
        }

        System.out.println(map.get("foo"));
        System.out.println(map.get("bar"));

    }
}

