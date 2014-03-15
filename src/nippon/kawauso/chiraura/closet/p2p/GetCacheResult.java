package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.storage.Chunk;

/**
 * 複製も考えたデータ片取得の結果。
 * 1. データ片。
 * 2. 不在。データ片が無かった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class GetCacheResult {

    private final boolean givenUp;

    private final Chunk chunk;
    private final long accessDate;

    private GetCacheResult(final boolean givenUp, final Chunk chunk, final long accessDate) {
        this.givenUp = givenUp;
        this.chunk = chunk;
        this.accessDate = accessDate;
    }

    static GetCacheResult newGiveUp() {
        return new GetCacheResult(true, null, 0);
    }

    static GetCacheResult newNotFound(final long accessDate) {
        return new GetCacheResult(false, null, accessDate);
    }

    GetCacheResult(final Chunk chunk, final long accessDate) {
        this(false, chunk, accessDate);
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isNotFound() {
        return this.chunk == null;
    }

    Chunk getChunk() {
        return this.chunk;
    }

    long getAccessDate() {
        return this.accessDate;
    }

}
