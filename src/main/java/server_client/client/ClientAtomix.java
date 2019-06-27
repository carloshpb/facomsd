//package server_client.client;
//
//import io.atomix.catalyst.transport.Address;
//import io.atomix.catalyst.transport.netty.NettyTransport;
//import io.atomix.copycat.client.CopycatClient;
//import io.atomix.copycat.server.StateMachine;
//import server_client.client.view.TerminalView;
//import server_client.model.Message;
//
//import javax.swing.*;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//public class ClientAtomix extends StateMachine {
//
//    public static void main( String[] args ){
//        List<Address> addresses = new LinkedList<>();
//
//        CopycatClient.Builder builder = CopycatClient.builder()
//                .withTransport( NettyTransport.builder()
//                        .withThreads(4)
//                        .build());
//        CopycatClient client = builder.build();
//
//        for(int i = 0; i <args.length;i+=2)
//        {
//            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
//            addresses.add(address);
//        }
//
//        CompletableFuture<CopycatClient> future = client.connect(addresses);
//        future.join();
//
//        final TerminalView terminalView = new TerminalView();
//
//        Message message = terminalView.startReadMessage();
//
//        Timer timer = null;
//        Message answer = null;
//
//        final int FIVE_SECONDS = 5000;
//
//        timer = new Timer(FIVE_SECONDS, event -> {
//            //this.shutdown();
//        });
//
//        timer.start();
//
//        switch (message.getLastOption()) {
//            case 1:
//                answer = client.submit().get();
//                break;
//            case 2:
//                answer = client.submit().get();
//                break;
//            case 3:
//                answer = client.submit().get();
//                break;
//            case 4:
//                answer = client.submit().get();
//                break;
//            default:
//                //this.shutdown();
//                break;
//        }
//
//        if (answer != null) {
//            timer.stop();
//            if (answer.getLastOption() == 5) {
//                System.out.println("Shutting down.");
//                //this.shutdown();
//            } else {
//                terminalView.readMessage(answer);
//                answer = null;
//            }
//        }
//
//        CompletableFuture[] futures = new CompletableFuture[]{
//                client.submit(new AddVertexCommand(1,1, "vertice1")),
//                client.submit(new AddVertexCommand(2,1, "vertice2")),
//                client.submit(new AddVertexCommand(3,1, "vertice3")),
//                client.submit(new AddVertexCommand(4,2, "vertice4")),
//                client.submit(new AddEdgeCommand(1,2, "Edge from 1 to 2")),
//                client.submit(new AddEdgeCommand(1,3, "Edge")),
//                client.submit(new AddEdgeCommand(1,4, "Edge")),
//                client.submit(new AddEdgeCommand(4,3, "Edge"))
//        };
//
//        CompletableFuture.allOf(futures).thenRun(() -> System.out.println("Commands completed!"));
//
//        try {
//            System.out.println("1: " + client.submit(new GetVertexQuery(1)).get());
//            System.out.println("2: " + client.submit(new GetVertexQuery(2)).get());
//        } catch (Exception e)
//        {
//            System.out.println("Commands may have failed.");
//            e.printStackTrace();
//        }
//
//        client.submit(new GetEdgeQuery(1,2)).thenAccept(result -> {
//            System.out.println("1-2: " + result);
//        });
//    }
//}
