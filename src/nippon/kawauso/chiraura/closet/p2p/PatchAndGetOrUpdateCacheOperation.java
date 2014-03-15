package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class PatchAndGetOrUpdateCacheOperation<T extends Mountain> implements Operation {

    private final Chunk.Id<T> id;
    final Mountain.Dust<T> diff;

    PatchAndGetOrUpdateCacheOperation(final Chunk.Id<T> id, final Mountain.Dust<T> diff) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (diff == null) {
            throw new IllegalArgumentException("Null diff.");
        }
        this.id = id;
        this.diff = diff;
    }

    Chunk.Id<T> getId() {
        return this.id;
    }

    Mountain.Dust<T> getDiff() {
        return this.diff;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(this.diff)
                .append(']').toString();
    }

}
