/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.connection.BasicConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.connection.PortIgnoringConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.connection.TrafficLimiter;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.process.Chief;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class Boss extends Chief {

    private static final Logger LOG = Logger.getLogger(Boss.class.getName());

    // 参照。
    private final ExecutorService executor;

    private final BlockingQueue<ConnectRequest> connectRequestQueue;

    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final SendQueuePool sendQueuePool;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final ConnectionPool<AcceptedConnection> acceptedConnectionPool;
    private final ConnectionPool<ContactingConnection> contactingConnectionPool;
    private final ConnectionPool<Connection> connectionPool;

    private final boolean portIgnore;
    private final int connectionLimit;

    private final int port;
    private final int receiveBufferSize;
    private final int sendBufferSize;
    private final long connectionTimeout;
    private final long operationTimeout;

    private final long version;
    private final long versionGapThreshold;
    private final KeyPair id;
    private final long commonKeyLifetime;
    private final AtomicReference<InetSocketAddress> self;

    // 保持。
    private final AtomicInteger connectionSerialGenerator;

    private final BlockingQueue<Socket> acceptedSocketQueue;

    private final TransceiverShare transceiver;

    private final TrafficLimiter limiter;

    private final PublicKeyManager keyManager;

    private ServerSocket serverSocket;

    Boss(final ExecutorService executor, final BlockingQueue<ConnectRequest> connectRequestQueue, final BlockingQueue<ReceivedMail> receivedMailSink,
            final SendQueuePool sendQueuePool, final BlockingQueue<MessengerReport> messengerReportSink,
            final ConnectionPool<AcceptedConnection> acceptedConnectionPool, final ConnectionPool<ContactingConnection> contactingConnectionPool,
            final ConnectionPool<Connection> connectionPool, final int port, final int receiveBufferSize, final int sendBufferSize,
            final long connectionTimeout, final long operationTimeout, final int messageSizeLimit, final TypeRegistry<Message> registry, final long version,
            final long versionGapThreshold, final KeyPair id, final long publicKeyLifetime, final long commonKeyLifetime,
            final AtomicReference<InetSocketAddress> self, final boolean portIgnore, final int connectionLimit, final long trafficDuration,
            final long trafficSizeLimit, final int trafficCountLimit, final long trafficPenalty) {
        super(new LinkedBlockingQueue<Reporter.Report>());

        if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (connectRequestQueue == null) {
            throw new IllegalArgumentException("Null connect request queue.");
        } else if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (sendQueuePool == null) {
            throw new IllegalArgumentException("Null send queue pool.");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (acceptedConnectionPool == null) {
            throw new IllegalArgumentException("Null accepted connection pool.");
        } else if (contactingConnectionPool == null) {
            throw new IllegalArgumentException("Null contacting connection pool.");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Negative operation timeout ( " + operationTimeout + " ).");
        } else if (messageSizeLimit < 0) {
            throw new IllegalArgumentException("Negative message size limit ( " + messageSizeLimit + " ).");
        } else if (registry == null) {
            throw new IllegalArgumentException("Null registry.");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Invalid version gap threshold ( " + versionGapThreshold + " ).");
        } else if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (publicKeyLifetime < 0) {
            throw new IllegalArgumentException("Negative public key lifetime ( " + publicKeyLifetime + " ).");
        } else if (commonKeyLifetime < 0) {
            throw new IllegalArgumentException("Negative common key lifetime ( " + commonKeyLifetime + " ).");
        } else if (self == null) {
            throw new IllegalArgumentException("Null self.");
        } else if (connectionLimit < 0) {
            throw new IllegalArgumentException("Negative connection limit ( " + connectionLimit + " ).");
        } else if (trafficDuration < 0) {
            throw new IllegalArgumentException("Negative traffic duration ( " + trafficDuration + " ).");
        } else if (trafficSizeLimit < 0) {
            throw new IllegalArgumentException("Negative traffic size limit ( " + trafficSizeLimit + " ).");
        } else if (trafficCountLimit < 0) {
            throw new IllegalArgumentException("Negative traffic count limit ( " + trafficCountLimit + " ).");
        } else if (trafficPenalty < 0) {
            throw new IllegalArgumentException("Negative traffic penalty ( " + trafficPenalty + " ).");
        }

        this.executor = executor;

        this.connectRequestQueue = connectRequestQueue;

        this.receivedMailSink = receivedMailSink;
        this.sendQueuePool = sendQueuePool;
        this.messengerReportSink = messengerReportSink;
        this.acceptedConnectionPool = acceptedConnectionPool;
        this.contactingConnectionPool = contactingConnectionPool;
        this.connectionPool = connectionPool;

        this.port = port;
        this.receiveBufferSize = receiveBufferSize;
        this.sendBufferSize = sendBufferSize;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;

        this.version = version;
        this.versionGapThreshold = versionGapThreshold;
        this.id = id;
        this.commonKeyLifetime = commonKeyLifetime;
        this.self = self;

        this.portIgnore = portIgnore;
        this.connectionLimit = connectionLimit;

        this.connectionSerialGenerator = new AtomicInteger();
        this.acceptedSocketQueue = new LinkedBlockingQueue<>();
        this.transceiver = new TransceiverShare(messageSizeLimit, registry);
        if (portIgnore) {
            this.limiter = new PortIgnoringConstantTrafficLimiter(trafficDuration, trafficSizeLimit, trafficCountLimit, trafficPenalty);
        } else {
            this.limiter = new BasicConstantTrafficLimiter(trafficDuration, trafficSizeLimit, trafficCountLimit, trafficPenalty);
        }
        this.keyManager = new PublicKeyManager(publicKeyLifetime);

        this.serverSocket = null;
    }

    private Server newServer() throws IOException {
        return new Server(getReportQueue(), this.acceptedSocketQueue, this.port, this.receiveBufferSize);
    }

    private AcceptorMaster newAcceptorMaster() {
        return new AcceptorMaster(getReportQueue(), this.acceptedSocketQueue, this.connectionSerialGenerator, this.executor, this.portIgnore,
                this.connectionLimit, this.receivedMailSink, this.sendQueuePool, this.limiter, this.messengerReportSink, this.acceptedConnectionPool,
                this.connectionPool, this.sendBufferSize, this.connectionTimeout, this.operationTimeout, this.transceiver, this.version,
                this.versionGapThreshold, this.id, this.keyManager, this.commonKeyLifetime, this.self);
    }

    private ContactorMaster newContactorMaster() {
        return new ContactorMaster(getReportQueue(), this.connectRequestQueue, this.connectionSerialGenerator, this.executor, this.receivedMailSink,
                this.sendQueuePool, this.limiter, this.messengerReportSink, this.contactingConnectionPool, this.connectionPool, this.receiveBufferSize,
                this.sendBufferSize, this.connectionTimeout, this.operationTimeout, this.transceiver, this.version, this.versionGapThreshold, this.port,
                this.id, this.keyManager, this.commonKeyLifetime, this.self);
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
            ConcurrentFunctions.completePut(new ServerError(this.port, e), this.messengerReportSink);
        }
        this.executor.submit(newAcceptorMaster());
        this.executor.submit(newContactorMaster());
    }

    @Override
    protected void reaction(final Reporter.Report report) {
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
                ConcurrentFunctions.completePut(new ServerError(this.port, report.getCause()), this.messengerReportSink);
                return;
            } else {
                try {
                    final Server server = newServer();
                    this.executor.submit(server);
                    this.serverSocket = server.getServerSocket();
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", report.getCause());
                    LOG.log(Level.SEVERE, "サーバを再作成できませんでした。");
                    ConcurrentFunctions.completePut(new ServerError(this.port, e), this.messengerReportSink);
                    return;
                }
            }
        } else if (report.getSource() == AcceptorMaster.class) {
            this.executor.submit(newAcceptorMaster());
        } else if (report.getSource() == ContactorMaster.class) {
            this.executor.submit(newContactorMaster());
        } else {
            done = false;
        }

        if (done) {
            LOG.log(Level.WARNING, "異常が発生しました", report.getCause());
            LOG.log(Level.WARNING, report.getSource().getName() + " を再起動しました。");
        } else {
            LOG.log(Level.WARNING, "知らない奴から報告 {0} が来ました。", report);
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
        for (final ContactingConnection connection : this.contactingConnectionPool.getAll()) {
            connection.close();
        }
        for (final AcceptedConnection connection : this.acceptedConnectionPool.getAll()) {
            connection.close();
        }
        for (final Connection connection : this.connectionPool.getAll()) {
            connection.close();
        }
    }

}
