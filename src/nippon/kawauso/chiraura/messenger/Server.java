/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 接続を受け付ける。
 * @author chirauraNoSakusha
 */
final class Server extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    // 参照。
    private final int port;
    private final int receiveBufferSize;
    private final BlockingQueue<Socket> acceptedSocketSink;

    // 保持。
    private final ServerSocket serverSocket;

    Server(final BlockingQueue<Reporter.Report> reportSink, final int port, final int receiveBufferSize, final BlockingQueue<Socket> acceptedSocketSink)
            throws IOException {
        super(reportSink);

        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (acceptedSocketSink == null) {
            throw new IllegalArgumentException("Null accepted socket sink.");
        }

        this.port = port;
        this.receiveBufferSize = receiveBufferSize;
        this.acceptedSocketSink = acceptedSocketSink;
        this.serverSocket = new ServerSocket();
    }

    ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    @Override
    public Void subCall() throws IOException {
        try {
            // 受信バッファの設定。
            final int oldReceiveBufferSize = this.serverSocket.getReceiveBufferSize();
            if (oldReceiveBufferSize < this.receiveBufferSize) {
                this.serverSocket.setReceiveBufferSize(this.receiveBufferSize);
                LOG.log(Level.FINER, "受信バッファサイズを {0} から {1} に変更しました。",
                        new Object[] { Integer.toString(oldReceiveBufferSize), Integer.toString(this.receiveBufferSize) });
            }

            if (Global.isDebug()) {
                this.serverSocket.setReuseAddress(true);
            }

            LOG.log(Level.FINER, "{0} 番ポートで待機を始めます。", Integer.toString(this.port));
            this.serverSocket.bind(new InetSocketAddress(this.port));

            while (!Thread.currentThread().isInterrupted()) {
                final Socket socket;
                try {
                    socket = this.serverSocket.accept();
                } catch (final IOException e) {
                    if (Thread.currentThread().isInterrupted() || this.serverSocket.isClosed()) {
                        // 別プロセスが serverSocket を閉じて終了を教えてくれた。
                        break;
                    } else {
                        throw e;
                    }
                }

                ConcurrentFunctions.completePut(socket, this.acceptedSocketSink);
                LOG.log(Level.FINER, "{0} の受け入れを開始しました。", socket.getInetAddress());
            }
        } finally {
            this.serverSocket.close();
        }

        return null;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.port)
                .append(']').toString();
    }

}
