/**
 *
 */
package nippon.kawauso.chiraura.lib.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * ソケットを包んだもの。
 * 使用例は getErrorInputSocket を参照。
 * @author chirauraNoSakusha
 */
public abstract class SocketWrapper extends Socket {
    private final Socket base;

    /**
     * 包むソケットを指定して作成。
     * @param base 包むソケット
     */
    public SocketWrapper(final Socket base) {
        this.base = base;
    }

    @Override
    public void connect(final SocketAddress endpoint) throws IOException {
        this.base.connect(endpoint);
    }

    @Override
    public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
        this.base.connect(endpoint, timeout);
    }

    @Override
    public void bind(final SocketAddress bindpoint) throws IOException {
        this.base.bind(bindpoint);
    }

    @Override
    public InetAddress getInetAddress() {
        return this.base.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.base.getLocalAddress();
    }

    @Override
    public int getPort() {
        return this.base.getPort();
    }

    @Override
    public int getLocalPort() {
        return this.base.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return this.base.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return this.base.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
        return this.base.getChannel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.base.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return this.base.getOutputStream();
    }

    @Override
    public void setTcpNoDelay(final boolean on) throws SocketException {
        this.base.setTcpNoDelay(on);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return this.base.getTcpNoDelay();
    }

    @Override
    public void setSoLinger(final boolean on, final int linger) throws SocketException {
        this.base.setSoLinger(on, linger);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return this.base.getSoLinger();
    }

    @Override
    public void sendUrgentData(final int data) throws IOException {
        this.base.sendUrgentData(data);
    }

    @Override
    public void setOOBInline(final boolean on) throws SocketException {
        this.base.setOOBInline(on);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return this.base.getOOBInline();
    }

    @Override
    public synchronized void setSoTimeout(final int timeout) throws SocketException {
        this.base.setSoTimeout(timeout);
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return this.base.getSoTimeout();
    }

    @Override
    public synchronized void setSendBufferSize(final int size) throws SocketException {
        this.base.setSendBufferSize(size);
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return this.base.getSendBufferSize();
    }

    @Override
    public synchronized void setReceiveBufferSize(final int size) throws SocketException {
        this.base.setReceiveBufferSize(size);
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return this.base.getReceiveBufferSize();
    }

    @Override
    public void setKeepAlive(final boolean on) throws SocketException {
        this.base.setKeepAlive(on);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return this.base.getKeepAlive();
    }

    @Override
    public void setTrafficClass(final int tc) throws SocketException {
        this.base.setTrafficClass(tc);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return this.base.getTrafficClass();
    }

    @Override
    public void setReuseAddress(final boolean on) throws SocketException {
        this.base.setReuseAddress(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return this.base.getReuseAddress();
    }

    @Override
    public synchronized void close() throws IOException {
        this.base.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        this.base.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        this.base.shutdownOutput();
    }

    @Override
    public String toString() {
        return this.base.toString();
    }

    @Override
    public boolean isConnected() {
        return this.base.isConnected();
    }

    @Override
    public boolean isBound() {
        return this.base.isBound();
    }

    @Override
    public boolean isClosed() {
        return this.base.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return this.base.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return this.base.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
        this.base.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public int hashCode() {
        return this.base.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this.base.equals(obj);
    }

    /**
     * getInputStream で、read を与えられた回数だけ呼ぶと IOException を投げるような InputStream を返すソケットを返す。
     * @param socket 元にするソケット
     * @param errorThreshold InputStream が IOException を投げるまでの回数
     * @return 指定された条件で IOException を投げる InputStream を返すソケット
     */
    public static Socket getErrorInputSocket(final Socket socket, final int errorThreshold) {
        return new SocketWrapper(socket) {
            @Override
            public InputStream getInputStream() throws IOException {
                return InputStreamWrapper.getErrorStream(super.getInputStream(), errorThreshold);
            }
        };
    }

}
