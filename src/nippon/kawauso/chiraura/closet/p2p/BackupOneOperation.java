package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.storage.Chunk;

/**
 * 複製用近接個体の 1 つに複製をつくる。
 * @author chirauraNoSakusha
 */
final class BackupOneOperation implements Operation {

    private final Chunk.Id<?> id;

    BackupOneOperation(final Chunk.Id<?> id) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.id = id;
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof BackupOneOperation)) {
            return false;
        }
        final BackupOneOperation other = (BackupOneOperation) obj;
        /*
         * 1 つのデータ片に対して 1 つしか実行しない。
         */
        return this.id.equals(other.id);
    }

}
