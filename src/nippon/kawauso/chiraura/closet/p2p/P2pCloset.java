/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.closet.Mountain;
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
    private static final long VERSION = 7L; // 2014/02/08.

    /**
     * いくつ離れていたら切り捨てるか。
     * 切り捨てたいときは一気にそれだけ上げれば良い。
     * 古いバージョンを弾きたいときは弾き、そうでないときも上位バージョンを通知できるようにするため。
     * TODO メジャーバージョンとマイナーバージョンにすべきだった。設計ミス。
     */
    private static final long VERSION_GAP_THRESHOLD = 50L;

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
        private int peerCapacity = 1_000;
        private int receiveBufferSize = 128 * 1024; // 128 KB.
        private int sendBufferSize = 64 * 1024; // 64 KB.
        private long maintenanceInterval = 60 * 1_000L; // 1 分。
        private long sleepTime = 30 * 60 * 1_000L; // 30 分。
        private long backupInterval = 5 * 60 * 1_000L; // 5 分。
        private long connectionTimeout = 15 * 60 * 1_000L; // 15 分。
        private long operationTimeout = 60 * 1_000L; // 1 分。
        private int cacheLogCapacity = 10_000;
        private long cacheDuration = 30 * 1_000; // 30 秒。
        private long publicKeyLifetime = 24 * 60 * 60 * 1_000L; // 1 日。
        private long commonKeyLifetime = 60 * 60 * 1_000L; // 1 時間。
        private int blacklistCapacity = 200;
        private long blacklistTimeout = 30 * 60 * 1_000L; // 30 分。
        private int potCapacity = 1_000;
        private int activeAddressLogCapacity = 1_000;
        private long activeAddressDuration = 5 * 60 * 1_000; // 5 分。

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
         * @param chunkSizeLimit 新しい値 (バイト)
         * @return this
         */
        public Parameters setChunkSizeLimit(final int chunkSizeLimit) {
            this.chunkSizeLimit = chunkSizeLimit;
            return this;
        }

        /**
         * 倉庫のディレクトリ分けに使うビット数を変える。
         * @param storageDirectoryBitSize 新しい値
         * @return this
         */
        public Parameters setStorageDirectoryBitSize(final int storageDirectoryBitSize) {
            this.storageDirectoryBitSize = storageDirectoryBitSize;
            return this;
        }

        /**
         * データ片をキャッシュする数を変える。
         * @param chunkCacheCapacity 新しい値
         * @return this
         */
        public Parameters setChunkCacheCapacity(final int chunkCacheCapacity) {
            this.chunkCacheCapacity = chunkCacheCapacity;
            return this;
        }

        /**
         * データ片の概要をキャッシュする数を変える。
         * @param indexCacheCapacity 新しい値
         * @return this
         */
        public Parameters setIndexCacheCapacity(final int indexCacheCapacity) {
            this.indexCacheCapacity = indexCacheCapacity;
            return this;
        }

        /**
         * データ片の範囲取得結果をキャッシュする数を変える。
         * @param rangeCacheCapacity 新しい値
         * @return this
         */
        public Parameters setRangeCacheCapacity(final int rangeCacheCapacity) {
            this.rangeCacheCapacity = rangeCacheCapacity;
            return this;
        }

        /**
         * 個体を保持する数を変える。
         * @param peerCapacity 新しい値
         * @return this
         */
        public Parameters setPeerCapacity(final int peerCapacity) {
            this.peerCapacity = peerCapacity;
            return this;
        }

        /**
         * 保守間隔を変える。
         * @param maintenanceInterval 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setMaintenanceInterval(final long maintenanceInterval) {
            this.maintenanceInterval = maintenanceInterval;
            return this;
        }

        /**
         * ふて寝の時間を変える。
         * @param sleepTime 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setSleepTime(final long sleepTime) {
            this.sleepTime = sleepTime;
            return this;
        }

        /**
         * データ片の保守間隔を変える。
         * @param backupInterval 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setBackupInterval(final long backupInterval) {
            this.backupInterval = backupInterval;
            return this;
        }

        /**
         * 非通信接続を切断するまでの時間を変える。
         * @param connectionTimeout 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setConnectionTimeout(final long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * 受信バッファサイズを変える。
         * @param receiveBufferSize 新しい値 (バイト)
         * @return this
         */
        public Parameters setReceiveBufferSize(final int receiveBufferSize) {
            this.receiveBufferSize = receiveBufferSize;
            return this;
        }

        /**
         * 送信バッファサイズを変える。
         * @param sendBufferSize 新しい値 (バイト)
         * @return this
         */
        public Parameters setSendBufferSize(final int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
            return this;
        }

        /**
         * 通信用公開鍵を使い回す期間を変える。
         * @param publicKeyLifetime 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setPublicKeyLifetime(final long publicKeyLifetime) {
            this.publicKeyLifetime = publicKeyLifetime;
            return this;
        }

        /**
         * 通信用共通鍵を使い回す期間を変える。
         * @param commonKeyLifetime 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setCommonKeyLifetime(final long commonKeyLifetime) {
            this.commonKeyLifetime = commonKeyLifetime;
            return this;
        }

        /**
         * 拒否対象の個体を保持する数を変える。
         * @param blacklistCapacity 新しい値
         * @return this
         */
        public Parameters setBlacklistCapacity(final int blacklistCapacity) {
            this.blacklistCapacity = blacklistCapacity;
            return this;
        }

        /**
         * 個体の除外を解除するまでの時間を変える。
         * @param blacklistTimeout 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setBlacklistTimeout(final long blacklistTimeout) {
            this.blacklistTimeout = blacklistTimeout;
            return this;
        }

        /**
         * 予備に保持する個体の数を変える。
         * @param potCapacity 新しい値
         * @return this
         */
        public Parameters setPotCapacity(final int potCapacity) {
            this.potCapacity = potCapacity;
            return this;
        }

        /**
         * 通信を要する操作を諦めるまでの時間を変える。
         * @param operationTimeout 新しい値 (ミリ秒)
         * @return this
         */
        public Parameters setOperationTimeout(final long operationTimeout) {
            this.operationTimeout = operationTimeout;
            return this;
        }

        /**
         * 通信結果を記録する数を変える。
         * @param cacheLogCapacity 新しい値
         * @return this
         */
        public Parameters setCacheLogCapacity(final int cacheLogCapacity) {
            this.cacheLogCapacity = cacheLogCapacity;
            return this;
        }

        /**
         * 通信結果を使い回す期間を変える。
         * @param cacheDuration 新しい値
         * @return this
         */
        public Parameters setCacheDuration(final long cacheDuration) {
            this.cacheDuration = cacheDuration;
            return this;
        }

        /**
         * 言付けの許容サイズを変える。
         * @param messageSizeLimit 新しい値 (バイト)
         * @return this
         */
        public Parameters setMessageSizeLimit(final int messageSizeLimit) {
            this.messageSizeLimit = messageSizeLimit;
            return this;
        }

        /**
         * 直接通信して得た個体の論理位置を記憶する数を変える。
         * @param activeAddressLogCapacity 新しい値
         * @return this
         */
        public Parameters setActiveAddressLogCapacity(final int activeAddressLogCapacity) {
            this.activeAddressLogCapacity = activeAddressLogCapacity;
            return this;
        }

        /**
         * 直接通信して得た個体の論理位置を優先する期間を変える。
         * @param activeAddressDuration 新しい値
         * @return this
         */
        public Parameters setActiveAddressDuration(final long activeAddressDuration) {
            this.activeAddressDuration = activeAddressDuration;
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

    private final BlockingQueue<Operation> operationQueue;
    private final SessionManager sessionManager;
    private final DriverSet drivers;

    private final BlockingQueue<ClosetReport> closetReportQueue;

    // 実行用引数。
    private final long maintenanceInterval;
    private final long sleepTime;
    private final long backupInterval;
    private final long operationTimeout;

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
                param.operationTimeout, param.messageSizeLimit, param.id, VERSION, VERSION_GAP_THRESHOLD, param.publicKeyLifetime, param.commonKeyLifetime);
        final AddressableNetwork rawNetwork = AddressableNetworks.newInstance(param.calculator.calculate(param.id.getPublic()), param.peerCapacity,
                param.maintenanceInterval);
        final PeerBlacklist blacklist = new TimeLimitedPeerBlacklist(param.blacklistCapacity, param.blacklistTimeout);
        final PeerBlacklist lostPeers = new TimeLimitedPeerBlacklist(param.blacklistCapacity, param.maintenanceInterval * 3 / 2);
        final PeerPot pot = new FifoPeerPot(param.potCapacity);
        this.network = new NetworkWrapper(P2pCloset.VERSION, rawNetwork, messenger, blacklist, lostPeers, pot, this.operationQueue, param.calculator,
                param.activeAddressLogCapacity, param.activeAddressDuration);

        Register.init(this.network, this.storage);

        this.closetReportQueue = new LinkedBlockingQueue<>();
        this.drivers = new DriverSet(this.network, this.storage, this.sessionManager, this.operationQueue, param.executor);

        this.maintenanceInterval = param.maintenanceInterval;
        this.sleepTime = param.sleepTime;
        this.backupInterval = param.backupInterval;
        this.operationTimeout = param.operationTimeout;
        if (param.peers != null) {
            for (final InetSocketAddress host : param.peers) {
                this.network.reservePeer(host);
            }
        }
        // Lonely に並列性を持たせたので以下は不要。
        // if (param.addressedPeers != null && !param.addressedPeers.isEmpty()) {
        // // 初期個体への接続を予約する。
        // for (final AddressedPeer peer : param.addressedPeers) {
        // this.network.addPeer(peer);
        // }
        // if (!this.network.isEmpty()) {
        // ConcurrentFunctions.completePut(new AddressAccessOperation(this.network.getSelfAddress().subtractOne()), this.operationQueue);
        // }
        // }
    }

    @Override
    public void start(final ExecutorService executor) {
        executor.submit(new Boss(this.network, this.sessionManager, this.maintenanceInterval, this.sleepTime, this.backupInterval, this.operationTimeout,
                executor, this.operationQueue, this.closetReportQueue, this.drivers));
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
    }

    @Override
    public <C extends Mountain, I extends Chunk.Id<C>, D extends Mountain.Dust<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser,
            final Class<D> diffClass, final BytesConvertible.Parser<? extends D> diffParser) {
        this.storage.registerChunk(type, chunkClass, chunkParser, idClass, idParser, diffClass, diffParser);
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
                LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
                return null;
            }
            if (result.isGivenUp() || result.isNotFound()) {
                try {
                    // 古いキャッシュで我慢。
                    return getLocal(id);
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
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
                LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
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
            LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
            return null;
        }
    }

    @Override
    public boolean addChunk(final Chunk chunk, final long timeout) throws InterruptedException {
        AddCacheResult result;
        try {
            result = addCache(chunk, timeout);
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
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
            LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
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
            LOG.log(Level.WARNING, "異常発生。でも、無視します。", e);
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
