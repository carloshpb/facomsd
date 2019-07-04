package server_client.client;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.profile.ConsensusProfile;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Namespace;
import io.atomix.utils.serializer.Namespaces;
import io.atomix.utils.serializer.Serializer;
import server_client.client.view.TerminalView;
import server_client.model.Message;

import java.util.LinkedList;
import java.util.List;

public class DistributedMapClient {
    public static void main(String[] args) {
        int myId = Integer.parseInt(args[0]);
        List<Address> addresses = new LinkedList<>();

        Serializer serializer = Serializer.using(Namespace.builder()
                .register(Namespaces.BASIC)
                .register(MemberId.class)
                .register(Message.class)
                .build());

        for(int i = 2; i <args.length; i+=2)
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

        final TerminalView terminalView = new TerminalView();

        boolean exit = false;

        while (!exit) {
            Message message = terminalView.startReadMessage();

            switch (message.getLastOption()) {
                case 1:
                case 2:
                case 3:
                case 4:
                    atomix.getCommunicationService().send("test", message, serializer::encode, serializer::decode, MemberId.from("member-" + args[1])).thenAccept(System.out::println);
                    break;
                default:
                    exit = true;
                    break;
            }
        }

//        Serializer serializer = Serializer.using(Namespace.builder()
//                .register(Namespaces.BASIC)
//                .register(MemberId.class)
//                .register(Message.class)
//                .build());

//        Message test = new Message(1,1,"Ola");
//
//        atomix.getCommunicationService()
//
//        atomix.getCommunicationService().send("testando", test, MemberId.from("member-0"));



//        DistributedMap<Object, Object> map = atomix.mapBuilder("my-map")
//                .withCacheEnabled()
//                .build();
//
//        map.put("foo", "Hello world!");
//
//        System.out.println(map.get("foo"));
//        System.out.println(map.get("bar"));
//
//        String value = (String) map.get("foo");
//
//        if (map.replace("foo", value, "Hello world again!")) {
//            System.out.println();
//        }
//
//        System.out.println(map.get("foo"));
//        System.out.println(map.get("bar"));

    }
}

