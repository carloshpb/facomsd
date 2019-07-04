package server_client.server;

import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.profile.ConsensusProfile;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Namespace;
import io.atomix.utils.serializer.Namespaces;
import io.atomix.utils.serializer.Serializer;
import server_client.constants.StringsConstants;
import server_client.model.Message;
import server_client.server.database.MemoryDB;
import server_client.server.threads.handlers.MessageData;
import server_client.server.threads.message_queues.first_stage.FirstQueueThread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class MessageServer {


    private final static Logger LOGGER = Logger.getLogger(MessageServer.class.getName());

    private static volatile BlockingQueue<MessageData> fila1;
    private static volatile BlockingQueue<Message> fila2;
    private static volatile BlockingQueue<MessageData> fila3;

    private volatile boolean exit = false;

    private ExecutorService clientThreadPool = Executors.newFixedThreadPool(10);
    private ExecutorService queueThreadPool = Executors.newFixedThreadPool(3);

    static {
        MemoryDB.getInstance();
        fila1 = new LinkedBlockingDeque<>();
        fila2 = new LinkedBlockingDeque<>();
        fila3 = new LinkedBlockingDeque<>();
    }

    public static BlockingQueue<MessageData> getFila1() {
        return fila1;
    }

    public static BlockingQueue<Message> getFila2() {
        return fila2;
    }

    public static BlockingQueue<MessageData> getFila3() {
        return fila3;
    }

//    public Message SendMessage(Commit<SendMessageQuery> commit) {
//
//        BlockingQueue<Message> answerQueue = new LinkedBlockingDeque<>();
//
//        SendMessageQuery sendMessageQuery = commit.operation();
//        Message receivedMessage = sendMessageQuery.getMessage();
//
//        LOGGER.info("Mensagem obtida: " + receivedMessage);
//
//        if (receivedMessage == null || receivedMessage.getLastOption() < 1 || receivedMessage.getLastOption() > 4) {
//            LOGGER.info(StringsConstants.ERR_INVALID_OPTION.toString());
//            return new Message(StringsConstants.ERR_INVALID_OPTION.toString());
//        }
//
//        MessageData messageData = new MessageData(receivedMessage, answerQueue);
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.submit(new FirstQueueThread(messageData));
//
//
//        try {
//            receivedMessage =  answerQueue.take();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return new Message(e.getMessage());
//        }
//
//        return receivedMessage;
//
//    }


    public static void main(String[] args) {
        int myId = Integer.parseInt(args[0]);
        List<Address> addresses = new LinkedList<>();

        Serializer serializer = Serializer.using(Namespace.builder()
                .register(Namespaces.BASIC)
                .register(MemberId.class)
                .register(Message.class)
                .build());

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

        atomix.getMembershipService().addListener(event -> {
            switch (event.type()) {
                case MEMBER_ADDED:
                    System.out.println(event.subject().id() + " joined the cluster");
                    break;
                case MEMBER_REMOVED:
                    System.out.println(event.subject().id() + " left the cluster");
                    break;
            }
        });

        BlockingQueue<Message> answerQueue = new LinkedBlockingDeque<>();

//        SendMessageQuery sendMessageQuery = commit.operation();

//        AtomicReference<Message> value = new AtomicReference<>();

        atomix.getCommunicationService().subscribe("test", serializer::encode, message -> {

            Message receivedMessage = serializer.decode(message);

            LOGGER.info("Mensagem obtida: " + receivedMessage);

            if (receivedMessage == null || receivedMessage.getLastOption() < 1 || receivedMessage.getLastOption() > 4) {
                LOGGER.info(StringsConstants.ERR_INVALID_OPTION.toString());
                return CompletableFuture.completedFuture(serializer.encode(new Message(StringsConstants.ERR_INVALID_OPTION.toString())));
            }

            MessageData messageData = new MessageData(receivedMessage, answerQueue);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new FirstQueueThread(messageData));


            try {
                receivedMessage =  answerQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return CompletableFuture.completedFuture(serializer.encode(new Message(e.getMessage())));
            }

            return CompletableFuture.completedFuture(serializer.encode(receivedMessage));
        }, serializer::decode);

//        Message receivedMessage = value.get();

//        LOGGER.info("Mensagem obtida: " + receivedMessage);
//
//        if (receivedMessage == null || receivedMessage.getLastOption() < 1 || receivedMessage.getLastOption() > 4) {
//            LOGGER.info(StringsConstants.ERR_INVALID_OPTION.toString());
//            return new Message(StringsConstants.ERR_INVALID_OPTION.toString());
//        }
//
//        MessageData messageData = new MessageData(receivedMessage, answerQueue);
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.submit(new FirstQueueThread(messageData));
//
//
//        try {
//            receivedMessage =  answerQueue.take();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return new Message(e.getMessage());
//        }
//
//        return receivedMessage;
    }
}
