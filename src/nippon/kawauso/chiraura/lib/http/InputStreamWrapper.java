/**
 * 
 */
package nippon.kawauso.chiraura.lib.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import nippon.kawauso.chiraura.lib.BytesFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * テキスト読み込みとバイト列読み込みを両備した InputStream もどき。
 * @author chirauraNoSakusha
 */
public final class InputStreamWrapper implements AutoCloseable {

    private final InputStream base;
    private final Charset charset;
    private final byte[] separator;
    private final byte[] buff;

    // base から読み込み buff に格納されているバイトサイズ。
    private int readSize;

    /**
     * InputStream もどきを作成する。
     * @param base 元にする入力
     * @param charset 文字コード
     * @param separator 改行
     * @param lineSize 1 行の許容バイトサイズ
     */
    public InputStreamWrapper(final InputStream base, final Charset charset, final String separator, final int lineSize) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        } else if (charset == null) {
            throw new IllegalArgumentException("Null charset.");
        } else if (separator == null) {
            throw new IllegalArgumentException("Null separator.");
        } else if (lineSize <= 0) {
            throw new IllegalArgumentException("Negative line size ( " + lineSize + " ).");
        }

        this.base = base;
        this.charset = charset;
        this.separator = separator.getBytes(charset);
        this.buff = new byte[lineSize + separator.length()];
        this.readSize = 0;
    }

    /**
     * 指定したバイト数だけ全部読む。
     * @param output 読んだデータを格納するところ
     * @throws IOException 読み込み異常
     * @throws MyRuleException 読み込めるデータが足りなかった場合
     */
    public void completeRead(final byte[] output) throws IOException, MyRuleException {
        if (output.length <= this.readSize) {
            // 全部読み込み済み。

            // 出力にコピー。
            System.arraycopy(this.buff, 0, output, 0, output.length);

            // 余りを詰める。
            System.arraycopy(this.buff, output.length, this.buff, 0, this.readSize - output.length);
            this.readSize = this.readSize - output.length;
        } else {
            // 読み込み済みの分をコピー。
            System.arraycopy(this.buff, 0, output, 0, this.readSize);

            // 不足分を直接読み込む。
            for (int i = this.readSize; i < output.length;) {
                final int size = this.base.read(output, i, output.length - i);
                if (size < 0) {
                    throw new MyRuleException("Read size ( " + i + " ) is shorter than required size ( " + output.length + " ).");
                }
                i += size;
            }
            this.readSize = 0;
        }
    }

    /**
     * 1 行読む。
     * @return 1 行
     * @throws IOException 読み込み異常
     * @throws MyRuleException 1 行が許容サイズを超えた場合
     */
    public String readLine() throws IOException, MyRuleException {
        int tail = 0;
        while (true) {
            final int index = BytesFunctions.indexOf(this.buff, this.separator, tail, this.readSize);
            if (index >= 0) {
                // 改行があった。
                final String line = new String(this.buff, 0, index, this.charset);
                final int usedLength = index + this.separator.length;
                System.arraycopy(this.buff, usedLength, this.buff, 0, this.readSize - usedLength);
                this.readSize -= usedLength;
                return line;
            }

            if (this.readSize == this.buff.length) {
                // 行が長過ぎる。
                throw new MyRuleException("Too long line over limit ( " + (this.buff.length - this.separator.length) + " ).");
            }

            final int size = this.base.read(this.buff, this.readSize, this.buff.length - this.readSize);
            if (size < 0) {
                // EOF.
                if (this.readSize == 0) {
                    return null;
                } else {
                    final String line = new String(this.buff, 0, this.readSize, this.charset);
                    this.readSize = 0;
                    return line;
                }
            }
            tail = Math.max(0, this.readSize - this.separator.length);
            this.readSize += size;
        }

    }

    @Override
    public void close() throws IOException {
        this.base.close();
    }

    @SuppressWarnings("javadoc")
    public static void main(final String[] args) throws IOException, MyRuleException {
        final String content = "POST /test/bbs.cgi HTTP/1.0\r\n"
                + "COOKIE:\r\n"
                + "HOST: localhost:11111\r\n"
                + "REFERER: http://localhost:11111/test/\r\n"
                + "CONNECTION: close\r\n"
                + "USER_AGENT: Monazilla/1.00 Navi2ch\r\n"
                + "CONTENT_LENGTH: 104\r\n"
                + "CONTENT_TYPE: application/x-www-form-urlencoded\r\n"
                + "\r\n"
                + "submit=%8F%91%82%AB%8D%9E%82%DE&FROM=&mail=&bbs=test&time=1369396215&MESSAGE=%82%A0%82%D9&key=1369396110";
        try (final InputStreamWrapper input = new InputStreamWrapper(new ByteArrayInputStream(content.getBytes()), Charset.defaultCharset(), "\r\n", 8192)) {
            for (int i = 0; i < 9; i++) {
                final String line = input.readLine();
                System.out.println(line);
            }
            final byte[] buff = new byte[104];
            input.completeRead(buff);
            System.out.println(new String(buff));
        }
    }

}
