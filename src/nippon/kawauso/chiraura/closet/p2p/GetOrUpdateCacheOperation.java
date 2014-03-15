package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class GetOrUpdateCacheOperation implements Operation {

    private final Chunk.Id<? extends Mountain> id;

    GetOrUpdateCacheOperation(final Chunk.Id<? extends Mountain> id) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.id = id;
    }

    Chunk.Id<? extends Mountain> getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(']').toString();
    }
}
