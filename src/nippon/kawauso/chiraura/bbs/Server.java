package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * サーバ。
 * @author chirauraNoSakusha
 */
final class Server extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    // 参照。
    private final int port;
    private final ConnectionPool connectionPool;
    private final ExecutorService executor;
    private final long connectionTimeout;
    private final ResponseMaker responseMaker;
    private final long internalTimeout;

    private final Limiter<InetSocketAddress> limiter;

    // 保持。
    private final ServerSocket serverSocket;

    Server(final BlockingQueue<? super Reporter.Report> reportSink, final int port, final ConnectionPool connectionPool, final ExecutorService executor,
            final long connectionTimeout, final ResponseMaker responseMaker, final long internalTimeout, final Limiter<InetSocketAddress> limiter)
            throws IOException {
        super(reportSink);

        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (responseMaker == null) {
            throw new IllegalArgumentException("Null response maker.");
        } else if (internalTimeout < 0) {
            throw new IllegalArgumentException("Negative internal timeout ( " + internalTimeout + " ).");
        } else if (limiter == null) {
            throw new IllegalArgumentException("Null limiter.");
        }

        this.port = port;
        this.connectionPool = connectionPool;
        this.executor = executor;
        this.connectionTimeout = connectionTimeout;
        this.responseMaker = responseMaker;
        this.internalTimeout = internalTimeout;

        this.limiter = limiter;

        this.serverSocket = new ServerSocket();
    }

    ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    private void startCommunicator(final Socket socket) {
        final Connection connection = new Connection(socket);
        this.connectionPool.add(connection);
        try {
            // 受信の時間制限を設定。
            socket.setSoTimeout((int) this.connectionTimeout);

            final Communicator communicator = new Communicator(connection, this.connectionPool, this.responseMaker, this.internalTimeout, this.limiter);
            this.executor.submit(communicator);
            LOG.log(Level.FINER, "{0} との通信を始めます。", socket);
        } catch (final IOException | RuntimeException e) {
            connection.close();
            this.connectionPool.remove(connection);
        }
    }

    @Override
    protected Void subCall() throws IOException {
        try {
            if (Global.isDebug()) {
                this.serverSocket.setReuseAddress(true);
            }

            LOG.log(Level.FINER, "{0} 番ポートで待機を始めます。", Integer.toString(this.port));
            this.serverSocket.bind(new InetSocketAddress(this.port));

            while (!Thread.currentThread().isInterrupted()) {
                final Socket socket;
                try {
                    socket = this.serverSocket.accept();
                } catch (final IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        // 別プロセスが serverSocket を閉じて終了を教えてくれた。
                        break;
                    } else {
                        throw e;
                    }
                }

                startCommunicator(socket);
            }
        } finally {
            this.serverSocket.close();
        }

        return null;
    }

}
