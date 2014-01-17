/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.net.Socket;

/**
 * 接続。
 * @author chirauraNoSakusha
 */
final class Connection implements AutoCloseable {

    private final Socket socket;
    private boolean isClosed;

    Connection(final Socket socket) {
        if (socket == null) {
            throw new IllegalArgumentException("Null socket.");
        }

        this.socket = socket;
        this.isClosed = false;
    }

    Socket getSocket() {
        return this.socket;
    }

    synchronized boolean isClosed() {
        return this.isClosed;
    }

    @Override
    public synchronized void close() {
        this.isClosed = true;
        try {
            this.socket.close();
        } catch (final IOException ignored) {
        }
    }

}
