/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 同期していないが、toStream やらを利用するスレッドを開始する前に register してしまえば、問題無い。
 * @author chirauraNoSakusha
 */
final class Transceiver {

    private static final Logger LOG = Logger.getLogger(Transceiver.class.getName());

    private final TransceiverShare share;
    private final InputStream input;
    private final OutputStream output;

    Transceiver(final TransceiverShare share, final InputStream input, final OutputStream output) {
        if (share == null) {
            throw new IllegalArgumentException("Null share.");
        } else if (input == null) {
            throw new IllegalArgumentException("Null input.");
        } else if (output == null) {
            throw new IllegalArgumentException("Null output.");
        }
        this.share = share;
        this.input = input;
        this.output = output;
    }

    /**
     * fromStream で読み込めるようにメッセージを書き込む。
     * @param output 書き込み先
     * @param mail メッセージ
     * @param type 使用する封筒の種別
     * @param encryptionKey 暗号鍵
     * @return 書き込みサイズ
     * @throws IOException 書き込みエラー
     */
    int toStream(final List<Message> mail, final Class<? extends Envelope> type, final Key encryptionKey) throws IOException {
        /*
         * {封書の種別}{封書のサイズ}{封書} の形で書き込む。
         */
        final Long id = this.share.typeToId.get(type);
        if (id == null) {
            throw new IllegalStateException("Not registered type ( " + type + " ).");
        }

        final TransceiverShare.Sealer<?> sealer = this.share.idToSealer.get(id);
        final Envelope envelope = sealer.seal(mail, encryptionKey);
        final int size = envelope.byteSize();
        if (size > this.share.maxSize) {
            throw new IllegalArgumentException("Invalid size ( " + size + " ) not in [ 0, " + this.share.maxSize + " ].");
        }
        return BytesConversion.toStream(this.output, "lio", id.longValue(), size, envelope);
    }

    /**
     * toStream で書き込まれたメッセージを読み込む。
     * @param input 読み込み元
     * @param decryptionKey 暗号鍵
     * @param sink 読み込んだメッセージの格納先
     * @return 読み込みサイズ
     * @throws MyRuleException 規約違反
     * @throws IOException 読み込みエラー
     */
    int fromStream(final Key decryptionKey, final List<? super Message> sink) throws MyRuleException, IOException {
        while (!Thread.currentThread().isInterrupted()) {
            // 封筒の種別とサイズ。
            final long[] id = new long[1];
            final int[] size = new int[1];
            final int headerSize = BytesConversion.fromStream(this.input, this.share.maxSize, "li", id, size);

            if (size[0] < 0 || this.share.maxSize < size[0]) {
                throw new MyRuleException("Invalid size ( " + size[0] + " ) not in [ 0, " + this.share.maxSize + " ].");
            }

            final TransceiverShare.ParserGenerator<?> parserGenerator = this.share.idToParserGenerator.get(id[0]);
            if (parserGenerator != null) {
                final List<Envelope> envelope = new ArrayList<>(1);
                final int bodySize = parserGenerator.getParser(decryptionKey).fromStream(this.input, size[0], envelope);
                if (bodySize != size[0]) {
                    throw new MyRuleException("Invalid mail size ( " + bodySize + " ) differing from declared size ( " + size[0] + " ).");
                }
                sink.addAll(envelope.get(0).getMail());
            } else {
                StreamFunctions.completeSkip(this.input, size[0]);
                LOG.log(Level.WARNING, "不明な封筒種別 ( {0} ) なので {1} バイト分無視します。", new Object[] { id[0], size[0] });
            }
            return headerSize + size[0];
        }
        throw new MyRuleException("Interrupted");
    }

    boolean isEof() throws IOException {
        return StreamFunctions.isEof(this.input);
    }

    void flush() throws IOException {
        this.output.flush();
    }

}
