//package server_client.server;
//
//import io.atomix.cluster.Node;
//import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
//import io.atomix.core.Atomix;
//import io.atomix.core.AtomixBuilder;
//import io.atomix.core.profile.ConsensusProfile;
//import io.atomix.utils.net.Address;
//import server_client.constants.StringsConstants;
//import server_client.model.Message;
//import server_client.server.database.MemoryDB;
//import server_client.server.threads.handlers.ClientHandler;
//import server_client.server.threads.handlers.MessageData;
//import server_client.server.threads.message_queues.first_stage.FirstQueueThread;
//import server_client.server.threads.message_queues.second_stage.SecondThirdQueueThread;
//import server_client.server.threads.message_queues.third_stage.DatabaseProcessingThread;
//import server_client.server.threads.message_queues.third_stage.LogThread;
//
//import java.io.EOFException;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.LinkedBlockingDeque;
//import java.util.logging.Logger;
//
//public class ServerAtomix {
//
//    // -- ServerThread
//
//    private final static Logger LOGGER = Logger.getLogger(ServerAtomix.class.getName());
//
//    private static volatile BlockingQueue<MessageData> fila1;
//    private static volatile BlockingQueue<Message> fila2;
//    private static volatile BlockingQueue<MessageData> fila3;
//
//    //private volatile boolean exit = false;
//
//    //private static int port;
//    //private ServerSocket serverSocket = null;
//    //private ExecutorService clientThreadPool = Executors.newFixedThreadPool(10);
//    private static volatile ExecutorService queueThreadPool = Executors.newFixedThreadPool(3);
//
//
//
//    /// -- ClientHandler
//
//    private volatile boolean exit = false;
//
////    private Socket socket = null;
////    private ObjectInputStream objectInputStream = null;
////    private ObjectOutputStream objectOutputStream = null;
//
//    private MessageData messageData;
//    private volatile BlockingQueue<Message> answerQueue;
//
//
//
//
//
//
//    // -- ServerThread
//    static {
//        MemoryDB.getInstance();
//        fila1 = new LinkedBlockingDeque<>();
//        fila2 = new LinkedBlockingDeque<>();
//        fila3 = new LinkedBlockingDeque<>();
//
//        queueThreadPool.submit(new SecondThirdQueueThread());
//        queueThreadPool.submit(new DatabaseProcessingThread());
//        queueThreadPool.submit(new LogThread());
//    }
//
//    private void startServer() {
//        try {
//            //serverSocket = new ServerSocket(port);
//            //LOGGER.info("Servidor iniciado na porta " + serverSocket.getLocalPort());
//            //LOGGER.info("A espera de um cliente conectar...");
//
//            this.getAndInsertClientConnection();
//        } catch (IOException e) {
//            LOGGER.severe(e.getMessage());
//            try {
//                this.stopServer();
//            } catch (IOException ex) {
//                LOGGER.severe(ex.getMessage());
//            }
//        }
//    }
//
//    private void getAndInsertClientConnection() throws IOException{
//        while (!exit && !Thread.interrupted()) {
//
//            ClientHandler clientConnection = new ClientHandler(this.serverSocket.accept());
//            LOGGER.info("Conectou!");
//            clientThreadPool.submit(clientConnection);
//        }
//    }
//
//    public synchronized void stopServer() throws IOException{
//        this.exit = true;
//        if (this.serverSocket != null ) {
//            this.serverSocket.close();
//        }
//    }
//
//    public static BlockingQueue<MessageData> getFila1() {
//        return fila1;
//    }
//
//    public static BlockingQueue<Message> getFila2() {
//        return fila2;
//    }
//
//    public static BlockingQueue<MessageData> getFila3() {
//        return fila3;
//    }
//
//
//
//    // --ClientHandler
//    private void connect() {
//        try {
//            if (socket.getRemoteSocketAddress() != null) {
//                LOGGER.info("Cliente " + socket.getRemoteSocketAddress() + " conectado ao servidor...");
//            } else {
//                LOGGER.info("Cliente " + socket.toString() + " conectado ao servidor...");
//            }
//
//            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
//            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
//
//            LOGGER.info("Streams preparados para cliente " + socket.getRemoteSocketAddress());
//
//            this.receiveMessage();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            this.disconnect();
//        }
//    }
//
//    private void receiveMessage() throws Exception {
//        LOGGER.info("Start receive message");
//        while (!exit && !Thread.interrupted()) {
//            // get data from client
//            Message message = null;
//            try {
//                message = (Message) objectInputStream.readObject();
//            } catch (EOFException e) {
//                this.disconnect();
//            } catch (Exception e) {
//                LOGGER.info(e.getMessage());
//                this.disconnect();
//                continue;
//            }
//
//            LOGGER.info("Mensagem obtida: " + message);
//
//            if (message == null) {
//                continue;
//            }
//
//            if (message.getLastOption() < 1 || message.getLastOption() > 5) {
//                LOGGER.info(StringsConstants.ERR_INVALID_OPTION.toString());
//                this.sendAnswer(new Message(StringsConstants.ERR_INVALID_OPTION.toString()));
//                this.disconnect();
//                continue;
//            }
//
//            if (message.getLastOption() == 5) {
//                this.disconnect();
//                this.sendAnswer(message);
//                continue;
//            }
//
//            this.messageData = new MessageData(message, this.answerQueue);
//
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            executor.submit(new FirstQueueThread(this.messageData));
//
//            this.sendAnswer(this.answerQueue.take());
//        }
//    }
//
//    private void sendAnswer(Message answer) throws IOException, ClassNotFoundException {
//        objectOutputStream.writeObject(answer);
//        objectOutputStream.flush();
//    }
//
//    public synchronized void disconnect() {
//        try {
//            this.exit = true;
//            LOGGER.info("Cliente " + socket.getRemoteSocketAddress() + " desconectado do servidor...");
//        } catch (Exception e) {
//            LOGGER.severe("Error : " + e.getMessage());
//        }
//    }
//
////    @Override
////    public void run() {
////        this.answerQueue = new LinkedBlockingDeque<>();
////        this.connect();
////    }
//
//
//
//
//
//    public static void main(String[] args) {
//        int myId = Integer.parseInt(args[0]);
//        List<Address> addresses = new LinkedList<>();
//
//        for(int i = 1; i <args.length; i+=2)
//        {
//            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
//            addresses.add(address);
//        }
//
//        AtomixBuilder builder = Atomix.builder();
//
//        Atomix atomix = builder.withMemberId("member-"+myId)
//                .withAddress(addresses.get(myId))
//                .withMembershipProvider(BootstrapDiscoveryProvider.builder()
//                        .withNodes( Node.builder()
//                                        .withId("member-0")
//                                        .withAddress(addresses.get(0))
//                                        .build(),
//                                Node.builder()
//                                        .withId("member-1")
//                                        .withAddress(addresses.get(1))
//                                        .build(),
//                                Node.builder()
//                                        .withId("member-2")
//                                        .withAddress(addresses.get(2))
//                                        .build())
//                        .build())
//                .withProfiles(ConsensusProfile.builder().withDataPath("/tmp/member-"+myId).withMembers("member-1", "member-2", "member-3").build())
//                .build();
//
//        atomix.start().join();
//
//        System.out.println("Cluster joined");
//    }
//}
