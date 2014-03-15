package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.Duration;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ServerTest {

    private final int port;
    private final BlockingQueue<ServerSocket> serverSocketQueue;
    private final int receiveBufferSize;
    private final BlockingQueue<Socket> acceptedSocketQueue;

    private final ExecutorService executor;

    /**
     * 初期化。
     */
    public ServerTest() {
        this.port = 6666;
        this.serverSocketQueue = new LinkedBlockingQueue<>();
        this.receiveBufferSize = 128 * 1024;
        this.acceptedSocketQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newCachedThreadPool();

        // TestFunctions.testLogging(Level.ALL, Level.OFF);
    }

    /**
     * 後片付け。
     * @throws Exception 異常
     */
    public void after() throws Exception {
        this.executor.shutdownNow();
        final ServerSocket serverSocket = this.serverSocketQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(serverSocket);
        serverSocket.close();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.acceptedSocketQueue.poll());
    }

    /**
     * 正常系。
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        final Server instance = new Server(null, this.acceptedSocketQueue, this.port, this.receiveBufferSize);

        this.executor.submit(instance);

        // 受け付け開始待ち。
        Thread.sleep(100L);

        final Socket socket = new Socket(InetAddress.getLocalHost(), this.port);
        final Socket acceptedSocket = this.acceptedSocketQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(socket.getRemoteSocketAddress(), acceptedSocket.getLocalSocketAddress());
        Assert.assertEquals(acceptedSocket.getRemoteSocketAddress(), socket.getLocalSocketAddress());

        socket.close();
        acceptedSocket.close();
    }

}
