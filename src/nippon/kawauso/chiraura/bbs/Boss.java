/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.connection.PortIgnoringConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.process.Chief;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.lib.process.Reporter.Report;

/**
 * 偉い人。
 * @author chirauraNoSakusha
 */
final class Boss extends Chief {

    private static final Logger LOG = Logger.getLogger(Boss.class.getName());

    // 参照。
    private final int port;
    private final long connectionTimeout;
    private final long internalTimeout;
    private final ExecutorService executor;

    // 保持。
    private final ConnectionPool connectionPool;
    private final ResponseMaker responseMaker;

    private ServerSocket serverSocket;

    private final Limiter<InetSocketAddress> limiter;

    Boss(final int port, final long connectionTimeout, final long internalTimeout, final ClosetWrapper closet, final Menu menu, final ExecutorService executor,
            final long trafficDuration, final int trafficCountLimit) {
        super(new LinkedBlockingQueue<Reporter.Report>());

        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (internalTimeout < 0) {
            throw new IllegalArgumentException("Negative internal timeout ( " + internalTimeout + " ).");
        } else if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        } else if (menu == null) {
            throw new IllegalArgumentException("Null menu.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (trafficDuration < 0) {
            throw new IllegalArgumentException("Negative traffic duration ( " + trafficDuration + " ).");
        } else if (trafficCountLimit < 0) {
            throw new IllegalArgumentException("Negative traffic count limit ( " + trafficCountLimit + " ).");
        }

        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.internalTimeout = internalTimeout;
        this.executor = executor;

        this.connectionPool = new ConnectionPool();
        this.responseMaker = new ResponseMaker(closet, menu, port);
        this.serverSocket = null;

        this.limiter = new PortIgnoringConstantTrafficLimiter(trafficDuration, Long.MAX_VALUE, trafficCountLimit, 0L);
    }

    private Server newServer() throws IOException {
        return new Server(getReportQueue(), this.port, this.connectionPool, this.executor, this.connectionTimeout, this.responseMaker, this.internalTimeout,
                this.limiter);
    }

    @Override
    protected void before() {
        try {
            final Server server = newServer();
            this.executor.submit(server);
            this.serverSocket = server.getServerSocket();
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.SEVERE, "サーバを作成できませんでした。");
        }
    }

    @Override
    protected void reaction(final Report report) throws Exception {
        boolean done = true;
        if (report.getSource() == Server.class) {
            if (this.serverSocket != null) {
                try {
                    this.serverSocket.close();
                } catch (final IOException ignore) {
                }
                this.serverSocket = null;
            }
            if (report.getCause() instanceof BindException) {
                LOG.log(Level.WARNING, "異常が発生しました", report.getCause());
                LOG.log(Level.SEVERE, "接続の待機を始められませんでした。");
            } else {
                try {
                    final Server server = newServer();
                    this.executor.submit(server);
                    this.serverSocket = server.getServerSocket();
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", report.getCause());
                    LOG.log(Level.SEVERE, "サーバを再作成できませんでした。");
                }
            }
        } else {
            done = false;
        }

        if (done) {
            LOG.log(Level.WARNING, "異常が発生しました", report.getCause());
            LOG.log(Level.INFO, "{0} を再起動しました。", report.getSource().getName());
        } else {
            LOG.log(Level.WARNING, "知らない奴から報告 ( {0} ) が来ました。", report);
        }
    }

    @Override
    public void after() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (final IOException ignored) {
            }
        }
        for (final Connection connection : this.connectionPool.getAll()) {
            connection.close();
        }
    }

}
