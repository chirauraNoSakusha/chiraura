/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryOperation implements Operation {

    private final Chunk.Id<?> id;

    SimpleRecoveryOperation(final Chunk.Id<?> id) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.id = id;
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SimpleRecoveryOperation)) {
            return false;
        }
        /*
         * 1 つのデータ片に対して 1つしか実行しない。
         */
        final SimpleRecoveryOperation other = (SimpleRecoveryOperation) obj;
        return this.id.equals(other.id);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(']').toString();
    }

}
