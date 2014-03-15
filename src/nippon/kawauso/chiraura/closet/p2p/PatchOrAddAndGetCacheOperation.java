package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * @author chirauraNoSakusha
 */
final class PatchOrAddAndGetCacheOperation implements Operation {

    private final Mountain chunk;

    PatchOrAddAndGetCacheOperation(final Mountain chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
        this.chunk = chunk;
    }

    Mountain getChunk() {
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
        } else if (!(obj instanceof PatchOrAddAndGetCacheOperation)) {
            return false;
        }
        /*
         * 内容まで見る。
         * 1 つのデータ片に対しても、並列実行される。
         */
        final PatchOrAddAndGetCacheOperation other = (PatchOrAddAndGetCacheOperation) obj;
        return this.chunk.equals(other.chunk);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.chunk.getId())
                .append(']').toString();
    }

}
