package nippon.kawauso.chiraura.closet.alone;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Storage;
import nippon.kawauso.chiraura.storage.Storages;

/**
 * 単独稼動の押し入れ。
 * 試験用。
 * @author chirauraNoSakusha
 */
public final class StandAloneCloset implements Closet {

    private static final Logger LOG = Logger.getLogger(StandAloneCloset.class.getName());

    private final Storage storage;

    private final BlockingQueue<ClosetReport> errorQueue;

    private StandAloneCloset(final Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.storage = storage;
        this.errorQueue = new LinkedBlockingQueue<>();
    }

    /**
     * 作成する。
     * @param root 使用するディレクトリ
     * @param chunkSizeLimit データ片の保存時の許容サイズ (バイト)
     * @param directoryBitSize データ片の保存先のディレクトリ分けに使うビット数
     * @param chunkCacheCapacity データ片をキャッシュする数
     * @param indexCacheCapacity データ片の概要をキャッシュする数
     * @param rangeCacheCapacity データ片の概要の範囲取得結果をキャッシュする数
     */
    public StandAloneCloset(final File root, final int chunkSizeLimit, final int directoryBitSize, final int chunkCacheCapacity, final int indexCacheCapacity,
            final int rangeCacheCapacity) {
        this(Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity));
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
        this.storage.registerChunk(type, chunkClass, chunkParser, idClass, idParser);
    }

    @Override
    public void removeBackupType(final Class<? extends Chunk> chunkClass) {
        return;
    }

    @Override
    public <T extends Chunk> T getChunk(final Chunk.Id<T> id, final long timeout) throws InterruptedException {
        try {
            return this.storage.read(id);
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "{0} は壊れています。", id);
            return null;
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            return null;
        }
    }

    @Override
    public <T extends Chunk> T getChunkImmediately(final Chunk.Id<T> id) throws InterruptedException {
        try {
            return this.storage.read(id);
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "{0} は壊れています。", id);
            return null;
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            return null;
        }
    }

    @Override
    public boolean addChunk(final Chunk chunk, final long timeout) throws InterruptedException {
        this.storage.lock(chunk.getId());
        try {
            if (this.storage.contains(chunk.getId())) {
                return false;
            } else {
                try {
                    this.storage.write(chunk);
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", e);
                    return false;
                }
                return true;
            }
        } catch (final IOException e) { // contains で
            LOG.log(Level.WARNING, "異常が発生しました", e);
            return false;
        } finally {
            this.storage.unlock(chunk.getId());
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
    public <T extends Mountain> PatchResult<T> patchChunk(final Chunk.Id<T> id, final Mountain.Dust<T> diff, final long timeout) throws InterruptedException {
        this.storage.lock(id);
        try {
            final T chunk = this.storage.read(id);
            if (chunk == null) {
                return PatchResult.newNotFound();
            } else if (!chunk.patchable(diff)) {
                return PatchResult.newFailure(chunk);
            } else {
                @SuppressWarnings("unchecked")
                final T clone = (T) chunk.copy();
                if (clone.patch(diff)) {
                    this.storage.write(clone);
                    return new PatchResult<>(clone);
                } else {
                    return PatchResult.newFailure(chunk);
                }
            }
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "{0} は壊れています。", id);
            return PatchResult.newGiveUp();
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            return PatchResult.newGiveUp();
        } finally {
            this.storage.unlock(id);
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
        this.storage.lock(chunk.getId());
        try {
            @SuppressWarnings("unchecked")
            final Chunk.Id<T> id = (Chunk.Id<T>) chunk.getId();
            final T chunk0 = this.storage.read(id);
            if (chunk0 == null) {
                if (this.storage.write(chunk)) {
                    return new PatchOrAddResult<>(chunk);
                } else {
                    return PatchOrAddResult.newGiveUp();
                }
            } else if (!chunk0.baseEquals(chunk)) {
                return new PatchOrAddResult<>(chunk0);
            } else {
                @SuppressWarnings("unchecked")
                final T clone = (T) chunk0.copy();
                boolean success = false;
                for (final Mountain.Dust<?> diff : chunk.getDiffsAfter(Long.MIN_VALUE)) {
                    success |= clone.patch(diff);
                }
                if (success) {
                    this.storage.write(clone);
                    return new PatchOrAddResult<>(clone);
                } else {
                    return new PatchOrAddResult<>(chunk0);
                }
            }
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            LOG.log(Level.INFO, "{0} を上書きします。", chunk.getId());
            try {
                if (this.storage.write(chunk)) {
                    return new PatchOrAddResult<>(chunk);
                } else {
                    return PatchOrAddResult.newGiveUp();
                }
            } catch (final IOException e2) {
                LOG.log(Level.WARNING, "異常が発生しました", e2);
                return PatchOrAddResult.newGiveUp();
            }
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            return PatchOrAddResult.newGiveUp();
        } finally {
            this.storage.unlock(chunk.getId());
        }
    }

    @Override
    public ClosetReport takeError() throws InterruptedException {
        return this.errorQueue.take();
    }

    @Override
    public void start(final ExecutorService executor) {
        // することない。
    }

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException {
        this.storage.close();
    }

}
