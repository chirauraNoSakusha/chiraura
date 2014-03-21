package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Storage;

/**
 * 押し入れように取り繕った記録装置。
 * @author chirauraNoSakusha
 */
final class StorageWrapper {

    private static final Logger LOG = Logger.getLogger(StorageWrapper.class.getName());

    private final Storage base;
    private final BlockingQueue<Operation> operationSink;

    private final TypeRegistry<Mountain.Dust<?>> diffRegistry;
    private final CacheLog cacheLog;

    StorageWrapper(final Storage base, final BlockingQueue<Operation> operationSink, final int cacheLogLimit, final long cacheDuration) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        } else if (operationSink == null) {
            throw new IllegalArgumentException("Null operation sink.");
        } else if (cacheLogLimit < 0) {
            throw new IllegalArgumentException("Negative cache log limit ( " + cacheLogLimit + " ).");
        } else if (cacheDuration < 0) {
            throw new IllegalArgumentException("Negative cache duration ( " + cacheDuration + " ).");
        }

        this.base = base;
        this.operationSink = operationSink;
        this.diffRegistry = TypeRegistries.newRegistry();

        this.cacheLog = new CacheLog(cacheLogLimit, cacheDuration);
    }

    <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser,
            final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser) {
        this.base.registerChunk(type, chunkClass, chunkParser, idClass, idParser);
    }

    <C extends Mountain, I extends Chunk.Id<C>, D extends Mountain.Dust<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser,
            final Class<D> diffClass, final BytesConvertible.Parser<? extends D> diffParser) {
        this.base.registerChunk(type, chunkClass, chunkParser, idClass, idParser);
        this.diffRegistry.register(type, diffClass, diffParser);
    }

    public TypeRegistry<Chunk> getChunkRegistry() {
        return this.base.getChunkRegistry();
    }

    public TypeRegistry<Chunk.Id<?>> getIdRegistry() {
        return this.base.getIdRegistry();
    }

    public TypeRegistry<Mountain.Dust<?>> getDiffRegistry() {
        return this.diffRegistry;
    }

    /**
     * データ片の存在を調べる。
     * @param id データ片の識別子
     * @return 存在する場合のみ true
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれた場合
     */
    boolean contains(final Chunk.Id<?> id) throws IOException, InterruptedException {
        return this.base.contains(id);
    }

    /**
     * データ片の概要を取得する。
     * @param id データ片の識別子
     * @return データ片の概要。
     *         データ片が無い場合は null
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれ
     */
    Storage.Index getIndex(final Chunk.Id<?> id) throws IOException, InterruptedException {
        try {
            return this.base.getIndex(id);
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            this.base.delete(id);
            LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
            ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
            // TODO 管理者なら取り寄せ。
            return null;
        }
    }

    /**
     * データ片の概要を列挙する。
     * @param start 列挙する論理位置の始点
     * @param end 列挙する論理位置の終点
     * @return 指定範囲内のデータ片の概要
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれ
     */
    Collection<Storage.Index> getIndices(final Address start, final Address end) throws IOException, InterruptedException {
        if (start.compareTo(end) <= 0) {
            // start <= end.
            return this.base.getIndices(start, end).values();
        } else if (end.equals(start.subtractOne())) {
            // 全域。
            return this.base.getIndices(Address.ZERO, Address.MAX).values();
        } else {
            // 0 <= end < start <= MAX.
            final Map<Chunk.Id<?>, Storage.Index> indices = this.base.getIndices(Address.ZERO, end);
            indices.putAll(this.base.getIndices(start, Address.MAX));
            return indices.values();
        }
    }

    /**
     * データ片を読み込む。
     * @param <T> データ片の型
     * @param id 読み込むデータ片の識別子
     * @return 読み込んだデータ片。
     *         データ片が無い場合は null
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれた場合
     */
    <T extends Chunk> T read(final Chunk.Id<T> id) throws IOException, InterruptedException {
        this.base.lock(id);
        try {
            return this.base.read(id);
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            this.base.delete(id);
            LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
            ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
            return null;
        } finally {
            this.base.unlock(id);
        }
    }

    /**
     * 差分を適用する。
     * @param <T> 対象のデータ片の型
     * @param before 適用対象
     * @param diffs 差分
     * @return 適用後のデータ片。
     *         1 つも適用できる差分が無かった場合は null
     */
    private static <T extends Mountain> T patchChunk(final T before, final List<? extends Mountain.Dust<T>> diffs) {
        int i = 0;
        for (; i < diffs.size(); i++) {
            if (before.patchable(diffs.get(i))) {
                break;
            }
        }

        if (i >= diffs.size()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final T after = (T) before.copy();
        for (; i < diffs.size(); i++) {
            after.patch(diffs.get(i));
        }
        return after;
    }

    /**
     * 操作結果。
     * 操作対象が無かったら、isNotFound は true。
     * 操作が成功したら isSuccess が true。
     * 操作の成否に関わらず、getChunk は操作後の操作対象を返す。
     * isNotFound が true の場合の getChunk は未定義。
     * @author chirauraNoSakusha
     * @param <T> データ片の型
     */
    static final class Result<T extends Chunk> {
        private final boolean success;
        private final T chunk;

        private static <T extends Mountain> Result<T> newNotFound() {
            return new Result<>(false, null);
        }

        private Result(final boolean success, final T chunk) {
            this.success = success;
            this.chunk = chunk;
        }

        boolean isNotFound() {
            return this.chunk == null;
        }

        boolean isSuccess() {
            return this.success;
        }

        T getChunk() {
            return this.chunk;
        }
    }

    /**
     * データ片が無いなら書き込む。
     * データ片が既に存在しても基礎が違うなら何もしない。
     * 基礎が同じなら全差分を加える。
     *
     * <pre>
     * isNotFound isSuccess
     * X          O         書き換えた。
     * X          X         書き換えなかった。
     * </pre>
     * @param <T> データ片の型
     * @param chunk 差分源のデータ片
     * @return 結果。
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 読み書き異常
     */
    <T extends Mountain> Result<T> patchOrWriteAndRead(final T chunk) throws InterruptedException, IOException {
        this.base.lock(chunk.getId());
        try {
            if (this.base.contains(chunk.getId())) {
                final T old;
                try {
                    @SuppressWarnings("unchecked")
                    final Chunk.Id<T> id = (Chunk.Id<T>) chunk.getId();
                    old = this.base.read(id);
                } catch (final MyRuleException e) {
                    this.base.forceWrite(chunk);
                    return new Result<>(true, chunk);
                }

                if (old.baseEquals(chunk)) {
                    // 基礎が同じ場合は全差分適用。
                    @SuppressWarnings("unchecked")
                    final List<Mountain.Dust<T>> diffs = (List<Mountain.Dust<T>>) chunk.getDiffsAfter(Long.MIN_VALUE);
                    final T after = patchChunk(old, diffs);
                    if (after == null) {
                        return new Result<>(false, old);
                    } else {
                        this.base.forceWrite(after);
                        return new Result<>(true, after);
                    }
                } else {
                    // 基礎から違う場合は何もしない。
                    return new Result<>(false, old);
                }
            } else {
                // 無い場合は追加。
                this.base.forceWrite(chunk);
                return new Result<>(true, chunk);
            }
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    /**
     * データ片が無いなら書き込む。
     * データ片が既に存在しても基礎が違うなら上書きする。
     * 差分形式で基礎が同じなら全差分を加える。
     *
     * <pre>
     * isNotFound isSuccess
     * X          O         書き換えた。
     * X          X         書き換えなかった。
     * </pre>
     * @param <T> データ片の型
     * @param chunk 差分源のデータ片
     * @return 結果。
     *         書き込んだか 1 つでも差分が適用できたら isSuccess が true
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 書き込み異常
     */
    <T extends Mountain> Result<T> forcePatchOrWriteAndRead(final T chunk) throws InterruptedException, IOException {
        this.base.lock(chunk.getId());
        try {
            if (this.base.contains(chunk.getId())) {
                final T old;
                try {
                    @SuppressWarnings("unchecked")
                    final Chunk.Id<T> id = (Chunk.Id<T>) chunk.getId();
                    old = this.base.read(id);
                } catch (final MyRuleException e) {
                    this.base.forceWrite(chunk);
                    return new Result<>(true, chunk);
                }

                if (old.baseEquals(chunk)) {
                    // 基礎が同じ場合は全差分適用。
                    @SuppressWarnings("unchecked")
                    final List<Mountain.Dust<T>> diffs = (List<Mountain.Dust<T>>) chunk.getDiffsAfter(Long.MIN_VALUE);
                    final T after = patchChunk(old, diffs);
                    if (after == null) {
                        return new Result<>(false, old);
                    } else {
                        this.base.forceWrite(after);
                        return new Result<>(true, after);
                    }
                } else {
                    // 基礎から違う場合は上書き。
                    this.base.forceWrite(chunk);
                    return new Result<>(true, chunk);
                }
            } else {
                // 無い場合は追加。
                this.base.forceWrite(chunk);
                return new Result<>(true, chunk);
            }
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    /**
     * データ片が無いなら書き込む。
     * データ片が既に存在して、差分形式でないなら上書きする。
     * データ片が既に存在して、差分形式で基礎が違うなら何もしない。
     * 差分形式で基礎が同じなら全差分を加える。
     *
     * <pre>
     * isNotFound isSuccess
     * X          O         書き換えた。
     * X          X         書き換えなかった。
     * </pre>
     * @param chunk 書き込むデータ片
     * @return 書き込んだ場合のみ true。
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    <T extends Chunk> Result<T> update(final T chunk) throws IOException, InterruptedException {
        if (chunk instanceof Mountain) {
            @SuppressWarnings("unchecked")
            final Result<T> result = (Result<T>) patchOrWriteAndRead((Mountain) chunk);
            return result;
        } else {
            try {
                return new Result<>(this.base.write(chunk), chunk);
            } finally {
                this.base.unlock(chunk.getId());
            }
        }
    }

    /**
     * データ片が無いなら書き込む。
     * データ片が既に存在して、差分形式でない、または、差分形式でも基礎が違うなら上書きする。
     * 差分形式で基礎が同じなら全差分を加える。
     *
     * <pre>
     * isNotFound isSuccess
     * X          O         書き換えた。
     * X          X         書き換えなかった。
     * </pre>
     * @param chunk 書き込むデータ片
     * @return 書き込んだ場合のみ true。
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    <T extends Chunk> Result<T> forceUpdate(final T chunk) throws IOException, InterruptedException {
        if (chunk instanceof Mountain) {
            @SuppressWarnings("unchecked")
            final Result<T> result = (Result<T>) forcePatchOrWriteAndRead((Mountain) chunk);
            return result;
        } else {
            // 差分形式でない。
            this.base.lock(chunk.getId());
            try {
                return new Result<>(this.base.write(chunk), chunk);
            } finally {
                this.base.unlock(chunk.getId());
            }
        }
    }

    /**
     * データ片を書き込む。
     * 既にデータ片がある場合、書き込まない。
     * @param chunk 書き込むデータ片
     * @return 書き込んだ場合のみ true。
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    boolean weakWrite(final Chunk chunk) throws IOException, InterruptedException {
        this.base.lock(chunk.getId());
        try {
            if (this.base.contains(chunk.getId())) {
                return false;
            } else {
                this.base.forceWrite(chunk);
                return true;
            }
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    /**
     * データ片を書き込む。
     * 既にデータ片がある場合、上書きする。
     * 既に同じのがあるなら書き込まない。
     * @param chunk 書き込むデータ片
     * @return 書き換えたら
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    boolean forceWrite(final Chunk chunk) throws IOException, InterruptedException {
        this.base.lock(chunk.getId());
        try {
            return this.base.write(chunk);
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    /**
     * 差分を適用する。
     *
     * <pre>
     * isNotFound isSuccess
     * O          -         対象が無かった。
     * X          O         書き換えた。
     * X          X         書き換えなかった。
     * </pre>
     * @param id 適用先のデータ片の識別子
     * @param diff 適用する差分
     * @return 適用結果
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 読み込み異常
     */
    <T extends Mountain> Result<T> patch(final Chunk.Id<T> id, final Mountain.Dust<T> diff) throws InterruptedException, IOException {
        this.base.lock(id);
        try {
            final T before;
            try {
                before = this.base.read(id);
            } catch (final MyRuleException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                this.base.delete(id);
                LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
                ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
                return Result.newNotFound();
            }

            if (before == null) {
                return Result.newNotFound();
            } else if (!before.patchable(diff)) {
                return new Result<>(false, before);
            } else {
                @SuppressWarnings("unchecked")
                final T after = (T) before.copy();
                after.patch(diff);
                this.base.forceWrite(after);
                return new Result<>(true, after);
            }
        } finally {
            this.base.unlock(id);
        }
    }

    /**
     * 差分を適用する。
     *
     * <pre>
     * isNotFound isSuccess
     * O          -         対象が無かった。
     * X          O         書き換えた。
     * X          X         書き換えなかった。
     * </pre>
     * @param id 適用先のデータ片の識別子
     * @param diffs 適用する差分
     * @return 適用結果
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 読み込み異常
     */
    <T extends Mountain> Result<T> patch(final Chunk.Id<T> id, final List<? extends Mountain.Dust<T>> diffs) throws InterruptedException, IOException {
        this.base.lock(id);
        try {
            T before;
            try {
                before = this.base.read(id);
            } catch (final MyRuleException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                this.base.delete(id);
                LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
                ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
                return Result.newNotFound();
            }

            if (before == null) {
                return Result.newNotFound();
            }

            final T after = patchChunk(before, diffs);
            if (after == null) {
                return new Result<>(false, before);
            } else {
                this.base.forceWrite(after);
                return new Result<>(true, after);
            }
        } finally {
            this.base.unlock(id);
        }
    }

    /**
     * 複製操作の結果。
     * 新鮮なデータ片か新鮮な不在情報がある場合、hasInfo が true
     * 新鮮な不在情報がある場合 isNotFound は true。
     * getChunk は操作後の複製を返す。
     * getAccessDate は複製された日時を返す。
     * @author chirauraNoSakusha
     * @param <T> データ片の型
     */
    static final class CacheResult<T extends Chunk> {
        private final boolean info;
        private final boolean success;

        private final T chunk;
        private final long accessDate;

        private CacheResult(final boolean info, final boolean success, final T chunk, final long accessDate) {
            this.info = info;
            this.success = success;
            this.chunk = chunk;
            this.accessDate = accessDate;
        }

        private static <S extends Chunk> CacheResult<S> newNoInfo() {
            return new CacheResult<>(false, false, null, 0);
        }

        private static <S extends Chunk> CacheResult<S> newNoFound(final long accessDate) {
            return new CacheResult<>(true, false, null, accessDate);
        }

        private CacheResult(final boolean success, final T chunk, final long accessDate) {
            this(true, success, chunk, accessDate);
        }

        boolean hasInfo() {
            return this.info;
        }

        boolean isNotFound() {
            return this.chunk == null;
        }

        boolean isSuccess() {
            return this.success;
        }

        T getChunk() {
            return this.chunk;
        }

        long getAccessDate() {
            return this.accessDate;
        }
    }

    /**
     * 新鮮な複製か、新鮮な不在情報があるかどうか調べる。
     * @param id 対象のデータ片の識別子
     * @return 新鮮な複製、または、新鮮な不在情報がある場合のみ true
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    boolean containsCache(final Chunk.Id<?> id) throws InterruptedException, IOException {
        this.base.lock(id);
        try {
            return this.cacheLog.contains(id);
        } finally {
            this.base.unlock(id);
        }
    }

    /**
     * 新鮮な複製を読み込む。
     *
     * <pre>
     * hasInfo isNotFound isSuccess
     * O       O          -         新鮮な不在情報がある。
     * O       X          -         新鮮な複製がある。
     * X       -          -         新鮮な複製も新鮮な不在情報も無い。
     * </pre>
     * @param <T> データ片の型
     * @param id 対象のデータ片の識別子
     * @return 結果
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    <T extends Chunk> CacheResult<T> readCache(final Chunk.Id<T> id) throws IOException, InterruptedException {
        this.base.lock(id);
        try {
            final CacheLog.Entry entry = this.cacheLog.get(id);
            if (entry == null) {
                return CacheResult.newNoInfo();
            } else if (entry.isNotFound()) {
                return CacheResult.newNoFound(entry.getAccessDate());
            }

            final T chunk;
            try {
                chunk = this.base.read(id);
            } catch (final MyRuleException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                this.base.delete(id);
                LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
                ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
                return CacheResult.newNoInfo();
            }
            return new CacheResult<>(true, chunk, entry.getAccessDate());
        } finally {
            this.base.unlock(id);
        }
    }

    /**
     * 複製を保存する。
     * ただし、より新しい複製がある場合は何もしない。
     * 不在情報ならより新しくても無視して保存する。
     *
     * <pre>
     * hasInfo isNotFound isSuccess
     * O       X          O         書き換えた。
     * O       X          X         書き換えなかった。
     * </pre>
     * @param chunk 複製
     * @param accessDate 複製された時刻
     * @return 結果
     * @throws IOException 書き込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    <T extends Chunk> CacheResult<T> forceWriteCache(final T chunk, final long accessDate) throws IOException, InterruptedException {
        this.base.lock(chunk.getId());
        try {
            final CacheLog.Entry entry = this.cacheLog.get(chunk.getId());
            if (entry != null && !entry.isNotFound() && accessDate < entry.getAccessDate()) {
                T cur = null;
                @SuppressWarnings("unchecked")
                final Chunk.Id<T> id = (Chunk.Id<T>) chunk.getId();
                try {
                    cur = this.base.read(id);
                } catch (final MyRuleException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", e);
                    this.base.delete(chunk.getId());
                    LOG.log(Level.INFO, "壊れていた {0} を削除しました。", chunk.getId());
                    ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
                }

                if (cur != null) {
                    return new CacheResult<>(false, cur, entry.getAccessDate());
                }
            }

            this.cacheLog.add(chunk.getId(), accessDate);
            return new CacheResult<>(this.base.write(chunk), chunk, accessDate);
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    /**
     * 不在情報を登録する。
     * ただし、新鮮な複製がある場合は何もしない。
     *
     * <pre>
     * hasInfo isNotFound isSuccess
     * O       O          O         登録した。
     * O       O          X         もっと新しい不在情報があった。
     * O       X          X         新鮮な複製があった。
     * </pre>
     * @param <T> データ片の型
     * @param id データ片の識別子
     * @param accessDate 情報の発生時間
     * @return 結果
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれ
     */
    <T extends Chunk> CacheResult<T> addNotFoundCache(final Chunk.Id<T> id, final long accessDate) throws IOException, InterruptedException {
        this.base.lock(id);
        try {
            final CacheLog.Entry entry = this.cacheLog.get(id);
            if (entry != null) {
                if (!entry.isNotFound()) {
                    T cur = null;
                    try {
                        cur = this.base.read(id);
                    } catch (final MyRuleException e) {
                        LOG.log(Level.WARNING, "異常が発生しました", e);
                        this.base.delete(id);
                        LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
                        ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
                    }
                    if (cur != null) {
                        return new CacheResult<>(false, cur, entry.getAccessDate());
                    }
                } else if (accessDate < entry.getAccessDate()) {
                    return CacheResult.newNoFound(entry.getAccessDate());
                }
            }

            this.cacheLog.addNotFound(id, accessDate);
            return CacheResult.newNoFound(accessDate);
        } finally {
            this.base.unlock(id);
        }
    }

    /**
     * 複製に差分を適用する。
     * ただし、複製が無い、または、複製の方が新しい場合は何もしない。
     * 差分を適用した後のハッシュ値が hashValue と異なる場合も何もしない。
     *
     * <pre>
     * hasInfo isNotFound isSuccess
     * O       O          -         新鮮な不在情報があった。
     * O       X          O         書き換えた。
     * O       X          X         新鮮な複製があり、かつ、書き換えなかった。
     * X       O          -         対象が無かった。
     * X       X          X         新鮮な複製が無く、かつ、書き換えなかった。
     * </pre>
     * @param <T> データ片の型
     * @param id データ片の識別子
     * @param diffs 差分
     * @param hashValue 適用後の正しいハッシュ値
     * @param accessDate 差分の発行日時
     * @return 結果
     * @throws InterruptedException 割り込まれ
     * @throws IOException 読み書き異常
     */
    <T extends Mountain> CacheResult<T> patchCache(final Chunk.Id<T> id, final List<? extends Mountain.Dust<T>> diffs, final HashValue hashValue,
            final long accessDate) throws InterruptedException, IOException {
        this.base.lock(id);
        try {
            final CacheLog.Entry entry = this.cacheLog.get(id);
            if (entry != null && entry.isNotFound()) {
                return CacheResult.newNoFound(entry.getAccessDate());
            }

            T before;
            try {
                before = this.base.read(id);
            } catch (final MyRuleException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                this.base.delete(id);
                LOG.log(Level.INFO, "壊れていた {0} を削除しました。", id);
                ConcurrentFunctions.completePut(new SimpleRecoveryOperation(id), this.operationSink);
                return CacheResult.newNoInfo();
            }

            if (before == null) {
                return CacheResult.newNoInfo();
            }

            final T after = patchChunk(before, diffs);
            if (after == null) {
                if (before.getHashValue().equals(hashValue)) {
                    this.cacheLog.add(id, accessDate);
                }
                return new CacheResult<>(false, before, accessDate);
            } else if (after.getHashValue().equals(hashValue)) {
                this.base.forceWrite(after);
                this.cacheLog.add(id, accessDate);
                return new CacheResult<>(true, after, accessDate);
            } else {
                return new CacheResult<>(false, before, 0);
            }
        } finally {
            this.base.unlock(id);
        }
    }

    void close() throws MyRuleException, InterruptedException, IOException {
        this.base.close();
    }

}
