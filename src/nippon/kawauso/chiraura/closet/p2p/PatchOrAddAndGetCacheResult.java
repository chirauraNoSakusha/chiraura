/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * 複製も考えた差分追加またはデータ片追加とデータ片取得の結果。
 * 1. データ片。
 * 2. 諦め。
 * @author chirauraNoSakusha
 */
final class PatchOrAddAndGetCacheResult {

    private final boolean givenUp;

    private final Mountain chunk;
    private final long accessDate;

    private PatchOrAddAndGetCacheResult(final boolean givenUp, final Mountain chunk, final long accessDate) {
        this.givenUp = givenUp;
        this.chunk = chunk;
        this.accessDate = accessDate;
    }

    static PatchOrAddAndGetCacheResult newGiveUp() {
        return new PatchOrAddAndGetCacheResult(true, null, 0);
    }

    static PatchOrAddAndGetCacheResult newNotFound(final long accessDate) {
        return new PatchOrAddAndGetCacheResult(false, null, accessDate);
    }

    PatchOrAddAndGetCacheResult(final Mountain chunk, final long accessDate) {
        this(false, chunk, accessDate);
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    Mountain getChunk() {
        return this.chunk;
    }

    long getAccessDate() {
        return this.accessDate;
    }

}
