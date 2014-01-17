/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class UpdateChunkOperation implements Operation {

    private final Chunk.Id<? extends Mountain> id;
    private final long date;

    UpdateChunkOperation(final Chunk.Id<? extends Mountain> id, final long date) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.id = id;
        this.date = date;
    }

    Chunk.Id<? extends Mountain> getId() {
        return this.id;
    }

    long getDate() {
        return this.date;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof UpdateChunkOperation)) {
            return false;
        }
        /*
         * 1 つのデータ片、1 つの日時に対して 1 つしか実行しない。
         */
        final UpdateChunkOperation other = (UpdateChunkOperation) obj;
        return this.id.equals(other.id) && this.date == other.date;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(']').toString();
    }

}
