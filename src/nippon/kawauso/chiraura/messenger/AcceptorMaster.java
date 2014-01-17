/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private final AtomicInteger serialGenerator;

    // 主に後続のために。
    private final ExecutorService executor;

    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final SendQueuePool sendQueuePool;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final ConnectionPool<AcceptedConnection> acceptedConnectionPool;
    private final BoundConnectionPool<Connection> connectionPool;

    private final int sendBufferSize;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final Transceiver transceiver;

    private final KeyPair id;
    private final long version;
    private final PublicKeyManager keyManager;
    private final long keyLifetime;

    private final AtomicReference<InetSocketAddress> self;

    AcceptorMaster(final BlockingQueue<Reporter.Report> reportSink, final BlockingQueue<Socket> acceptedSocketSource, final AtomicInteger serialGenerator,
            final ExecutorService executor, final BlockingQueue<ReceivedMail> receivedMailSink, final SendQueuePool sendQueuePool,
            final BlockingQueue<MessengerReport> messengerReportSink, final ConnectionPool<AcceptedConnection> acceptedConnectionPool,
            final BoundConnectionPool<Connection> connectionPool, final int sendBufferSize, final long connectionTimeout, final long operationTimeout,
            final Transceiver transceiver, final KeyPair id, final long version, final PublicKeyManager keyManager, final long keyLifetime,
            final AtomicReference<InetSocketAddress> self) {
        super(reportSink);

        if (acceptedSocketSource == null) {
            throw new IllegalArgumentException("Null accepted socket source.");
        } else if (serialGenerator == null) {
            throw new IllegalArgumentException("Null serial generator.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (sendQueuePool == null) {
            throw new IllegalArgumentException("Null send queue pool.");
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
        this.receivedMailSink = receivedMailSink;
        this.sendQueuePool = sendQueuePool;
        this.messengerReportSink = messengerReportSink;
        this.acceptedConnectionPool = acceptedConnectionPool;
        this.connectionPool = connectionPool;
        this.sendBufferSize = sendBufferSize;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;
        this.transceiver = transceiver;
        this.id = id;
        this.version = version;
        this.keyManager = keyManager;
        this.keyLifetime = keyLifetime;
        this.self = self;
    }

    @Override
    protected Void subCall() throws InterruptedException, SocketException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final Socket socket = this.acceptedSocketSource.take();
                final int idNumber = this.serialGenerator.getAndIncrement();

                final AcceptedConnection connection = new AcceptedConnection(idNumber, socket);
                this.acceptedConnectionPool.add(connection);

                LOG.log(Level.FINER, "接続番号 {0} で {1} の受け入れ作業を始めます。", new Object[] { Integer.toString(idNumber), socket.getInetAddress() });
                final Acceptor acceptor = new Acceptor(this.messengerReportSink, this.acceptedConnectionPool, this.sendBufferSize, this.connectionTimeout,
                        this.operationTimeout, connection, this.transceiver, this.id, this.version, this.keyManager, this.executor, this.sendQueuePool,
                        this.receivedMailSink, this.connectionPool, this.keyLifetime, this.self);
                this.executor.submit(acceptor);
            } catch (final InterruptedException e) {
                break;
            }
        }

        return null;
    }
}
