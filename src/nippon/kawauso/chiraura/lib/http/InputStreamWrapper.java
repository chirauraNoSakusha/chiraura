package nippon.kawauso.chiraura.lib.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import nippon.kawauso.chiraura.lib.BytesFunctions;
import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * テキスト読み込みとバイト列読み込みを両備した InputStream。
 * mark と reset を実装するが、バイト操作の mark と reset の間にテキスト操作を挟むことはできない。
 * @author chirauraNoSakusha
 */
public final class InputStreamWrapper extends InputStream {

    private final InputStream base;
    private final Charset charset;
    private final byte[] separator;
    private final byte[] buff;

    // buff に読み込んであるまだ取り出してないバイトの先頭。
    private int head;
    // buff に読み込んであるバイトの末尾。
    private int tail;
    // mark, reset で使う。
    private int mark;
    private int readLimit;

    /**
     * 作成する。
     * @param base 元にする InputStream
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
        this.head = 0;
        this.tail = 0;
        this.mark = 0;
        this.readLimit = 0;
    }

    // buff の中身を前に詰める。
    private void shift() {
        final int len = this.tail - this.head;
        System.arraycopy(this.buff, this.head, this.buff, 0, len);
        this.head = 0;
        this.tail = len;
        this.mark -= len;
    }

    // buff の後ろに入力を読み込む。
    // EOF のときだけ true を返す。
    private boolean fill() throws IOException {
        final int size = this.base.read(this.buff, this.tail, this.buff.length - this.tail);
        if (size < 0) {
            return true;
        } else {
            this.tail += size;
            return false;
        }
    }

    /**
     * 1 行読む。
     * @return 1 行。もう無いときは null
     * @throws IOException 読み込み異常
     * @throws MyRuleException 1 行が許容サイズを超えた場合
     */
    public String readLine() throws IOException, MyRuleException {
        for (int pointer = this.head;;) {
            // System.out.println("Aho " + this.head + " " + pointer + " " + this.tail);
            if (pointer + this.separator.length > this.tail) {
                // System.out.println("Baka " + this.head + " " + pointer + " " + this.tail);
                if (this.tail - this.head == this.buff.length) {
                    // 行が長過ぎる。
                    throw new MyRuleException("Too long line over limit ( " + (this.buff.length - this.separator.length) + " ).");
                }

                pointer -= this.head;
                shift();
                if (fill()) {
                    // EOF.
                    if (this.head == this.tail) {
                        return null;
                    } else {
                        final String line = new String(this.buff, this.head, this.tail - this.head, this.charset);
                        this.head = this.tail;
                        return line;
                    }
                }
            }

            final int index = BytesFunctions.indexOf(this.buff, this.separator, pointer, this.tail);
            if (index < 0) {
                pointer = Math.max(0, this.tail - (this.separator.length - 1));
                continue;
            }

            // 改行があった。
            // System.out.println("len = " + (index - this.head) + " " + Arrays.toString(Arrays.copyOfRange(this.buff, this.head, index)));
            final String line = new String(this.buff, this.head, index - this.head, this.charset);
            this.head = index + this.separator.length;
            return line;
        }
    }

    @Override
    public void close() throws IOException {
        this.base.close();
    }

    @Override
    public int read() throws IOException {
        final int len = this.tail - this.head;
        if (len == 0) {
            return this.base.read();
        } else {
            final int val = this.buff[this.head];
            this.head++;
            return val;
        }
    }

    @Override
    public int read(final byte[] b) throws IOException {
        final int len = this.tail - this.head;
        if (len == 0) {
            return this.base.read(b);
        } else if (len < b.length) {
            System.arraycopy(this.buff, this.head, b, 0, len);
            this.head = this.tail;
            return len + this.base.read(b, len, b.length - len);
        } else {
            System.arraycopy(this.buff, this.head, b, 0, b.length);
            this.head += b.length;
            return b.length;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int buffLen = this.tail - this.head;
        if (buffLen == 0) {
            return this.base.read(b, off, len);
        } else if (buffLen < len) {
            System.arraycopy(this.buff, this.head, b, off, buffLen);
            this.head = this.tail;
            return buffLen + this.base.read(b, off + buffLen, len - buffLen);
        } else {
            System.arraycopy(this.buff, this.head, b, off, len);
            this.head += len;
            return len;
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        final int len = this.tail - this.head;
        if (len == 0) {
            return this.base.skip(n);
        } else if (len < n) {
            this.head = this.tail;
            return len + this.base.skip(n - len);
        } else {
            this.head += n;
            return n;
        }
    }

    @Override
    public int available() throws IOException {
        final int len = this.tail - this.head;
        if (len == 0) {
            return this.base.available();
        } else {
            return len + this.base.available();
        }
    }

    @Override
    public synchronized void mark(final int readlimit) {
        this.mark = this.head;
        this.readLimit = readlimit;

        final int len = this.tail - this.head;
        if (len == 0) {
            this.base.mark(readlimit);
        } else if (len < readlimit) {
            this.base.mark(readlimit - len);
        } else {
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        final int len = this.tail - this.mark;
        if (len == 0) {
            this.base.reset();
        } else if (len < this.readLimit) {
            this.base.reset();
        } else {
        }

        this.head = this.mark;

        // shift してたら動かない仕様。
        if (this.head < 0) {
            throw new IOException("Not supported situation.");
        }
    }

    @Override
    public boolean markSupported() {
        return this.base.markSupported();
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
            StreamFunctions.completeRead(input, buff, 0, buff.length);
            System.out.println(new String(buff));
        }
    }

}
