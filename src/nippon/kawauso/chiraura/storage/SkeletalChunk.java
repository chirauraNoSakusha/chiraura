package nippon.kawauso.chiraura.storage;

import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;

/**
 * 土台。
 * @author chirauraNoSakusha
 */
public abstract class SkeletalChunk implements Chunk {

    @Override
    public HashValue getHashValue() {
        return HashValue.calculateFromBytes(BytesConversion.toBytes(this));
    }

}
