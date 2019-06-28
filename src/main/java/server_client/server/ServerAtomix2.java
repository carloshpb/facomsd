package server_client.server;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.map.AsyncDistributedMap;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.profile.ConsensusProfile;
import io.atomix.utils.net.Address;

import java.util.LinkedList;
import java.util.List;

public class ServerAtomix2 {
    public static void main(String[] args) {
        int myId = Integer.parseInt(args[0]);
        List<Address> addresses = new LinkedList<>();

        for(int i = 1; i <args.length; i+=2)
        {
            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
            addresses.add(address);
        }

        AtomixBuilder builder = Atomix.builder();
        Atomix atomix = builder.withMemberId("member-"+myId)
                .withAddress(addresses.get(myId))
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
                .withProfiles(ConsensusProfile.builder().withDataPath("/tmp/member-"+myId).withMembers("member-1", "member-2", "member-3").build())
                .build();

        atomix.start().join();

        System.out.println("Cluster joined");


        atomix.


//        DistributedMap<String, String> map = (DistributedMap<String, String>) atomix.getAtomicMap("my-map");
//        AsyncDistributedMap<String, String> asyncMap = map.async();
//
//        asyncMap.get("foo").thenAccept(value -> {
//            asyncMap.replace("foo", value, value + "world").thenAccept(succeeded -> {
//                System.out.println("Replace successful!");
//            });
//        });
    }
}
