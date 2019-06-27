//package server_client.server;
//
//import io.atomix.catalyst.transport.Address;
//import io.atomix.catalyst.transport.netty.NettyTransport;
//import io.atomix.copycat.server.CopycatServer;
//import io.atomix.copycat.server.StateMachine;
//import io.atomix.copycat.server.storage.Storage;
//import io.atomix.copycat.server.storage.StorageLevel;
//
//import java.io.File;
//import java.util.LinkedList;
//import java.util.List;
//
//public class ServerAtomix extends StateMachine {
//    class Pair<A,B>{
//        A a;
//        B b;
//
//        public Pair(A na, B nb)
//        {
//            a = na;
//            b = nb;
//        }
//
//        @Override
//        public String toString()
//        {
//            return "("+ a + "," + b + ")";
//        }
//
//        @Override
//        public boolean equals(Object arg0) {
//            if (arg0 instanceof Pair)
//            {
//                @SuppressWarnings("rawtypes")
//                Pair other = (Pair) arg0;
//                return a.equals(other.a) && b.equals(other.b);
//            }
//            else
//            {
//                return false;
//            }
//        }
//
//        @Override
//        public int hashCode() {
//            return a.hashCode() + b.hashCode();
//        }
//    }
//
//    private Map<Integer,Vertex> vertices = new HashMap<>();
//    private Map<Pair<Integer,Integer>,Edge> edges = new HashMap<>();
//
//    public Boolean AddEdge(Commit<AddEdgeCommand> commit){
//        try{
//            AddEdgeCommand aec = commit.operation();
//            Pair<Integer,Integer> p = new Pair<>(aec.id, aec.id2);
//            Edge e = new Edge(aec.id, aec.id2, aec.desc);
//
//            System.out.println("Adding " + e);
//
//            return edges.putIfAbsent(p, e) == null;
//        }finally{
//            commit.close();
//        }
//    }
//
//    public Boolean AddVertex(Commit<AddVertexCommand> commit){
//        try{
//            AddVertexCommand avc = commit.operation();
//            Vertex v = new Vertex(avc.id, avc.desc);
//
//            System.out.println("Adding " + v);
//
//            return vertices.putIfAbsent(avc.id, v) == null;
//        }finally{
//            commit.close();
//        }
//    }
//
//    public Edge GetEdge(Commit<GetEdgeQuery> commit){
//        try{
//            GetEdgeQuery geq = commit.operation();
//            Pair<Integer,Integer> p = new Pair<>(geq.id, geq.id2);
//
//            System.out.println("Vertices:" + vertices);
//            System.out.println("Edges:" + edges);
//
//            Edge result = edges.get(p);
//            System.out.println("GetEdge " + p + " = " + result);
//            return result;
//        }finally{
//            commit.close();
//        }
//    }
//
//    public Vertex GetVertex(Commit<GetVertexQuery> commit){
//        try{
//            GetVertexQuery gvq = commit.operation();
//            System.out.println("Vertices:" + vertices);
//            System.out.println("Edges:" + edges);
//
//            Vertex result = vertices.get(gvq.id);
//            System.out.println("GetVertex " + gvq.id + " = " + result);
//            return result;
//        }finally{
//            commit.close();
//        }
//    }
//
//
//
//    public static void main( String[] args ){
//        int myId = Integer.parseInt(args[0]);
//        List<Address> addresses = new LinkedList<>();
//
//        for(int i = 1; i <args.length; i+=2)
//        {
//            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
//            addresses.add(address);
//        }
//
//        CopycatServer.Builder builder = CopycatServer.builder(addresses.get(myId))
//                .withStateMachine(ServerAtomix::new)
//                .withTransport( NettyTransport.builder()
//                        .withThreads(4)
//                        .build())
//                .withStorage( Storage.builder()
//                        .withDirectory(new File("logs_"+myId)) //Must be unique
//                        .withStorageLevel(StorageLevel.DISK)
//                        .build());
//        CopycatServer server = builder.build();
//
//        if(myId == 0)
//        {
//            server.bootstrap().join();
//        }
//        else
//        {
//            server.join(addresses).join();
//        }
//    }
//}
