package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.lib.connection.BasicConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.connection.PortIgnoringConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.process.Chief;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * お山の大将。偉い。
 * @author chirauraNoSakusha
 */
final class Boss extends Chief {

    private static final Logger LOG = Logger.getLogger(Boss.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final SessionManager sessionManager;
    private final long maintenanceInterval;
    private final long sleepTime;
    private final long backupInterval;
    private final long operationTimeout;
    private final long versionGapThreshold;
    private final ExecutorService executor;
    private final BlockingQueue<Operation> operationQueue;
    private final BlockingQueue<ClosetReport> closetReportSink;
    private final BlockingQueue<OutlawReport> outlawReportQueue;
    private final DriverSet drivers;

    private final Map<AddressedPeer, BackupperMaster.BackupperUnit> backupeerPool;
    private final Limiter<InetSocketAddress> outlawLimiter;
    private final ConcurrentMap<InetSocketAddress, Boolean> outlawRemovers;

    Boss(final NetworkWrapper network, final SessionManager sessionManager, final long maintenanceInterval, final long sleepTime, final long backupInterval,
            final long operationTimeout, final long versionGapThreshold, final ExecutorService executor, final BlockingQueue<Operation> operationQueue,
            final BlockingQueue<ClosetReport> closetReportSink, final DriverSet drivers, final BlockingQueue<OutlawReport> outlawReportQueue,
            final boolean portIgnore, final long outlawDuration, final int outlawCountLimit) {
        super(new LinkedBlockingQueue<Reporter.Report>());

        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (maintenanceInterval < 0) {
            throw new IllegalArgumentException("Invalid maintenance interval ( " + maintenanceInterval + " ).");
        } else if (sleepTime < 0) {
            throw new IllegalArgumentException("Invalid sleep time ( " + sleepTime + " ).");
        } else if (backupInterval < 0) {
            throw new IllegalArgumentException("Invalid backup interval ( " + backupInterval + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Invalid operation timeout ( " + operationTimeout + " ).");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Too small version gap threshold ( " + versionGapThreshold + " ).");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (operationQueue == null) {
            throw new IllegalArgumentException("Null operation queue.");
        } else if (closetReportSink == null) {
            throw new IllegalArgumentException("Null closet report sink.");
        } else if (outlawReportQueue == null) {
            throw new IllegalArgumentException("Null outlaw report queue.");
        } else if (drivers == null) {
            throw new IllegalArgumentException("Null drivers.");
        } else if (outlawDuration < 0) {
            throw new IllegalArgumentException("Negative outlaw duration ( " + outlawDuration + " ).");
        } else if (outlawCountLimit < 0) {
            throw new IllegalArgumentException("Negative outlaw count limit ( " + outlawCountLimit + " ).");
        }

        this.network = network;
        this.sessionManager = sessionManager;
        this.maintenanceInterval = maintenanceInterval;
        this.sleepTime = sleepTime;
        this.backupInterval = backupInterval;
        this.operationTimeout = operationTimeout;
        this.versionGapThreshold = versionGapThreshold;
        this.executor = executor;
        this.operationQueue = operationQueue;
        this.closetReportSink = closetReportSink;
        this.outlawReportQueue = outlawReportQueue;
        this.drivers = drivers;

        this.backupeerPool = new HashMap<>();
        if (portIgnore) {
            this.outlawLimiter = new PortIgnoringConstantTrafficLimiter(outlawDuration, Long.MAX_VALUE, outlawCountLimit, 0);
        } else {
            this.outlawLimiter = new BasicConstantTrafficLimiter(outlawDuration, Long.MAX_VALUE, outlawCountLimit, 0);
        }
        this.outlawRemovers = new ConcurrentHashMap<>();
    }

    private MailReader newMailReader() {
        return new MailReader(getReportQueue(), this.network, this.sessionManager, this.operationTimeout, this.drivers, this.drivers);
    }

    private MessengerMonitor newMessengerMonitor() {
        return new MessengerMonitor(getReportQueue(), this.network, this.closetReportSink, this.versionGapThreshold, this.drivers, this.outlawReportQueue);
    }

    private NetworkManager newNetworkManager() {
        return new NetworkManager(getReportQueue(), this.network, this.operationTimeout, this.drivers.getPeerAccessNonBlocking(),
                this.drivers.getAddressAccessNonBlocking());
    }

    private Worker newWorker() {
        return new Worker(getReportQueue(), this.operationQueue, this.operationTimeout, this.drivers);
    }

    private Lonely newLonely() {
        return new Lonely(getReportQueue(), this.network, this.executor, this.maintenanceInterval, this.sleepTime, this.operationTimeout,
                this.drivers.getFirstAccessSelect());
    }

    private Unpartitioner newUnpartitioner() {
        return new Unpartitioner(getReportQueue(), this.network, this.sleepTime, this.operationTimeout, this.drivers.getFirstAccessSelect());
    }

    private BackupperMaster newBackupperMaster() {
        return new BackupperMaster(getReportQueue(), this.network, this.maintenanceInterval, this.backupInterval, this.operationTimeout, this.backupeerPool,
                this.executor, this.drivers);
    }

    private Blacklister newBlacklister() {
        return new Blacklister(getReportQueue(), this.outlawReportQueue, this.outlawLimiter, this.network, this.outlawRemovers, this.executor);
    }

    @Override
    protected void before() {
        this.executor.submit(newMailReader());
        this.executor.submit(newMessengerMonitor());
        this.executor.submit(newNetworkManager());
        this.executor.submit(newWorker());
        this.executor.submit(newLonely());
        this.executor.submit(newUnpartitioner());
        this.executor.submit(newBackupperMaster());
        this.executor.submit(newBlacklister());
    }

    @Override
    protected void reaction(final Reporter.Report report) {
        try {
            boolean done = true;
            if (report.getSource() == MailReader.class) {
                this.executor.submit(newMailReader());
            } else if (report.getSource() == MessengerMonitor.class) {
                this.executor.submit(newMessengerMonitor());
            } else if (report.getSource() == NetworkManager.class) {
                this.executor.submit(newNetworkManager());
            } else if (report.getSource() == Worker.class) {
                this.executor.submit(newWorker());
            } else if (report.getSource() == Lonely.class) {
                this.executor.submit(newLonely());
            } else if (report.getSource() == Unpartitioner.class) {
                this.executor.submit(newUnpartitioner());
            } else if (report.getSource() == BackupperMaster.class) {
                this.executor.submit(newBackupperMaster());
            } else if (report.getSource() == Blacklister.class) {
                this.executor.submit(newBlacklister());
            } else {
                done = false;
            }

            if (done) {
                LOG.log(Level.WARNING, "異常が発生しました", report.getCause());
                LOG.log(Level.INFO, "{0} を再起動しました。", report.getSource().getName());
            } else {
                LOG.log(Level.WARNING, "知らない報告 {0} が来ました。", report);
            }
        } catch (final RejectedExecutionException e) {
            if (!Thread.currentThread().isInterrupted()) {
                throw e;
            }

        }
    }

    @Override
    protected void after() {
        // 念の為。
        for (final BackupperMaster.BackupperUnit unit : this.backupeerPool.values()) {
            unit.close();
        }
    }

}
