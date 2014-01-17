/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class AddCacheOperation implements Operation {

    private final Chunk chunk;

    AddCacheOperation(final Chunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
        this.chunk = chunk;
    }

    Chunk getChunk() {
        return this.chunk;
    }

    @Override
    public int hashCode() {
        return this.chunk.getId().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddCacheOperation)) {
            return false;
        }
        /*
         * 内容まで見る。
         * 1 つのデータ片に対しても、並列実行される。
         */
        final AddCacheOperation other = (AddCacheOperation) obj;
        return this.chunk.equals(other.chunk);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.chunk.getId())
                .append(']').toString();
    }

}
