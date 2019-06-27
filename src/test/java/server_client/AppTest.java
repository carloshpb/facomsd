package server_client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.*;
import server_client.client.ClientThreadTest;
import server_client.constants.StringsConstants;
import server_client.model.Message;
import server_client.server.database.LogFile;
import server_client.server.database.MemoryDB;
import server_client.server.threads.ServerThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest {

    private final static Logger LOGGER = Logger.getLogger(AppTest.class.getName());

    private static ServerThread serverThread;

    private static ClientThreadTest clientThreadTest;
    private static ExecutorService executor;

    private static volatile BlockingQueue<Message> enviarMensagens = new LinkedBlockingDeque<>();
    private static volatile BlockingQueue<Message> receberRespostas = new LinkedBlockingDeque<>();

    // -----------------------------------------------------------------------------------------------------------------

    /* STARTER */

    @BeforeAll
    synchronized static void beginServer() {

        serverThread = new ServerThread(8080);

        executor = Executors.newFixedThreadPool(2);

        MemoryDB.getInstance();

        executor.execute(serverThread);

        clientThreadTest = new ClientThreadTest(enviarMensagens, receberRespostas);
        executor.execute(clientThreadTest);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /* TEST STARTERS */

    //@Disabled
    @Test
    @Order(1)
    void crudOK() {
        /* Teste responsável para verificar se o CRUD do server retornar as mensagens corretas,
           caso não tenha ocorrido erros */

        this.crudOKBody(1, enviarMensagens, receberRespostas);
    }

    //@Disabled
    @Test
    @Order(2)
    void crudNOK() {
        /* Teste responsável para verificar se o CRUD do server retornar as mensagens corretas,
           caso tenha ocorrido algum erro */

        this.crudNOKBody(1, enviarMensagens, receberRespostas);
    }

    //@Disabled
    @Test
    @Order(3)
    void stateRecovery() {
        /* Teste responsável para verificar se o ServerAtomix recupera o seu Banco de Dados em memória, através do Log */

        List<Message> listaDeItens = this.stateRecoveryBodyPart1(1, enviarMensagens, receberRespostas);
        this.stopServerAndClients(executor);
        this.restartServerAndClients();
        this.stateRecoveryBodyPart2(1, listaDeItens, enviarMensagens, receberRespostas);
    }

    //@Disabled
    @Test
    @Order(4)
    void executionOrder() {
        /* Teste responsável para verificar se o ServerAtomix está adicionando as mensagens na ordem correta */

        this.executionOrderBody(1, 1000, enviarMensagens, receberRespostas);
    }

    @Disabled
    @Test
    @Order(5)
    void concurrency10Threads() {
        /* Teste responsável de reexecutar todos os testes anteriores, paralelamente, com 10 clientes,
        com chaves diferentes e verificar que os resultados snão se alteram. */

        this.stopServerAndClients(executor);

        List<ClientThreadTest> clients = new ArrayList<>();

        ExecutorService executorClients = Executors.newFixedThreadPool(11);

        this.restartServer(executorClients);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Create and execute client threads
        for (int nThreads = 0; nThreads < 10; nThreads++) {
            clients.add(new ClientThreadTest(new LinkedBlockingDeque<>(), new LinkedBlockingDeque<>()));
            executorClients.submit(clients.get(nThreads));
        }

        // Test 1
        for (int nThreads = 0; nThreads < 10; nThreads++) {
            this.crudOKBody(nThreads + 1,
                    clients.get(nThreads).getNovasMensagensQueue(),
                    clients.get(nThreads).getNovasRespostasQueue());
        }

        // Test 2
        for (int nThreads = 0; nThreads < 10; nThreads++) {
            this.crudNOKBody(nThreads + 1,
                    clients.get(nThreads).getNovasMensagensQueue(),
                    clients.get(nThreads).getNovasRespostasQueue());
        }

        // Test 3
        List<List<Message>> listaDeListaDeItens = new ArrayList<>();
        for (int nThreads = 0; nThreads < 10; nThreads++) {
            listaDeListaDeItens.add(this.stateRecoveryBodyPart1(nThreads * 10,
                    clients.get(nThreads).getNovasMensagensQueue(),
                    clients.get(nThreads).getNovasRespostasQueue()));
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.stopServerAndClients(executor);
        this.stopServerAndClients(executorClients);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorClients = this.restartServerAndClients(executorClients, clients);

        for (int nThreads = 0; nThreads < 10; nThreads++) {
            this.stateRecoveryBodyPart2(nThreads * 10,
                    listaDeListaDeItens.get(nThreads),
                    clients.get(nThreads).getNovasMensagensQueue(),
                    clients.get(nThreads).getNovasRespostasQueue());
        }

        // Test 4
        int numInit = 1;
        int limite = 1000;
        for (int nThreads = 0; nThreads < 10; nThreads++) {

            this.executionOrderBody(numInit, limite,
                    clients.get(nThreads).getNovasMensagensQueue(),
                    clients.get(nThreads).getNovasRespostasQueue());

            numInit = limite + 1;
            limite = numInit + 1000;
        }

        this.stopServerAndClients(executorClients);
    }

    @AfterAll
    private static synchronized void cleanDB() {
        MemoryDB.restartDB();
        LogFile.deleteLogFile();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /* UTIL METHODS */

    private Message sendAndReceiveMessage(Message newMessage, BlockingQueue<Message> enviarMensagens,
                                          BlockingQueue<Message> receberRespostas) {
        try {
            enviarMensagens.put(newMessage);
            LOGGER.info("Mensagem " + newMessage.getMessage() + " enviada.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Message resposta = null;
        try {
            LOGGER.info("Resposta da mensagem " + newMessage.getMessage());
            resposta = receberRespostas.take();
            LOGGER.info("Resposta " + resposta.getMessage() + " recebida.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resposta;
    }

    private synchronized void stopServerAndClients(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private synchronized ExecutorService restartServerAndClients(ExecutorService executorClients,
                                                                 List<ClientThreadTest> listClients) {


        serverThread = new ServerThread(8080);

        executor = Executors.newSingleThreadExecutor();
        executorClients = Executors.newFixedThreadPool(10);

        MemoryDB.getInstance();

        executor.execute(serverThread);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int n = 0; n < 10; n++) {
            listClients.set(n, new ClientThreadTest(new LinkedBlockingDeque<>(), new LinkedBlockingDeque<>()));
            executorClients.execute(listClients.get(0));
        }

        return executorClients;
    }

    private synchronized void restartServerAndClients() {
        // RESTART SERVER & CLIENT
        enviarMensagens = new LinkedBlockingDeque<>();
        receberRespostas = new LinkedBlockingDeque<>();
        beginServer();
    }

    private synchronized void restartServer(ExecutorService executorClients) {
        serverThread = new ServerThread(8080);

//        executorClients = Executors.newSingleThreadExecutor();

        MemoryDB.getInstance();

        executorClients.submit(serverThread);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /* BODY BLOCKS FOR THE TESTS */
    private void crudOKBody(int n, BlockingQueue<Message> enviarMensagens, BlockingQueue<Message> receberRespostas) {
        // CREATE
        Message respostaCreate = this.etapa1CrudOk(n, enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.MESSAGE_CREATION_SUCCESS_ID.toString() + respostaCreate.getId()
                + " -- OK" + n, respostaCreate.getMessage());


        // READ
        Message messageRead = this.etapa2CrudOk(respostaCreate.getId(), enviarMensagens, receberRespostas);
        assertEquals("OK" + n, messageRead.getMessage());


        // UPDATE
        Message respostaUpdate = this.etapa3CrudOk(n, respostaCreate.getId(), enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.MESSAGE_UPDATE_SUCCESS.toString(), respostaUpdate.getMessage());


        // DELETE
        Message respostaDelete = this.etapa4CrudOk(respostaCreate.getId(), enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.MESSAGE_DELETE_SUCCESS.toString(), respostaDelete.getMessage());
    }

    private void crudNOKBody(int n, BlockingQueue<Message> enviarMensagens, BlockingQueue<Message> receberRespostas) {
        this.cleanDBCrudNOK(n, enviarMensagens, receberRespostas);

        // CREATE NEW ITEM I
        Message respostaCreate = this.etapa1And2CrudNOK(n, enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.MESSAGE_CREATION_SUCCESS_ID.toString() + (respostaCreate.getId())
                + " -- NOK" + n, respostaCreate.getMessage());


        // CREATE NEW ITEM EQUALS TO ITEM I
        Message respostaCreateEq = this.etapa1And2CrudNOK(n, enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.ERR_EXISTENT_MESSAGE.toString(), respostaCreateEq.getMessage());


        // READ NON EXISTENT ITEM J
        Message respostaRead = this.etapa3CrudNOK(respostaCreate.getId() + 2, enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.ERR_NON_EXISTENT_ID.toString(), respostaRead.getMessage());


        // UPDATE NON EXISTENT ITEM J
        Message respostaUpdateNOKitemJ = this.etapa4CrudNOK(n, -1, enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.ERR_NON_EXISTENT_ID.toString(), respostaUpdateNOKitemJ.getMessage());


        // READ NEW CREATE-UPDATE ITEM J
        String textoUpdateItemJ = "NOK-OK" + n + "Upgrade";
        Message respostaReadItemJ = this.etapa5CrudNOK(n, enviarMensagens, receberRespostas);
        assertEquals(textoUpdateItemJ, respostaReadItemJ.getMessage());


        // DELETE ITEM J & ITEM I
        Message respostaDelete = this.deleteMessagesDBCrudNOK(respostaReadItemJ.getId(),
                enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.MESSAGE_DELETE_SUCCESS.toString(), respostaDelete.getMessage());
        Message respostaDelete2 = this.deleteMessagesDBCrudNOK(respostaCreate.getId(),
                enviarMensagens, receberRespostas);
        assertEquals(StringsConstants.MESSAGE_DELETE_SUCCESS.toString(), respostaDelete2.getMessage());
    }

    private List<Message> stateRecoveryBodyPart1(int n, BlockingQueue<Message> enviarMensagens,
                                                 BlockingQueue<Message> receberRespostas) {
        // CREATE 5 ITEMS
        List<Message> listaDeItens = this.etapa1StateRecovery(n, enviarMensagens, receberRespostas);

//        // KILL SERVER (AND CLIENT)
//        this.stopServerAndClients(executor);
//
//        // RESTART SERVER & CLIENT
//        this.etapa3StateRecovery();

        return listaDeItens;
    }

    private void stateRecoveryBodyPart2(int n, List<Message> listaDeItens, BlockingQueue<Message> enviarMensagens,
                                        BlockingQueue<Message> receberRespostas) {
        // READ LASTLY CREATED ITEMS
        List<Message> listaDeItensRecuperados = this.etapa4StateRecovery(listaDeItens, n);
        assertEquals(listaDeItens, listaDeItensRecuperados);

        // DELETE ALL ITEMS
        List<Message> deletedItems = this.deleteMessagesDBStateRecovery(listaDeItensRecuperados);
        for (Message msg : deletedItems) {
            assertEquals(StringsConstants.MESSAGE_DELETE_SUCCESS.toString(), msg.getMessage());
        }
    }

    private void executionOrderBody(int numInicial, int quantidade, BlockingQueue<Message> enviarMensagens,
                                    BlockingQueue<Message> receberRespostas) {
        // CREATE (quantidade) ITEMS with (numInicial) values as messages

        List<Message> listMessages = this.etapa1ExecutionOrder(numInicial, quantidade);

        assertEquals(quantidade + 1,
                Integer.parseInt((listMessages.get(listMessages.size() - 1)).getMessage()));

        this.deleteMessagesDBExecutionOrder(listMessages);
    }


    // -----------------------------------------------------------------------------------------------------------------

    /* TEST INTERNAL FUNCTIONS */

    /* CRUDOK */
    private Message etapa1CrudOk(int num, BlockingQueue<Message> enviarMensagens,
                                 BlockingQueue<Message> receberRespostas) {
        // CREATE
        String texto = "OK" + num;
        Message messageCreate = new Message(1, texto);
        Message resposta = this.sendAndReceiveMessage(messageCreate, enviarMensagens, receberRespostas);
        return resposta;
    }

    private Message etapa2CrudOk(long id, BlockingQueue<Message> enviarMensagens,
                                 BlockingQueue<Message> receberRespostas) {
        // READ
        Message messageRead = new Message(2, id);
        Message respostaRead = this.sendAndReceiveMessage(messageRead, enviarMensagens, receberRespostas);
        return respostaRead;
    }

    private Message etapa3CrudOk(int num, long id, BlockingQueue<Message> enviarMensagens,
                                 BlockingQueue<Message> receberRespostas) {
        // UPDATE
        String textoUpdate = "OK" + num + "Upgrade";
        Message messageUpdate = new Message(3, id, textoUpdate);
        Message respostaUpdate = this.sendAndReceiveMessage(messageUpdate, enviarMensagens, receberRespostas);
        return respostaUpdate;
    }

    private Message etapa4CrudOk(long id, BlockingQueue<Message> enviarMensagens,
                                 BlockingQueue<Message> receberRespostas) {
        // DELETE
        Message messageDelete = new Message(4, id);
        Message respostaDelete = this.sendAndReceiveMessage(messageDelete, enviarMensagens, receberRespostas);
        return respostaDelete;
    }




    /* CRUDNOK */
    private void cleanDBCrudNOK(int num, BlockingQueue<Message> enviarMensagens,
                                BlockingQueue<Message> receberRespostas) {
        // DELETE ITEM I IF IT ALREADY EXISTS
        if (MemoryDB.getDatabase().containsValue("NOK" + num)) {
            MemoryDB.getDatabase().values().remove("NOK" + num);
        }
    }

    private Message etapa1And2CrudNOK(int num, BlockingQueue<Message> enviarMensagens,
                                      BlockingQueue<Message> receberRespostas) {
        // 1º - CREATE NEW ITEM I
        // 2º - CREATE NEW ITEM EQUALS TO ITEM I
        String textoItemI = "NOK" + num;
        Message messageCreate = new Message(1, textoItemI);
        Message respostaCreate = this.sendAndReceiveMessage(messageCreate, enviarMensagens, receberRespostas);
        return respostaCreate;
    }

    private Message etapa3CrudNOK(long id, BlockingQueue<Message> enviarMensagens,
                                  BlockingQueue<Message> receberRespostas) {
        // READ NON EXISTENT ITEM J
        Message messageRead = new Message(2, id);
        Message respostaRead = this.sendAndReceiveMessage(messageRead, enviarMensagens, receberRespostas);
        return respostaRead;
    }

    private Message etapa4CrudNOK(int num, long id, BlockingQueue<Message> enviarMensagens,
                                  BlockingQueue<Message> receberRespostas) {
        // UPDATE NON EXISTENT ITEM J
        String textoUpdateItemJ = "NOK" + num + "Upgrade";
        Message messageUpdateNOKitemJ = new Message(3, id, textoUpdateItemJ);
        Message respostaUpdateNOKitemJ = this.sendAndReceiveMessage(messageUpdateNOKitemJ,
                enviarMensagens, receberRespostas);
        return respostaUpdateNOKitemJ;
    }

    private Message etapa5CrudNOK(int num, BlockingQueue<Message> enviarMensagens,
                                  BlockingQueue<Message> receberRespostas) {
        // READ NEW CREATE-UPDATE ITEM J
        String textoItemJ = "NOK-OK" + num;
        String textoUpdateItemJ = "NOK-OK" + num + "Upgrade";
        Message messageCreateItemJ = new Message(1, textoItemJ);
        Message msgCreateAnswer = this.sendAndReceiveMessage(messageCreateItemJ, enviarMensagens, receberRespostas);
        Message messageUpdateItemJ = new Message(3, msgCreateAnswer.getId(), textoUpdateItemJ);
        Message msgUpdateAnswer = this.sendAndReceiveMessage(messageUpdateItemJ, enviarMensagens, receberRespostas);
        Message messageReadItemJ = new Message(2, msgUpdateAnswer.getId());
        Message respostaReadItemJ = this.sendAndReceiveMessage(messageReadItemJ, enviarMensagens, receberRespostas);
        return respostaReadItemJ;
    }

    private Message deleteMessagesDBCrudNOK(long id, BlockingQueue<Message> enviarMensagens,
                                            BlockingQueue<Message> receberRespostas) {
        // DELETE ITEM J & ITEM I
        Message messageDelete = new Message(4, id);
        Message respostaDelete = this.sendAndReceiveMessage(messageDelete, enviarMensagens, receberRespostas);
        return respostaDelete;
    }




    /* StateRecovery */
    private List<Message> etapa1StateRecovery(int num, BlockingQueue<Message> enviarMensagens,
                                              BlockingQueue<Message> receberRespostas) {
        // CREATE 5 ITEMS
        List<Message> listItems = new ArrayList<>();
        for (int i = num; i <= num + 4; i++) {
            Message messageCreate = new Message(1, "Item" + i);
            Message answer = this.sendAndReceiveMessage(messageCreate, enviarMensagens, receberRespostas);
            messageCreate.setId(answer.getId());
            listItems.add(messageCreate);
        }

        return listItems;
    }

    private List<Message> etapa4StateRecovery(List<Message> listDeItens, int num) {
        // READ LASTLY CREATED ITEMS
        List<Message> listaItemsRecuperados = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Message messageCreateRead = new Message(2, listDeItens.get(i).getId());
            Message answer = this.sendAndReceiveMessage(messageCreateRead, enviarMensagens, receberRespostas);
            listaItemsRecuperados.add(answer);
        }

        return listaItemsRecuperados;
    }

    private List<Message> deleteMessagesDBStateRecovery(List<Message> listaDeItens) {
        // DELETE ITEMS
        List<Message> deletedItens = new ArrayList<>();
        for (Message msg : listaDeItens) {
            Message messageDelete = new Message(4, msg.getId());
            Message respostaDelete = this.sendAndReceiveMessage(messageDelete, enviarMensagens, receberRespostas);
            deletedItens.add(respostaDelete);
        }

        return deletedItens;
    }




    /* Execution Order */
    private List<Message> etapa1ExecutionOrder(int numInicial, int quantidade) {
        // CREATE 1000 ITEMS with N values as messages
        List<Message> listOfMessages = new ArrayList<>();

        Message firstMessage = new Message(1, Integer.toString(numInicial));
        firstMessage = this.sendAndReceiveMessage(firstMessage, enviarMensagens, receberRespostas);
        Message msgRead = new Message(2, firstMessage.getId());
        msgRead = this.sendAndReceiveMessage(msgRead, enviarMensagens, receberRespostas);
        listOfMessages.add(msgRead);

        while (Integer.parseInt(msgRead.getMessage()) <= quantidade) {

            Message nextMessage = new Message(1, String.valueOf(Integer.parseInt(msgRead.getMessage()) + 1));
            nextMessage = this.sendAndReceiveMessage(nextMessage, enviarMensagens, receberRespostas);
            msgRead = new Message(2, nextMessage.getId());
            msgRead = this.sendAndReceiveMessage(msgRead, enviarMensagens, receberRespostas);
            listOfMessages.add(msgRead);

        }

        return listOfMessages;
    }

    private void deleteMessagesDBExecutionOrder(List<Message> listOfMessages) {
        // DELETE LAST THOUSAND OBJECTS
        // WORKS ONLY WITH JUNIT, BECAUSE OF ASSERTEQUALS
        for (Message msg : listOfMessages) {
            Message msgDelete = new Message(4, msg.getId());
            assertEquals(StringsConstants.MESSAGE_DELETE_SUCCESS.toString(),
                    this.sendAndReceiveMessage(msgDelete, enviarMensagens, receberRespostas).getMessage());
        }
    }

}