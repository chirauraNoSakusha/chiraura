/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.storage.Chunk;

/**
 * データ片取得の結果。
 * 1. データ片。
 * 2. 不在。データ片が無かった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class GetChunkResult {

    private final boolean givenUp;

    private final Chunk chunk;

    private GetChunkResult(final boolean givenUp, final Chunk chunk) {
        this.givenUp = givenUp;
        this.chunk = chunk;
    }

    static GetChunkResult newGiveUp() {
        return new GetChunkResult(true, null);
    }

    static GetChunkResult newNotFound() {
        return new GetChunkResult(false, null);
    }

    GetChunkResult(final Chunk chunk) {
        this(false, chunk);
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

}
