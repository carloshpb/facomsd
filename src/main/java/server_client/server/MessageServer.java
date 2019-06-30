package server_client.server;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import server_client.constants.StringsConstants;
import server_client.model.Message;
import server_client.model.query.SendMessageQuery;
import server_client.server.database.MemoryDB;
import server_client.server.threads.handlers.MessageData;
import server_client.server.threads.message_queues.first_stage.FirstQueueThread;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

public class MessageServer extends StateMachine {

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

    public Message SendMessage(Commit<SendMessageQuery> commit) {

        BlockingQueue<Message> answerQueue = new LinkedBlockingDeque<>();

        SendMessageQuery sendMessageQuery = commit.operation();
        Message receivedMessage = sendMessageQuery.getMessage();

        LOGGER.info("Mensagem obtida: " + receivedMessage);

        if (receivedMessage == null || receivedMessage.getLastOption() < 1 || receivedMessage.getLastOption() > 4) {
            LOGGER.info(StringsConstants.ERR_INVALID_OPTION.toString());
            return new Message(StringsConstants.ERR_INVALID_OPTION.toString());
        }

        MessageData messageData = new MessageData(receivedMessage, answerQueue);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new FirstQueueThread(messageData));


        try {
            receivedMessage =  answerQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new Message(e.getMessage());
        }

        return receivedMessage;

    }

    public static void main(String[] args) {
        int myId = Integer.parseInt(args[0]);
        List<Address> addresses = new LinkedList<>();

        for(int i = 1; i <args.length; i+=2)
        {
            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
            addresses.add(address);
        }

        CopycatServer.Builder builder = CopycatServer.builder(addresses.get(myId))
                .withStateMachine(MessageServer::new)
                .withTransport( NettyTransport.builder()
                        .withThreads(4)
                        .build())
                .withStorage( Storage.builder()
                        .withDirectory(new File("logs_"+myId)) //Must be unique
                        .withStorageLevel(StorageLevel.DISK)
                        .build());
        CopycatServer server = builder.build();

        if(myId == 0)
        {
            server.bootstrap().join();
        }
        else
        {
            server.join(addresses).join();
        }
    }
}
