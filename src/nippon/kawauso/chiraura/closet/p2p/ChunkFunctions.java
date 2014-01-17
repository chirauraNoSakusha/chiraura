/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class ChunkFunctions {

    // インスタンス化防止。
    private ChunkFunctions() {}

    static HashValue calculateHashValue(final Chunk chunk) {
        return HashValue.calculateFromBytes(BytesConversion.toBytes(chunk));
    }

}
