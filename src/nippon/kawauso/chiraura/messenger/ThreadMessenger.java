/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * 接続毎に専任のスレッドを用いる通信係。
 * @author chirauraNoSakusha
 */
final class ThreadMessenger implements Messenger {

    private static final Logger LOG = Logger.getLogger(ThreadMessenger.class.getName());

    // 参照。
    private final int port;
    private final int receveBufferSize;
    private final int sendBufferSize;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final int messageSizeLimit;

    private final long version;
    private final long versionGapThreshold;
    private final KeyPair id;
    private final long publicKeyLifetime;
    private final long commonKeyLifetime;

    private final boolean portIgnore;
    private final int connectionLimit;

    private final long trafficDuration;
    private final long trafficSizeLimit;
    private final int trafficCountLimit;
    private final long trafficPenalty;

    // 保持。
    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final SendQueuePool sendQueuePool;
    private final BlockingQueue<ConnectRequest> connectRequestQueue;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final ConnectionPool<AcceptedConnection> acceptedConnectionPool;
    private final BoundConnectionPool<ContactingConnection> contactingConnectionPool;
    private final BoundConnectionPool<Connection> connectionPool;

    private final TypeRegistry<Message> registry;

    private final AtomicReference<InetSocketAddress> self;

    ThreadMessenger(final int port, final int receveBufferSize, final int sendBufferSize, final long connectionTimeout, final long operationTimeout,
            final int messageSizeLimit, final long version, final long versionGapThreshold, final KeyPair id, final long publicKeyLifetime,
            final long commonKeyLifetime, final boolean portIgnore, final int connectionLimit, final long trafficDuration, final long trafficSizeLimit,
            final int trafficCountLimit, final long trafficPenalty) {
        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Negative operation timeout ( " + operationTimeout + " ).");
        } else if (messageSizeLimit < 0) {
            throw new IllegalArgumentException("Invalid message size limit ( " + messageSizeLimit + " ).");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Invalid version gap threshold ( " + versionGapThreshold + " ).");
        } else if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (publicKeyLifetime < 0) {
            throw new IllegalArgumentException("Invalid public key lifetime ( " + publicKeyLifetime + " ).");
        } else if (commonKeyLifetime < 0) {
            throw new IllegalArgumentException("Invalid common key lifetime ( " + publicKeyLifetime + " ).");
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

        this.port = port;
        this.receveBufferSize = receveBufferSize;
        this.sendBufferSize = sendBufferSize;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;
        this.messageSizeLimit = messageSizeLimit;
        this.version = version;
        this.versionGapThreshold = versionGapThreshold;
        this.id = id;
        this.publicKeyLifetime = publicKeyLifetime;
        this.commonKeyLifetime = commonKeyLifetime;

        this.portIgnore = portIgnore;
        this.connectionLimit = connectionLimit;

        this.trafficDuration = trafficDuration;
        this.trafficSizeLimit = trafficSizeLimit;
        this.trafficCountLimit = trafficCountLimit;
        this.trafficPenalty = trafficPenalty;

        this.receivedMailSink = new LinkedBlockingQueue<>();
        this.sendQueuePool = new BasicSendQueuePool();
        this.connectRequestQueue = new LinkedBlockingQueue<>();
        this.messengerReportSink = new LinkedBlockingQueue<>();
        this.acceptedConnectionPool = new ConnectionPool<>();
        this.contactingConnectionPool = new BoundConnectionPool<>();
        this.connectionPool = new BoundConnectionPool<>();

        this.registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(this.registry);

        this.self = new AtomicReference<>(null);
    }

    @Override
    public KeyPair getId() {
        return this.id;
    }

    @Override
    public InetSocketAddress getSelf() {
        return this.self.get();
    }

    @Override
    public void send(final InetSocketAddress destination, final int connectionType, final List<Message> mail) {
        if (this.sendQueuePool.put(destination, connectionType, mail)) {
            /*
             * 以下の流れだと通信の試み無しで送信失敗し得る。
             * 1. 送信プロセスが通常活動を止める。
             * 2. このメソッドでキューにメッセージを入れる。
             * 3. 送信プロセスがキューを削除する。
             */
            LOG.log(Level.FINEST, "種別 {0} の {1} への郵便ポストができました。", new Object[] { Integer.toString(connectionType), destination });
            ConcurrentFunctions.completePut(new ConnectRequest(destination, connectionType), this.connectRequestQueue);
        }
    }

    @Override
    public ReceivedMail take() throws InterruptedException {
        return this.receivedMailSink.take();
    }

    @Override
    public ReceivedMail takeIfExists() {
        return this.receivedMailSink.poll();
    }

    @Override
    public boolean containsConnection(final InetSocketAddress destination) {
        if (this.portIgnore) {
            return this.acceptedConnectionPool.contains(destination.getAddress()) ||
                    this.contactingConnectionPool.contains(destination.getAddress()) ||
                    this.connectionPool.contains(destination.getAddress());
        } else {
            return this.contactingConnectionPool.contains(destination) ||
                    this.connectionPool.contains(destination);
        }
    }

    @Override
    public boolean removeConnection(final InetSocketAddress destination) {
        boolean removed = false;

        if (this.portIgnore) {
            for (final AcceptedConnection connection : this.acceptedConnectionPool.get(destination.getAddress())) {
                removed = true;
                connection.close();
                LOG.log(Level.FINEST, "接続 {0} をぶっ殺しました。", connection);
            }
            for (final ContactingConnection connection : this.contactingConnectionPool.get(destination.getAddress())) {
                removed = true;
                connection.close();
                LOG.log(Level.FINEST, "接続中の {0} をぶっ殺しました。", connection);
            }
            for (final Connection connection : this.connectionPool.get(destination.getAddress())) {
                removed = true;
                connection.close();
                LOG.log(Level.FINEST, "受け入れ中の {0} をぶっ殺しました。", connection);
            }
        } else {
            for (final ContactingConnection connection : this.contactingConnectionPool.get(destination)) {
                removed = true;
                connection.close();
                LOG.log(Level.FINEST, "接続中の {0} をぶっ殺しました。", connection);
            }
            for (final Connection connection : this.connectionPool.get(destination)) {
                removed = true;
                connection.close();
                LOG.log(Level.FINEST, "受け入れ中の {0} をぶっ殺しました。", connection);
            }
        }
        return removed;
    }

    @Override
    public void start(final ExecutorService executor) {
        executor.submit(new Boss(executor, this.connectRequestQueue, this.receivedMailSink, this.sendQueuePool, this.messengerReportSink,
                this.acceptedConnectionPool, this.contactingConnectionPool, this.connectionPool, this.port, this.receveBufferSize, this.sendBufferSize,
                this.connectionTimeout, this.operationTimeout, this.messageSizeLimit, this.registry, this.version, this.versionGapThreshold, this.id,
                this.publicKeyLifetime, this.commonKeyLifetime, this.self, this.portIgnore, this.connectionLimit, this.trafficDuration, this.trafficSizeLimit,
                this.trafficCountLimit, this.trafficPenalty));
    }

    @Override
    public MessengerReport takeReport() throws InterruptedException {
        return this.messengerReportSink.take();
    }

    @Override
    public MessengerReport takeReportIfExists() {
        return this.messengerReportSink.poll();
    }

    @Override
    public <T extends Message> void registerMessage(final long typeId, final Class<T> type, final BytesConvertible.Parser<? extends T> parser) {
        this.registry.register(typeId, type, parser);
    }

}
