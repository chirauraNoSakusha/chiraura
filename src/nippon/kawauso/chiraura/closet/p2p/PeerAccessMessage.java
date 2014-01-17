/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * 個体確認のための言付け。
 * @author chirauraNoSakusha
 */
enum PeerAccessMessage implements Message {

    /**
     * 唯一のインスタンス。
     */
    INSTANCE,

    ;

    /*
     * 個体確認の旨だけ伝われば良いので中身は無い。
     */

    @Override
    public int byteSize() {
        return 0;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return 0;
    }

    static BytesConvertible.Parser<PeerAccessMessage> getParser() {
        return new BytesConvertible.Parser<PeerAccessMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PeerAccessMessage> output) throws MyRuleException,
                    IOException {
                output.add(INSTANCE);
                return 0;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[]").toString();
    }

}
