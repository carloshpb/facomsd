package server_client.server.database;

import server_client.server.repository.impl.MessageRepositoryMemory;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
    MemoryDB
    - Classe responsável pelas funções relacionadas ao bando de dados (BD)
    e também sobre as funções relacionadas ao controle do logFile.
    Praticamente todos os objetos serão static.
    Quando o Java for carregar o programa, a variável instance será criada (por ser static)
 */

public class MemoryDB {

    /* Padrão Singleton - A classe terá um objeto dentro dela da mesma classe
    * o qual será o único objeto existente no programa de MemoryDB, já que não existirá mais de um BD em memória */
    private static volatile MemoryDB SINGLETON_MEMORYDB = new MemoryDB();

    /* Nosso banco de dados (DB) em memória será esta simples lista
    (objeto ArrayList será criado para esta variável) */
    // private static volatile Map<BigInteger, byte[]> bancoEmMemoria;
//    private static volatile Map<BigInteger, String> bancoEmMemoria;
    private static volatile Map<BigInteger, String> bancoEmMemoria;

    /* Construtor do MemoryDB
    * Ao criar o objeto MemoryDB na variável instance, ele já irá abrir o logFile no programa (openLog)
    * e irá ler o log (readLog), preenchendo o nosso BD (dados) conforme as ações que foram feitas no passado*/
    private MemoryDB() {
        super();
        MemoryDB.restartDB();
    }

    public static synchronized MemoryDB getInstance() {

        if (SINGLETON_MEMORYDB == null) {
            SINGLETON_MEMORYDB = new MemoryDB();
        }

        return SINGLETON_MEMORYDB;
    }

    public static synchronized Map<BigInteger, String> getDatabase() {
        return bancoEmMemoria;
    }

    public static synchronized void restartDB() {
        bancoEmMemoria = new ConcurrentHashMap<>();
        MessageRepositoryMemory.resetAtomicLongIdCreator();
        LogFile.startLog();
    }
}
