/**
 * 
 */
package nippon.kawauso.chiraura.lib.test;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream を包んだもの。
 * 使用例は getErrorStream を参照。
 * @author chirauraNoSakusha
 */
public abstract class InputStreamWrapper extends InputStream {

    private final InputStream base;

    /**
     * 包むものを指定して作成。
     * @param base 包むもの
     */
    public InputStreamWrapper(final InputStream base) {
        this.base = base;
    }

    @Override
    public int read() throws IOException {
        return this.base.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return this.base.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return this.base.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return this.base.skip(n);
    }

    @Override
    public int available() throws IOException {
        return this.base.available();
    }

    @Override
    public void close() throws IOException {
        this.base.close();
    }

    @Override
    public synchronized void mark(final int readlimit) {
        this.base.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.base.reset();
    }

    @Override
    public boolean markSupported() {
        return this.base.markSupported();
    }

    @Override
    public int hashCode() {
        return this.base.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this.base.equals(obj);
    }

    @Override
    public String toString() {
        return this.base.toString();
    }

    /**
     * read を与えられた回数だけ呼ぶと IOException を投げる InputStream を返す。
     * @param base 元にする InputStream
     * @param errorThreshold IOException を投げるまでの回数
     * @return 指定された条件で IOException を投げる InputStream
     */
    public static InputStream getErrorStream(final InputStream base, final int errorThreshold) {
        return new InputStreamWrapper(base) {
            private int count = 0;

            private void before() throws IOException {
                this.count++;
                if (this.count > errorThreshold) {
                    throw new IOException("Dummy.");
                }
            }

            @Override
            public int read() throws IOException {
                before();
                return super.read();
            }

            @Override
            public int read(final byte[] b) throws IOException {
                before();
                return super.read(b);
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                before();
                return super.read(b, off, len);
            }
        };
    }
}
