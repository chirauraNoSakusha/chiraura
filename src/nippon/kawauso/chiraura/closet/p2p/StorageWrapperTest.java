package nippon.kawauso.chiraura.closet.p2p;

import java.io.File;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.storage.Storages;

/**
 * @author chirauraNoSakusha
 */
public final class StorageWrapperTest {

    static StorageWrapper sample(@SuppressWarnings("unused") final Random random, final BlockingQueue<Operation> operationSink) {
        final File root = new File(System.getProperty("java.io.tmpdir") + File.separator + StorageWrapperTest.class.getSimpleName() + File.separator
                + System.nanoTime());
        final int chunkSizeLimit = 1024 * 1024;
        final int directoryBitSize = 8;
        final int chunkCacheCapacity = 100;
        final int indexCacheCapacity = 10_000;
        final int rangeCacheCapacity = 10_000;
        final int cacheLogCapacity = 1_000;
        final long cacheDuration = 30 * Duration.SECOND;
        return new StorageWrapper(Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity),
                operationSink, cacheLogCapacity, cacheDuration);
    }

}
