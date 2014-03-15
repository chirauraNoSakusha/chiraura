package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class PatchChunkOperation<T extends Mountain> implements Operation {

    private final Chunk.Id<T> id;
    private final Mountain.Dust<T> diff;

    PatchChunkOperation(final Chunk.Id<T> id, final Mountain.Dust<T> diff) {
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.diff.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PatchChunkOperation)) {
            return false;
        }
        /*
         * 内容まで見る。
         * 1 つのデータ片に対しても、並列実行される。
         */
        final PatchChunkOperation<?> other = (PatchChunkOperation<?>) obj;
        return this.id.equals(other.id) && this.diff.equals(other.diff);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(this.diff)
                .append(']').toString();
    }

}
