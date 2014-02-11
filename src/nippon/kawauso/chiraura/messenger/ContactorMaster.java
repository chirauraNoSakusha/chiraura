/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 自分から他の個体への接続を開かせる。
 * @author chirauraNoSakusha
 */
final class ContactorMaster extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(ContactorMaster.class.getName());

    private final BlockingQueue<ConnectRequest> connectRequestSource;
    private final AtomicInteger serialGenerator;
    private final BoundConnectionPool<ContactingConnection> contactingConnectionPool;
    private final ExecutorService executor;

    // 子プロセスが必要とする参照。
    private final Transceiver transceiver;
    private final int port;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final int receiveBufferSize;
    private final int sendBufferSize;
    private final BoundConnectionPool<Connection> connectionPool;
    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final SendQueuePool sendQueuePool;
    private final KeyPair id;
    private final PublicKeyManager keyManager;

    private final long version;
    private final long versionGapThreshold;
    private final long keyLifetime;

    private final AtomicReference<InetSocketAddress> self;

    ContactorMaster(final BlockingQueue<Reporter.Report> reportSink, final BlockingQueue<ConnectRequest> connectRequestSource,
            final AtomicInteger serialGenerator, final BoundConnectionPool<ContactingConnection> contactingConnectionPool, final ExecutorService executor,
            final Transceiver transceiver, final int port, final long connectionTimeout, final long operationTimeout, final int receiveBufferSize,
            final int sendBufferSize, final BoundConnectionPool<Connection> connectionPool, final BlockingQueue<ReceivedMail> receivedMailSink,
            final BlockingQueue<MessengerReport> messengerReportSink, final SendQueuePool sendQueuePool, final KeyPair id, final PublicKeyManager keyManager,
            final long version, final long versionGapThreshold, final long keyLifetime, final AtomicReference<InetSocketAddress> self) {
        super(reportSink);

        if (connectRequestSource == null) {
            throw new IllegalArgumentException("Null connect request source.");
        } else if (serialGenerator == null) {
            throw new IllegalArgumentException("Null serial generator.");
        } else if (contactingConnectionPool == null) {
            throw new IllegalArgumentException("Null contacting connection pool.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (transceiver == null) {
            throw new IllegalArgumentException("Null transceiver.");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Negative operation timeout ( " + operationTimeout + " ).");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (sendQueuePool == null) {
            throw new IllegalArgumentException("Null send queue pool.");
        } else if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (keyManager == null) {
            throw new IllegalArgumentException("Null key manager.");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Invalid version gap threshold ( " + versionGapThreshold + " ).");
        } else if (keyLifetime < 0) {
            throw new IllegalArgumentException("Invalid key lifetime ( " + keyLifetime + " ).");
        } else if (self == null) {
            throw new IllegalArgumentException("Null self.");
        }

        this.connectRequestSource = connectRequestSource;
        this.serialGenerator = serialGenerator;
        this.contactingConnectionPool = contactingConnectionPool;
        this.executor = executor;
        this.transceiver = transceiver;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;
        this.receiveBufferSize = receiveBufferSize;
        this.sendBufferSize = sendBufferSize;
        this.connectionPool = connectionPool;
        this.receivedMailSink = receivedMailSink;
        this.messengerReportSink = messengerReportSink;
        this.sendQueuePool = sendQueuePool;
        this.id = id;
        this.keyManager = keyManager;
        this.version = version;
        this.versionGapThreshold = versionGapThreshold;
        this.keyLifetime = keyLifetime;
        this.self = self;
    }

    @Override
    protected Void subCall() throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final ConnectRequest request = this.connectRequestSource.take();
                final int idNumber = this.serialGenerator.getAndIncrement();

                final ContactingConnection connection = new ContactingConnection(idNumber, request.getDestination(), request.getConnectionType());
                this.contactingConnectionPool.add(connection);
                LOG.log(Level.FINER, "接続番号 {0} 種別 {1} で {2} へ接続します。",
                        new Object[] { idNumber, Integer.toString(request.getConnectionType()), request.getDestination() });
                final Contactor contactor = new Contactor(this.messengerReportSink, this.contactingConnectionPool, this.receiveBufferSize, this.sendBufferSize,
                        this.connectionTimeout, this.operationTimeout, connection, this.transceiver, this.port, this.id, this.version,
                        this.versionGapThreshold,
                        this.keyManager, this.executor, this.sendQueuePool, this.receivedMailSink, this.connectionPool, this.keyLifetime, this.self);
                this.executor.submit(contactor);
            } catch (final InterruptedException e) {
                break;
            }
        }

        return null;
    }

}
