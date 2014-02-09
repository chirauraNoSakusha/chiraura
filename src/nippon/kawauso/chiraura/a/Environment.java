/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.closet.p2p.AddressCalculator;
import nippon.kawauso.chiraura.closet.p2p.HashingCalculator;
import nippon.kawauso.chiraura.lib.Mosaic;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.OneLineThreadFormatter;
import nippon.kawauso.chiraura.messenger.CryptographicKeys;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 環境の整備。
 * @author chirauraNoSakusha
 */
final class Environment {

    private static final Logger LOG = Logger.getLogger(Environment.class.getName());

    /*
     * {root}/: 使用する最上位のディレクトリ。
     * {root}/self.txt: 自身の公開用個体情報を保存するファイル。
     * {root}/peers.txt: 公開されている個体情報を保存するファイル。
     * {root}/names.txt: 板固有の名無しを保存するファイル。
     * {root}/storage/: Storage に使用するディレクトリ。
     * {root}/resource/: 何かと突っこんでおくディレクトリ。
     * {root}/resource/id.dat: 自身の識別用鍵を保存するファイル。
     * {root}/resource/addressedPeers.dat: 個体を保存するファイル。
     * {root}/resource/peers.dat: 予備の個体を保存するファイル。
     */

    private final File root;
    private final File storageRoot;

    private final File idFile;

    private final int port;
    private final long shutdownTimeout;

    private final Level consoleLogLevel;
    private final Class<? extends Formatter> consoleLogFormatter;

    private final Level fileLogLevel;
    private final Class<? extends Formatter> fileLogFormatter;
    private final String fileLogPattern;
    private final int fileLogLimit;
    private final int fileLogCount;
    private final boolean fileLogAppend;

    private final int chunkSizeLimit;
    private final int storageDirectoryBitSize;
    private final int chunkCacheCapacity;
    private final int indexCacheCapacity;
    private final int rangeCacheCapacity;

    private final int peerCapacity;
    private final int messageSizeLimit;
    private final int receiveBufferSize;
    private final int sendBufferSize;
    private final long maintenanceInterval;
    private final long sleepTime;
    private final long backupInterval;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final int cacheLogCapacity;
    private final long cacheDuration;
    private final long idLifetime;
    private final long publicKeyLifetime;
    private final long commonKeyLifetime;
    private final int blacklistCapacity;
    private final long blacklistTimeout;
    private final int potCapacity;
    private final AddressCalculator addressCalculator;
    private final int activeAddressLogCapacity;
    private final long activeAddressDuration;

    private final File addressedPeerFile;
    private final File peerFile;
    private final File userPeerFile;

    private final int bbsPort;
    private final long bbsConnectionTimeout;
    private final long bbsInternalTimeout;
    private final long bbsUpdateThreshold;

    private final File bbsNameFile;

    private final File selfFile;
    private final boolean gui;

    private final ExecutorService executor;

    Environment(final Option option) throws IOException {
        this(option, new HashingCalculator(Integer.parseInt(option.get(Option.Item.addressCacheCapacity))));
    }

    private static long getDefaultLong(final Option option, final Option.Item item) {
        if (Global.isDebug()) {
            return Long.parseLong(option.get(item));
        } else {
            return Long.parseLong(item.getInitialValue());
        }
    }

    private static long getLargerLong(final Option option, final Option.Item item) {
        final long givenValue = Long.parseLong(option.get(item));
        if (Global.isDebug()) {
            return givenValue;
        } else {
            final long defaultValue = Long.parseLong(item.getInitialValue());
            return Math.max(givenValue, defaultValue);
        }
    }

    private static int getLargerInt(final Option option, final Option.Item item) {
        final int givenValue = Integer.parseInt(option.get(item));
        if (Global.isDebug()) {
            return givenValue;
        } else {
            final int defaultValue = Integer.parseInt(item.getInitialValue());
            return Math.max(givenValue, defaultValue);
        }
    }

    private static int getDefaultInt(final Option option, final Option.Item item) {
        if (Global.isDebug()) {
            return Integer.parseInt(option.get(item));
        } else {
            return Integer.parseInt(item.getInitialValue());
        }
    }

    /**
     * デバッグ用
     * @param option オプション
     * @param addressCalculator 論理位置の計算機
     * @throws IOException ファイルシステム異常
     */
    Environment(final Option option, final AddressCalculator addressCalculator) throws IOException {
        if (option == null) {
            throw new IllegalArgumentException("Null option.");
        } else if (addressCalculator == null) {
            throw new IllegalArgumentException("Null address calculator.");
        }

        this.root = loadDirectory(new File(option.get(Option.Item.root)));
        this.storageRoot = loadDirectory(new File(this.root, "storage"));
        final File resourceDirectory = loadDirectory(new File(this.root, "resource"));

        this.idFile = loadFile(new File(resourceDirectory, "id.dat"));

        this.port = Integer.parseInt(option.get(Option.Item.port));
        this.shutdownTimeout = getLargerLong(option, Option.Item.shutdownTimeout);

        this.consoleLogLevel = Level.parse(option.get(Option.Item.consoleLogLevel));
        this.consoleLogFormatter = OneLineThreadFormatter.class;

        final File logDirectory = new File(this.root, "log");
        this.fileLogLevel = Level.parse(option.get(Option.Item.fileLogLevel));
        this.fileLogFormatter = OneLineThreadFormatter.class;
        this.fileLogPattern = logDirectory.getPath() + File.separator + "%g.log";
        this.fileLogLimit = 1024 * 1024; // 1MB.
        this.fileLogCount = 100;
        this.fileLogAppend = true;
        if (this.fileLogLevel != Level.OFF) {
            loadDirectory(logDirectory);
        }

        this.chunkSizeLimit = 1024 * 1024 + 1024; // 1MB + 1KB.
        this.storageDirectoryBitSize = 8;
        this.chunkCacheCapacity = getLargerInt(option, Option.Item.chunkCacheCapacity);
        this.indexCacheCapacity = getLargerInt(option, Option.Item.indexCacheCapacity);
        this.rangeCacheCapacity = getLargerInt(option, Option.Item.rangeCacheCapacity);

        this.peerCapacity = getLargerInt(option, Option.Item.peerCapacity);
        this.messageSizeLimit = 1024 * 1024 + 1024; // 1MB + 1KB.
        this.receiveBufferSize = getLargerInt(option, Option.Item.receiveBufferSize);
        this.sendBufferSize = getLargerInt(option, Option.Item.sendBufferSize);
        this.maintenanceInterval = getDefaultLong(option, Option.Item.maintenanceInterval);
        this.sleepTime = getDefaultLong(option, Option.Item.sleepTime);
        this.backupInterval = getDefaultLong(option, Option.Item.backupInterval);
        this.connectionTimeout = getDefaultLong(option, Option.Item.connectionTimeout);
        this.operationTimeout = getLargerLong(option, Option.Item.operationTimeout);
        this.cacheLogCapacity = getDefaultInt(option, Option.Item.cacheLogCapacity);
        this.cacheDuration = getDefaultInt(option, Option.Item.cacheDuration);
        this.idLifetime = getLargerLong(option, Option.Item.idLifetime);
        this.publicKeyLifetime = getLargerLong(option, Option.Item.publicKeyLifetime);
        this.commonKeyLifetime = getLargerLong(option, Option.Item.commonKeyLifetime);
        this.blacklistCapacity = getLargerInt(option, Option.Item.blacklistCapacity);
        this.blacklistTimeout = getDefaultLong(option, Option.Item.blacklistTimeout);
        this.potCapacity = getLargerInt(option, Option.Item.potCapacity);
        this.addressCalculator = addressCalculator;
        this.activeAddressLogCapacity = getDefaultInt(option, Option.Item.activeAddressLogCapacity);
        this.activeAddressDuration = getDefaultInt(option, Option.Item.activeAddressDuration);

        this.addressedPeerFile = loadFile(new File(resourceDirectory, "addressedPeers.dat"));
        this.peerFile = loadFile(new File(resourceDirectory, "peers.dat"));
        this.userPeerFile = loadFile(new File(this.root, "peers.txt"));

        this.bbsPort = Integer.parseInt(option.get(Option.Item.bbsPort));
        this.bbsConnectionTimeout = getDefaultLong(option, Option.Item.bbsConnectionTimeout);
        this.bbsInternalTimeout = getLargerLong(option, Option.Item.bbsInternalTimeout);
        this.bbsUpdateThreshold = getLargerLong(option, Option.Item.bbsUpdateThreshold);

        this.bbsNameFile = loadFile(new File(this.root, "names.txt"));

        this.selfFile = loadFile(new File(this.root, "self.txt"));
        this.gui = Boolean.parseBoolean(option.get(Option.Item.gui));

        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * ファイルを準備する。
     * 異常が発生しなければ、ファイルが存在し読み書き可能である、または、
     * ファイルは存在しないが読み書き可で作成可能である。
     * @param file 準備するファイル
     * @return file
     * @throws IOException ファイル異常
     */
    private static File loadFile(final File file) throws IOException {
        if (file.exists()) {
            if (!file.isFile()) {
                throw new IOException("Not file ( " + file.getPath() + " ).");
            } else if (!file.canRead()) {
                throw new IOException("Not readable file ( " + file.getPath() + " ).");
            } else if (!file.canWrite()) {
                throw new IOException("Not writable file ( " + file.getPath() + " ).");
            }
        } else {
            loadDirectory(file.getParentFile());
        }
        return file;
    }

    /**
     * ディレクトリを準備する。
     * @param directory 準備するディレクトリ
     * @return directory
     * @throws IOException ディレクトリ異常
     */
    private static File loadDirectory(final File directory) throws IOException {
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                LOG.log(Level.INFO, "{0} を作成しました。", directory.getPath());
            } else {
                throw new IOException("Can't make directory ( " + directory.getPath() + " ).");
            }
        }
        if (!directory.isDirectory()) {
            throw new IOException("Not directory ( " + directory.getPath() + " ).");
        } else if (!directory.canRead()) {
            throw new IOException("Not readable directory ( " + directory.getPath() + " ).");
        } else if (!directory.canWrite()) {
            throw new IOException("Not writable directory ( " + directory.getPath() + " ).");
        }
        return directory;
    }

    String getRootPath() {
        return this.root.getPath();
    }

    File getStorageRoot() {
        return this.storageRoot;
    }

    int getPort() {
        return this.port;
    }

    long getShutdownTimeout() {
        return this.shutdownTimeout;
    }

    int getStorageDirectoryBitSize() {
        return this.storageDirectoryBitSize;
    }

    int getChunkCacheCapacity() {
        return this.chunkCacheCapacity;
    }

    int getIndexCacheCapacity() {
        return this.indexCacheCapacity;
    }

    int getRangeCacheCapacity() {
        return this.rangeCacheCapacity;
    }

    int getChunkSizeLimit() {
        return this.chunkSizeLimit;
    }

    int getPeerCapacity() {
        return this.peerCapacity;
    }

    long getMaintenanceInterval() {
        return this.maintenanceInterval;
    }

    long getSleepTime() {
        return this.sleepTime;
    }

    long getBackupInterval() {
        return this.backupInterval;
    }

    long getConnectionTimeout() {
        return this.connectionTimeout;
    }

    int getCacheLogCapacity() {
        return this.cacheLogCapacity;
    }

    long getCacheDuration() {
        return this.cacheDuration;
    }

    int getReceiveBufferSize() {
        return this.receiveBufferSize;
    }

    int getSendBuffereSize() {
        return this.sendBufferSize;
    }

    long getPublicKeyLifetime() {
        return this.publicKeyLifetime;
    }

    long getCommonKeyLifetime() {
        return this.commonKeyLifetime;
    }

    int getBlacklistCapacity() {
        return this.blacklistCapacity;
    }

    long getBlacklistTimeout() {
        return this.blacklistTimeout;
    }

    int getPotCapacity() {
        return this.potCapacity;
    }

    long getOperationTimeout() {
        return this.operationTimeout;
    }

    int getMessageSizeLimit() {
        return this.messageSizeLimit;
    }

    AddressCalculator getAddressCalculator() {
        return this.addressCalculator;
    }

    int getActiveAddressLogCapacity() {
        return this.activeAddressLogCapacity;
    }

    long getActiveAddressDuration() {
        return this.activeAddressDuration;
    }

    int getBbsPort() {
        return this.bbsPort;
    }

    long getBbsConnectionTimeout() {
        return this.bbsConnectionTimeout;
    }

    long getBbsInternalTimeout() {
        return this.bbsInternalTimeout;
    }

    long getBbsUpdateThreshold() {
        return this.bbsUpdateThreshold;
    }

    boolean getGui() {
        return this.gui;
    }

    ExecutorService getExecutor() {
        return this.executor;
    }

    KeyPair loadId() throws IOException {
        KeyPair id = null;
        if (System.currentTimeMillis() <= this.idFile.lastModified() + this.idLifetime) {
            id = IdIo.fromFile(this.idFile);
        }
        if (id == null) {
            id = CryptographicKeys.newPublicKeyPair();
            LOG.log(Level.INFO, "個体識別鍵を作成しました。");
            IdIo.toFile(id, this.idFile);
        }
        return id;
    }

    List<AddressedPeer> loadAddressedPeers() {
        return PeerIo.addressedPeersFromFile(this.addressedPeerFile);
    }

    void storeAddressedPeers(final List<AddressedPeer> peers) throws IOException {
        PeerIo.addressedPeersToFile(peers, this.addressedPeerFile);
    }

    List<InetSocketAddress> loadPeers() {
        final List<InetSocketAddress> peers = PeerIo.peersFromTextFile(this.userPeerFile);
        peers.addAll(PeerIo.peersFromFile(this.peerFile));
        return peers;
    }

    void storePeers(final List<InetSocketAddress> peers) throws IOException {
        PeerIo.peersToFile(peers, this.peerFile);
    }

    /**
     * 自分の個体情報。
     * @author chirauraNoSakusha
     */
    static final class Self {
        private final InetSocketAddress raw;
        private final String mosaic;

        private Self(final InetSocketAddress raw, final String mosaic) {
            if (raw == null) {
                throw new IllegalArgumentException("Null raw.");
            } else if (mosaic == null) {
                throw new IllegalArgumentException("Null mosaic.");
            }
            this.raw = raw;
            this.mosaic = mosaic;
        }

        InetSocketAddress get() {
            return this.raw;
        }

        String getPublicForm() {
            return this.mosaic;
        }
    }

    Self loadSelf() {
        if (!this.selfFile.exists()) {
            return null;
        }
        try (BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(this.selfFile), Global.INTERNAL_CHARSET))) {
            final String line = buff.readLine();
            if (line == null || line.charAt(0) != '^') {
                return null;
            }
            final String mosaic = line.substring(1);
            final InetSocketAddress raw = Mosaic.peerFrom(mosaic);
            return new Self(raw, mosaic);
        } catch (IOException | MyRuleException e) {
            LOG.log(Level.INFO, "{0} の読み込みに失敗しました。", this.selfFile.getPath());
            return null;
        }
    }

    Self storeSelf(final InetSocketAddress self) throws IOException {
        if (!this.selfFile.exists()) {
            if (this.selfFile.createNewFile()) {
                LOG.log(Level.INFO, "{0} を作成しました。", this.selfFile.getPath());
            }
        }
        final String mosaic = Mosaic.peerTo(self);
        try (BufferedWriter buff = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.selfFile), Global.INTERNAL_CHARSET))) {
            buff.append('^');
            buff.append(mosaic);
            buff.flush();
        }
        return new Self(self, mosaic);
    }

    Map<String, String> getDefaultNames() {
        return NameIo.fromTextFile(this.bbsNameFile);
    }

    void startLogging() {
        startLogging(Global.ROOT_LOGGER);
    }

    void startLogging(final Logger logger) {
        synchronized (logger) {
            final Level level = (this.consoleLogLevel.intValue() < this.fileLogLevel.intValue() ? this.consoleLogLevel : this.fileLogLevel);

            for (final Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
                handler.close();
            }

            final List<Handler> handlers = new ArrayList<>();

            // 標準エラー出力。
            handlers.add(new ConsoleHandler());
            handlers.get(0).setLevel(this.consoleLogLevel);
            try {
                handlers.get(0).setFormatter(this.consoleLogFormatter.newInstance());
            } catch (SecurityException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // ファイル。
            if (this.fileLogLevel != Level.OFF) {
                try {
                    handlers.add(new FileHandler(this.fileLogPattern, this.fileLogLimit, this.fileLogCount, this.fileLogAppend));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                handlers.get(1).setLevel(this.fileLogLevel);
                try {
                    handlers.get(1).setFormatter(this.fileLogFormatter.newInstance());
                } catch (SecurityException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            logger.setLevel(level);
            logger.setUseParentHandlers(false);

            for (final Handler handler : handlers) {
                logger.addHandler(handler);
            }
        }
    }

}
