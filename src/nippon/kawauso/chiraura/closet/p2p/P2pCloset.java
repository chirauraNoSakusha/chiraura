package nippon.kawauso.chiraura.closet.p2p;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Messenger;
import nippon.kawauso.chiraura.messenger.Messengers;
import nippon.kawauso.chiraura.network.AddressableNetwork;
import nippon.kawauso.chiraura.network.AddressableNetworks;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Storage;
import nippon.kawauso.chiraura.storage.Storages;

/**
 * P2Pによる四次元押し入れ。
 * @author chirauraNoSakusha
 */
public final class P2pCloset implements Closet {

    /**
     * バージョン。
     */
    private static final long VERSION = 9L; // 2014/03/09.

    /**
     * いくつ離れていたら切り捨てるか。
     * 切り捨てたいときは一気にそれだけ上げれば良い。
     * 古いバージョンを弾きたいときは弾き、そうでないときも上位バージョンを通知できるようにするため。
     * TODO メジャーバージョンとマイナーバージョンにすべきだった。設計ミス。
     */
    private static final long VERSION_GAP_THRESHOLD = 50L;

    /**
     * 1 回の発注・在庫確認で調べるデータ片の最大数。
     */
    private static final int CHECK_CHUNK_LIMIT = 100;

    /**
     * 引数用。
     * @author chirauraNoSakusha
     */
    public static final class Parameters {
        private final File root;
        private final KeyPair id;
        private final int port;

        private final ExecutorService executor;

        // デバッグ時に別なのを使いたいので受け取る。
        private AddressCalculator calculator = new HashingCalculator(10_000);

        private int chunkSizeLimit = 1024 * 1024 + 1024; // 1MB + 1KB.
        private int storageDirectoryBitSize = 8;
        private int chunkCacheCapacity = 300;
        private int indexCacheCapacity = 10_000;
        private int rangeCacheCapacity = 40;
        private int messageSizeLimit = 1024 * 1024 + 1024; // 1 MB + 1 KB.
        private boolean useHttpWrapper = false;
        private int peerCapacity = 1_000;
        private int receiveBufferSize = 128 * 1024; // 128 KB.
        private int sendBufferSize = 64 * 1024; // 64 KB.
        private long maintenanceInterval = Duration.MINUTE;
        private long sleepTime = 30 * Duration.MINUTE;
        private long backupInterval = 5 * Duration.MINUTE;
        private long connectionTimeout = 15 * Duration.MINUTE;
        private long operationTimeout = Duration.MINUTE;
        private int cacheLogCapacity = 10_000;
        private long cacheDuration = 30 * Duration.SECOND;
        private long publicKeyLifetime = Duration.DAY;
        private long commonKeyLifetime = Duration.HOUR;
        private boolean portIgnore = true;
        private int connectionLimit = 5;
        private long trafficDuration = Duration.SECOND;
        private long trafficSizeLimit = 10 * 1024 * 1024; // 10MB.
        private int trafficCountLimit = 500;
        private long trafficPenalty = 10 * Duration.SECOND;
        private long outlawDuration = 10 * Duration.MINUTE;
        private int outlawCountLimit = 10;
        private int blacklistCapacity = 200;
        private long blacklistTimeout = 30 * Duration.MINUTE;
        private int potCapacity = 1_000;
        private int activeAddressLogCapacity = 1_000;
        private long activeAddressDuration = 5 * Duration.MINUTE;

        @SuppressWarnings("unused")
        private List<AddressedPeer> addressedPeers = new ArrayList<>(0);
        private List<InetSocketAddress> peers = new ArrayList<>(0);

        /**
         * 作成する。
         * @param root 倉庫にするディレクトリ
         * @param id 識別用鍵
         * @param port 受け付けポート番号
         * @param executor 実行機
         */
        public Parameters(final File root, final KeyPair id, final int port, final ExecutorService executor) {
            this.root = root;
            this.id = id;
            this.port = port;
            this.executor = executor;
        }

        /**
         * 個体の論理位置の計算機を変える。
         * @param calculator 新しい計算機
         * @return this
         */
        public Parameters setCalculator(final AddressCalculator calculator) {
            this.calculator = calculator;
            return this;
        }

        /**
         * データ片の許容サイズを変える。
         * @param value 新しい値 (バイト)
         * @return this
         */
        public Parameters setChunkSizeLimit(final int value) {
            this.chunkSizeLimit = value;
            return this;
        }

        /**
         * 倉庫のディレクトリ分けに使うビット数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setStorageDirectoryBitSize(final int value) {
            this.storageDirectoryBitSize = value;
            return this;
        }

        /**
         * データ片をキャッシュする数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setChunkCacheCapacity(final int value) {
            this.chunkCacheCapacity = value;
            return this;
        }

        /**
         * データ片の概要をキャッシュする数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setIndexCacheCapacity(final int value) {
            this.indexCacheCapacity = value;
            return this;
        }

        /**
         * データ片の範囲取得結果をキャッシュする数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setRangeCacheCapacity(final int value) {
            this.rangeCacheCapacity = value;
            return this;
        }

        /**
         * 個体を保持する数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setPeerCapacity(final int value) {
            this.peerCapacity = value;
            return this;
        }

        /**
         * 保守間隔を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setMaintenanceInterval(final long value) {
            this.maintenanceInterval = value;
            return this;
        }

        /**
         * ふて寝の時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setSleepTime(final long value) {
            this.sleepTime = value;
            return this;
        }

        /**
         * データ片の保守間隔を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setBackupInterval(final long value) {
            this.backupInterval = value;
            return this;
        }

        /**
         * 非通信接続を切断するまでの時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setConnectionTimeout(final long value) {
            this.connectionTimeout = value;
            return this;
        }

        /**
         * 受信バッファサイズを変える。
         * @param value 新しい値 (バイト)
         * @return this
         */
        public Parameters setReceiveBufferSize(final int value) {
            this.receiveBufferSize = value;
            return this;
        }

        /**
         * 送信バッファサイズを変える。
         * @param value 新しい値 (バイト)
         * @return this
         */
        public Parameters setSendBufferSize(final int value) {
            this.sendBufferSize = value;
            return this;
        }

        /**
         * 通信用公開鍵を使い回す期間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setPublicKeyLifetime(final long value) {
            this.publicKeyLifetime = value;
            return this;
        }

        /**
         * 通信用共通鍵を使い回す期間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setCommonKeyLifetime(final long value) {
            this.commonKeyLifetime = value;
            return this;
        }

        /**
         * 接続制限にてポートの違いを無視するかどうかを変える。
         * @param value 新しい値。無視するなら true
         * @return this
         */
        public Parameters setPortIgnore(final boolean value) {
            this.portIgnore = value;
            return this;
        }

        /**
         * 1 つの通信相手に対する接続の制限数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setConnectionLimit(final int value) {
            this.connectionLimit = value;
            return this;
        }

        /**
         * 通信制限のための単位監視時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setTrafficDuration(final long value) {
            this.trafficDuration = value;
            return this;
        }

        /**
         * 通信を制限する通信量を変える。
         * @param value 新しい値 (バイト)
         * @return this
         */
        public Parameters setTrafficSizeLimit(final long value) {
            this.trafficSizeLimit = value;
            return this;
        }

        /**
         * 通信を制限する通信回数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setTrafficCountLimit(final int value) {
            this.trafficCountLimit = value;
            return this;
        }

        /**
         * 通信を制限する時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setTrafficPenalty(final long value) {
            this.trafficPenalty = value;
            return this;
        }

        /**
         * おかしな挙動の個体を弾くための単位監視時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setOutlawDuration(final long value) {
            this.outlawDuration = value;
            return this;
        }

        /**
         * おかしな挙動を許容する単位監視時間あたりの回数。
         * @param value 新しい値
         * @return this
         */
        public Parameters setOutlawCountLimit(final int value) {
            this.outlawCountLimit = value;
            return this;
        }

        /**
         * 拒否対象の個体を保持する数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setBlacklistCapacity(final int value) {
            this.blacklistCapacity = value;
            return this;
        }

        /**
         * 個体の除外を解除するまでの時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setBlacklistTimeout(final long value) {
            this.blacklistTimeout = value;
            return this;
        }

        /**
         * 予備に保持する個体の数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setPotCapacity(final int value) {
            this.potCapacity = value;
            return this;
        }

        /**
         * 通信を要する操作を諦めるまでの時間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setOperationTimeout(final long value) {
            this.operationTimeout = value;
            return this;
        }

        /**
         * 通信結果を記録する数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setCacheLogCapacity(final int value) {
            this.cacheLogCapacity = value;
            return this;
        }

        /**
         * 通信結果を使い回す期間を変える。
         * @param value 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setCacheDuration(final long value) {
            this.cacheDuration = value;
            return this;
        }

        /**
         * 言付けの許容サイズを変える。
         * @param value 新しい値 (バイト)
         * @return this
         */
        public Parameters setMessageSizeLimit(final int value) {
            this.messageSizeLimit = value;
            return this;
        }

        /**
         * デフォルトで HTTP 偽装するかどうかを変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setUseHttpWrapper(final boolean value) {
            this.useHttpWrapper = value;
            return this;
        }

        /**
         * 直接通信して得た個体の論理位置を記憶する数を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setActiveAddressLogCapacity(final int value) {
            this.activeAddressLogCapacity = value;
            return this;
        }

        /**
         * 直接通信して得た個体の論理位置を優先する期間を変える。
         * @param value 新しい値
         * @return this
         */
        public Parameters setActiveAddressDuration(final long value) {
            this.activeAddressDuration = value;
            return this;
        }

        /**
         * 初期個体を設定する。
         * @param peers 初期個体
         * @return this
         */
        public Parameters setAddressedPeers(final List<AddressedPeer> peers) {
            this.addressedPeers = peers;
            return this;
        }

        /**
         * 予備の初期個体を設定する。
         * @param hosts 予備の初期個体
         * @return this
         */
        public Parameters setPeers(final List<InetSocketAddress> hosts) {
            this.peers = hosts;
            return this;
        }
    }

    private static final Logger LOG = Logger.getLogger(P2pCloset.class.getName());

    // 保持。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final Set<Class<? extends Chunk>> backupTypes;

    private final BlockingQueue<Operation> operationQueue;
    private final SessionManager sessionManager;
    private final DriverSet drivers;

    private final BlockingQueue<ClosetReport> closetReportQueue;

    // 実行用引数。
    private final long maintenanceInterval;
    private final long sleepTime;
    private final long backupInterval;
    private final long operationTimeout;

    private final BlockingQueue<OutlawReport> outlawReportQueue;
    private final boolean portIgnore;
    private final long outlawDuration;
    private final int outlawCountLimit;

    /**
     * 作成する。
     * @param param 設定値
     */
    public P2pCloset(final Parameters param) {
        if (param == null) {
            throw new IllegalArgumentException("Null parameters.");
        }

        this.operationQueue = new LinkedBlockingQueue<>();
        this.sessionManager = new SessionManager();

        final Storage rawStorage = Storages.newInstance(param.root, param.chunkSizeLimit, param.storageDirectoryBitSize, param.chunkCacheCapacity,
                param.indexCacheCapacity, param.rangeCacheCapacity);
        this.storage = new StorageWrapper(rawStorage, this.operationQueue, param.cacheLogCapacity, param.cacheDuration);

        final Messenger messenger = Messengers.newInstance(param.port, param.receiveBufferSize, param.sendBufferSize, param.connectionTimeout,
                param.operationTimeout, param.messageSizeLimit, param.useHttpWrapper, VERSION, VERSION_GAP_THRESHOLD, param.id, param.publicKeyLifetime,
                param.commonKeyLifetime, param.portIgnore, param.connectionLimit, param.trafficDuration, param.trafficSizeLimit, param.trafficCountLimit,
                param.trafficPenalty);
        final AddressableNetwork rawNetwork = AddressableNetworks.newInstance(param.calculator.calculate(param.id.getPublic()), param.peerCapacity,
                param.maintenanceInterval);
        final PeerBlacklist blacklist = new TimeLimitedPeerBlacklist(param.blacklistCapacity, param.blacklistTimeout);
        final PeerBlacklist lostPeers = new TimeLimitedPeerBlacklist(param.blacklistCapacity, param.maintenanceInterval * 2); // TODO 時間はかなりてきとう。
        final PeerPot pot = new FifoPeerPot(param.potCapacity);
        this.network = new NetworkWrapper(P2pCloset.VERSION, rawNetwork, messenger, blacklist, lostPeers, pot, this.operationQueue, param.calculator,
                param.activeAddressLogCapacity, param.activeAddressDuration);

        Register.init(this.network, this.storage);

        this.backupTypes = new HashSet<>();

        this.closetReportQueue = new LinkedBlockingQueue<>();
        this.drivers = new DriverSet(this.network, this.storage, this.sessionManager, this.operationQueue, param.executor, CHECK_CHUNK_LIMIT, this.backupTypes);

        this.maintenanceInterval = param.maintenanceInterval;
        this.sleepTime = param.sleepTime;
        this.backupInterval = param.backupInterval;
        this.operationTimeout = param.operationTimeout;
        if (param.peers != null) {
            for (final InetSocketAddress host : param.peers) {
                this.network.reservePeer(host);
            }
        }

        this.outlawReportQueue = new LinkedBlockingQueue<>();
        this.portIgnore = param.portIgnore;
        this.outlawDuration = param.outlawDuration;
        this.outlawCountLimit = param.outlawCountLimit;
    }

    @Override
    public void start(final ExecutorService executor) {
        executor.submit(new Boss(this.network, this.sessionManager, this.maintenanceInterval, this.sleepTime, this.backupInterval, this.operationTimeout,
                VERSION_GAP_THRESHOLD, executor, this.operationQueue, this.closetReportQueue, this.drivers, this.outlawReportQueue, this.portIgnore,
                this.outlawDuration, this.outlawCountLimit));
        this.network.start(executor);
    }

    /**
     * 自身の識別用公開鍵を返す。
     * @return 自身の識別用公開鍵
     */
    public KeyPair getId() {
        return this.network.getId();
    }

    /**
     * 自身の個体情報を返す。
     * @return 自身の個体情報。
     *         不明な場合は null
     */
    public InetSocketAddress getSelf() {
        return this.network.getSelf();
    }

    Address getSelfAddress() {
        return this.network.getSelfAddress();
    }

    /**
     * 把握している個体を返す。
     * @return 把握している個体
     */
    public List<AddressedPeer> getPeers() {
        return this.network.getPeers();
    }

    /**
     * 予備に把握している個体を返す。
     * @return 予備に把握している個体
     */
    public List<InetSocketAddress> getBackupPeers() {
        return this.network.getReservedPeers();
    }

    /**
     * こちらから通信先として使用する個体を返す。
     * @return こちらから通信先として使用する個体
     */
    public List<AddressedPeer> getImportantPeers() {
        return this.network.getImportantPeers();
    }

    @Override
    public <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser) {
        this.storage.registerChunk(type, chunkClass, chunkParser, idClass, idParser);
        this.backupTypes.add(chunkClass);
    }

    @Override
    public <C extends Mountain, I extends Chunk.Id<C>, D extends Mountain.Dust<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser,
            final Class<D> diffClass, final BytesConvertible.Parser<? extends D> diffParser) {
        this.storage.registerChunk(type, chunkClass, chunkParser, idClass, idParser, diffClass, diffParser);
        this.backupTypes.add(chunkClass);
    }

    @Override
    public void removeBackupType(final Class<? extends Chunk> chunkClass) {
        this.backupTypes.remove(chunkClass);
    }

    @Override
    public <T extends Chunk> T getChunk(final Chunk.Id<T> id, final long timeout) throws InterruptedException {
        if (Mountain.class.isAssignableFrom(id.getChunkClass())) {
            @SuppressWarnings("unchecked")
            final Chunk.Id<? extends Mountain> id0 = (Chunk.Id<? extends Mountain>) id;
            GetOrUpdateCacheResult result;
            try {
                result = getOrUpdateCache(id0, timeout);
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                LOG.log(Level.INFO, "異常を無視します。");
                return null;
            }
            if (result.isGivenUp() || result.isNotFound()) {
                try {
                    // 古いキャッシュで我慢。
                    return getLocal(id);
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", e);
                    LOG.log(Level.INFO, "異常を無視します。");
                    return null;
                }
            } else {
                @SuppressWarnings("unchecked")
                final T chunk = (T) result.getChunk();
                return chunk;
            }
        } else {
            GetCacheResult result;
            try {
                result = getCache(id, timeout);
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                LOG.log(Level.INFO, "異常を無視します。");
                return null;
            }
            if (result.isGivenUp()) {
                return null;
            } else if (result.isNotFound()) {
                return null;
            } else {
                @SuppressWarnings("unchecked")
                final T chunk = (T) result.getChunk();
                return chunk;
            }
        }
    }

    @Override
    public <T extends Chunk> T getChunkImmediately(final Chunk.Id<T> id) throws InterruptedException {
        try {
            return getLocal(id);
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "異常を無視します。");
            return null;
        }
    }

    @Override
    public boolean addChunk(final Chunk chunk, final long timeout) throws InterruptedException {
        AddCacheResult result;
        try {
            result = addCache(chunk, timeout);
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "異常を無視します。");
            return false;
        }
        if (result.isGivenUp()) {
            return false;
        } else {
            return result.isSuccess();
        }
    }

    private static class PatchResult<T extends Mountain> implements Closet.PatchResult<T> {
        private final boolean givenUp;
        private final boolean notFound;
        private final boolean success;
        private final T chunk;

        private PatchResult(final boolean givenUp, final boolean notFound, final boolean success, final T chunk) {
            this.givenUp = givenUp;
            this.notFound = notFound;
            this.success = success;
            this.chunk = chunk;
        }

        private static <T extends Mountain> PatchResult<T> newGiveUp() {
            return new PatchResult<>(true, false, false, null);
        }

        private static <T extends Mountain> PatchResult<T> newNotFound() {
            return new PatchResult<>(false, true, false, null);
        }

        private static <T extends Mountain> PatchResult<T> newFailure(final T chunk) {
            if (chunk == null) {
                throw new IllegalArgumentException("Null chunk.");
            }
            return new PatchResult<>(false, false, false, chunk);
        }

        private PatchResult(final T chunk) {
            this(false, false, true, chunk);
            if (chunk == null) {
                throw new IllegalArgumentException("Null chunk.");
            }
        }

        @Override
        public boolean isGivenUp() {
            return this.givenUp;
        }

        @Override
        public boolean isNotFound() {
            return this.notFound;
        }

        @Override
        public boolean isSuccess() {
            return this.success;
        }

        @Override
        public T getChunk() {
            return this.chunk;
        }
    }

    @Override
    public <T extends Mountain> Closet.PatchResult<T> patchChunk(final Chunk.Id<T> id, final Mountain.Dust<T> diff, final long timeout)
            throws InterruptedException {
        PatchAndGetOrUpdateCacheResult result;
        try {
            result = patchAndGetOrUpdateCache(id, diff, timeout);
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "異常を無視します。");
            return PatchResult.newGiveUp();
        }
        if (result.isGivenUp()) {
            return PatchResult.newGiveUp();
        } else if (result.isNotFound()) {
            return PatchResult.newNotFound();
        } else if (result.isSuccess()) {
            @SuppressWarnings("unchecked")
            final T chunk = (T) result.getChunk();
            return new PatchResult<>(chunk);
        } else {
            @SuppressWarnings("unchecked")
            final T chunk = (T) result.getChunk();
            return PatchResult.newFailure(chunk);
        }
    }

    private static class PatchOrAddResult<T extends Mountain> implements Closet.PatchOrAddResult<T> {
        private final boolean givenUp;
        private final T chunk;

        private PatchOrAddResult(final boolean givenUp, final T chunk) {
            this.givenUp = givenUp;
            this.chunk = chunk;
        }

        private static <T extends Mountain> PatchOrAddResult<T> newGiveUp() {
            return new PatchOrAddResult<>(true, null);
        }

        private PatchOrAddResult(final T chunk) {
            this(false, chunk);
        }

        @Override
        public boolean isGivenUp() {
            return this.givenUp;
        }

        @Override
        public T getChunk() {
            return this.chunk;
        }

    }

    @Override
    public <T extends Mountain> Closet.PatchOrAddResult<T> patchOrAddChunk(final T chunk, final long timeout) throws InterruptedException {
        PatchOrAddAndGetCacheResult result;
        try {
            result = patchOrAddAndGetCache(chunk, timeout);
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "異常を無視します。");
            return PatchOrAddResult.newGiveUp();
        }
        if (result.isGivenUp()) {
            return PatchOrAddResult.newGiveUp();
        } else {
            @SuppressWarnings("unchecked")
            final T chunk2 = (T) result.getChunk();
            return new PatchOrAddResult<>(chunk2);
        }
    }

    <T extends Chunk> T getLocal(final Chunk.Id<T> id) throws IOException, InterruptedException {
        return this.storage.read(id);
    }

    GetChunkResult getOriginal(final Chunk.Id<?> id, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getGetChunkBlocking().execute(new GetChunkOperation(id), timeout);
    }

    GetCacheResult getCache(final Chunk.Id<?> id, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getGetCacheBlocking().execute(new GetCacheOperation(id), timeout);
    }

    UpdateChunkResult updateOriginal(final Chunk.Id<? extends Mountain> id, final long date, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getUpdateChunkBlocking().execute(new UpdateChunkOperation(id, date), timeout);
    }

    GetOrUpdateCacheResult getOrUpdateCache(final Chunk.Id<? extends Mountain> id, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getGetOrUpdateCacheBlocking().execute(new GetOrUpdateCacheOperation(id), timeout);
    }

    boolean addLocal(final Chunk chunk) throws IOException, InterruptedException {
        return this.storage.weakWrite(chunk);
    }

    AddChunkResult addOriginal(final Chunk chunk, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getAddChunkBlocking().execute(new AddChunkOperation(chunk), timeout);
    }

    AddCacheResult addCache(final Chunk chunk, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getAddCacheBlocking().execute(new AddCacheOperation(chunk), timeout);
    }

    <T extends Mountain> StorageWrapper.Result<T> patchLocal(final Chunk.Id<T> id, final Mountain.Dust<T> diff) throws InterruptedException, IOException {
        return this.storage.patch(id, diff);
    }

    <T extends Mountain> PatchChunkResult patchOriginal(final Chunk.Id<T> id, final Mountain.Dust<T> diff, final long timeout) throws InterruptedException,
            IOException {
        return this.drivers.getPatchChunkBlocking().execute(new PatchChunkOperation<>(id, diff), timeout);
    }

    PatchOrAddAndGetCacheResult patchOrAddAndGetCache(final Mountain chunk, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getPatchOrAddAndGetCacheBlocking().execute(new PatchOrAddAndGetCacheOperation(chunk), timeout);
    }

    <T extends Mountain> PatchAndGetOrUpdateCacheResult patchAndGetOrUpdateCache(final Chunk.Id<T> id, final Mountain.Dust<T> diff, final long timeout)
            throws InterruptedException, IOException {
        return this.drivers.getPatchAndGetOrUpdateCacheBlocking().execute(new PatchAndGetOrUpdateCacheOperation<>(id, diff), timeout);
    }

    CheckStockResult checkStock(final InetSocketAddress destination, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getCheckStock().execute(new CheckStockOperation(destination), timeout);
    }

    CheckDemandResult checkDemand(final InetSocketAddress destination, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getCheckDemand().execute(new CheckDemandOperation(destination), timeout);
    }

    RecoveryResult recovery(final StockEntry destinationStock, final InetSocketAddress destination, final long timeout) throws InterruptedException,
            IOException {
        return this.drivers.getRecovery().execute(new RecoveryOperation(destinationStock, destination), timeout);
    }

    BackupResult backup(final DemandEntry demand, final InetSocketAddress destination, final long timeout) throws InterruptedException, IOException {
        return this.drivers.getBackup().execute(new BackupOperation(demand, destination), timeout);
    }

    @Override
    public ClosetReport takeError() throws InterruptedException {
        return this.closetReportQueue.take();
    }

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException {
        this.storage.close();
    }

    /**
     * JCE が制限されているどうか検査。
     * @return 制限されていれば true
     */
    public static boolean isLimitedJce() {
        return Messengers.isLimitedJce();
    }

}
