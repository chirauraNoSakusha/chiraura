package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 受け付けた接続を捌く。
 * @author chirauraNoSakusha
 */
final class AcceptorMaster extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(AcceptorMaster.class.getName());

    // 参照。
    // 入出力。
    private final BlockingQueue<Socket> acceptedSocketSource;

    // 主に後続のために。
    private final AtomicInteger serialGenerator;
    private final ExecutorService executor;

    private final boolean portIgnore;
    private final int connectionLimit;

    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final SendQueuePool sendQueuePool;
    private final Limiter<InetSocketAddress> limiter;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final ConnectionPool<AcceptedConnection> acceptedConnectionPool;
    private final ConnectionPool<Connection> connectionPool;

    private final int sendBufferSize;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final Transceiver.Share transceiver;

    private final long version;
    private final long versionGapThreshold;
    private final KeyPair id;
    private final PublicKeyManager keyManager;
    private final long keyLifetime;
    private final AtomicReference<InetSocketAddress> self;

    AcceptorMaster(final BlockingQueue<Reporter.Report> reportSink, final BlockingQueue<Socket> acceptedSocketSource, final AtomicInteger serialGenerator,
            final ExecutorService executor, final boolean portIgnore, final int connectionLimit, final BlockingQueue<ReceivedMail> receivedMailSink,
            final SendQueuePool sendQueuePool, final Limiter<InetSocketAddress> limiter, final BlockingQueue<MessengerReport> messengerReportSink,
            final ConnectionPool<AcceptedConnection> acceptedConnectionPool, final ConnectionPool<Connection> connectionPool, final int sendBufferSize,
            final long connectionTimeout, final long operationTimeout, final Transceiver.Share transceiver, final long version, final long versionGapThreshold,
            final KeyPair id, final PublicKeyManager keyManager, final long keyLifetime, final AtomicReference<InetSocketAddress> self) {
        super(reportSink);

        if (acceptedSocketSource == null) {
            throw new IllegalArgumentException("Null accepted socket source.");
        } else if (serialGenerator == null) {
            throw new IllegalArgumentException("Null serial generator.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (connectionLimit < 0) {
            throw new IllegalArgumentException("Negative connection limit ( " + connectionLimit + " ).");
        } else if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (sendQueuePool == null) {
            throw new IllegalArgumentException("Null send queue pool.");
        } else if (limiter == null) {
            throw new IllegalArgumentException("Null limiter.");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (acceptedConnectionPool == null) {
            throw new IllegalArgumentException("Null accepted connection pool.");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Negative operation timeout ( " + operationTimeout + " ).");
        } else if (transceiver == null) {
            throw new IllegalArgumentException("Null transceiver.");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Invalid version gap threshold ( " + versionGapThreshold + " ).");
        } else if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (keyManager == null) {
            throw new IllegalArgumentException("Null key manager.");
        } else if (keyLifetime < 0) {
            throw new IllegalArgumentException("Invalid key lifetime ( " + keyLifetime + " ).");
        } else if (self == null) {
            throw new IllegalArgumentException("Null self.");
        }

        this.acceptedSocketSource = acceptedSocketSource;
        this.serialGenerator = serialGenerator;
        this.executor = executor;
        this.portIgnore = portIgnore;
        this.connectionLimit = connectionLimit;
        this.receivedMailSink = receivedMailSink;
        this.sendQueuePool = sendQueuePool;
        this.limiter = limiter;
        this.messengerReportSink = messengerReportSink;
        this.acceptedConnectionPool = acceptedConnectionPool;
        this.connectionPool = connectionPool;
        this.sendBufferSize = sendBufferSize;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;
        this.transceiver = transceiver;
        this.version = version;
        this.versionGapThreshold = versionGapThreshold;
        this.id = id;
        this.keyManager = keyManager;
        this.keyLifetime = keyLifetime;
        this.self = self;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final Socket socket = this.acceptedSocketSource.take();
            final int idNumber = this.serialGenerator.getAndIncrement();

            final AcceptedConnection connection = new AcceptedConnection(idNumber, socket);
            this.acceptedConnectionPool.add(connection);

            LOG.log(Level.FINER, "接続番号 {0} で {1} の受け入れ作業を始めます。", new Object[] { Integer.toString(idNumber), socket.getInetAddress() });
            final Acceptor acceptor = new Acceptor(this.portIgnore, this.connectionLimit, this.messengerReportSink, this.acceptedConnectionPool,
                    this.sendBufferSize, this.connectionTimeout, this.operationTimeout, this.transceiver, connection, this.version, this.versionGapThreshold,
                    this.id, this.keyManager, this.self, this.executor, this.sendQueuePool, this.receivedMailSink, this.limiter, this.connectionPool,
                    this.keyLifetime);
            this.executor.submit(acceptor);
        }

        return null;
    }
}
