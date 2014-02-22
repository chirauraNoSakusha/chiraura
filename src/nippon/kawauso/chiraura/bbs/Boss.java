/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.connection.PortFunctions;
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

    Boss(final int port, final long connectionTimeout, final long internalTimeout, final ClosetWrapper closet, final Menu menu, final ExecutorService executor) {
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
        }

        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.internalTimeout = internalTimeout;
        this.executor = executor;

        this.connectionPool = new ConnectionPool();
        this.responseMaker = new ResponseMaker(closet, menu, port);
        this.serverSocket = null;
    }

    private Server newServer() throws IOException {
        return new Server(getReportQueue(), this.port, this.connectionPool, this.executor, this.connectionTimeout, this.responseMaker, this.internalTimeout);
    }

    @Override
    protected void before() {
        try {
            final Server server = newServer();
            this.executor.submit(server);
            this.serverSocket = server.getServerSocket();
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "サーバを作成できませんでした", e);
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
                LOG.log(Level.SEVERE, "接続の待機を始められませんでした", report.getCause());
            } else {
                try {
                    final Server server = newServer();
                    this.executor.submit(server);
                    this.serverSocket = server.getServerSocket();
                } catch (final IOException e) {
                    LOG.log(Level.SEVERE, "サーバを再作成できませんでした", e);
                }
            }
        } else {
            done = false;
        }

        if (done) {
            LOG.log(Level.WARNING, report.getSource().getName() + " を再起動しました", report.getCause());
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
