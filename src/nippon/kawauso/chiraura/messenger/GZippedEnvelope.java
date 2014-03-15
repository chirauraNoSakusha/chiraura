package nippon.kawauso.chiraura.messenger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * gzip で圧縮する封筒。
 * @author chirauraNoSakusha
 */
final class GZippedEnvelope implements Envelope {

    private final PlainEnvelope base;

    private GZippedEnvelope(final PlainEnvelope base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }
        this.base = base;
    }

    GZippedEnvelope(final List<Message> mail, final TypeRegistry<Message> registry) {
        this(new PlainEnvelope(mail, registry));
    }

    @Override
    public List<Message> getMail() {
        return this.base.getMail();
    }

    private static final int PLAIN_FLAG = 0;
    private static final int GZIP_FLAG = 1;

    private static byte[] getGZippedBytes(final byte[] plain) {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try (final GZIPOutputStream gZipper = new GZIPOutputStream(buff)) {
            gZipper.write(plain);
            gZipper.finish();
        } catch (final IOException ignored) {
            // ここには来ないはず。
        }
        return buff.toByteArray();
    }

    @Override
    public int byteSize() {
        /*
         * 実際に使うときはバイト列を保存する EncryptedEnvelope に入れるから問題無い。
         */
        final byte[] plain = BytesConversion.toBytes(this.base);
        final byte[] gZipped = getGZippedBytes(plain);
        if (plain.length <= gZipped.length) {
            return BytesConversion.byteSize("iab", PLAIN_FLAG, plain);
        } else {
            return BytesConversion.byteSize("iab", GZIP_FLAG, gZipped);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        final byte[] plain = BytesConversion.toBytes(this.base);
        final byte[] gZipped = getGZippedBytes(plain);
        if (plain.length <= gZipped.length) {
            return BytesConversion.toStream(output, "iab", PLAIN_FLAG, plain);
        } else {
            return BytesConversion.toStream(output, "iab", GZIP_FLAG, gZipped);
        }
    }

    static BytesConvertible.Parser<GZippedEnvelope> getParser(final TypeRegistry<Message> registry) {
        return new BytesConvertible.Parser<GZippedEnvelope>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GZippedEnvelope> output) throws MyRuleException,
                    IOException {
                final int[] flag = new int[1];
                final byte[][] bytes = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "iab", flag, bytes);

                final InputStream baseInput;
                if (flag[0] == PLAIN_FLAG) {
                    baseInput = new ByteArrayInputStream(bytes[0]);
                } else if (flag[0] == GZIP_FLAG) {
                    // 解凍。
                    try {
                        baseInput = new GZIPInputStream(new ByteArrayInputStream(bytes[0]));
                    } catch (final ZipException | EOFException e) {
                        throw new MyRuleException(e);
                    }
                } else {
                    throw new MyRuleException("Invalid flag ( " + flag[0] + " ).");
                }

                final List<PlainEnvelope> base = new ArrayList<>(1);
                PlainEnvelope.getParser(registry).fromStream(baseInput, Integer.MAX_VALUE, base);
                baseInput.close();

                output.add(new GZippedEnvelope(base.get(0)));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.base.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof GZippedEnvelope)) {
            return false;
        }
        final GZippedEnvelope other = (GZippedEnvelope) obj;
        return this.base.equals(other.base);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[").append(this.base)
                .append(']').toString();
    }

}
