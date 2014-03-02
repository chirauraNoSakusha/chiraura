/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 通信先が不明な接続。
 * @author chirauraNoSakusha
 */
final class AcceptedConnection extends SkeletalConnection {

    AcceptedConnection(final int idNumber, final Socket socket) {
        super(idNumber, socket);
    }

    @Override
    InetSocketAddress getDestination() {
        final InetSocketAddress destination;
        if (getSocket().getRemoteSocketAddress() instanceof InetSocketAddress) {
            destination = (InetSocketAddress) getSocket().getRemoteSocketAddress();
        } else {
            destination = new InetSocketAddress(getSocket().getInetAddress(), getSocket().getPort());
        }
        return destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.getIdNumber())
                .append(", ").append(getSocket().getInetAddress())
                .append(']').toString();
    }

    public static void main(final String[] args) {
        System.out.println(new AcceptedConnection(1134, new Socket()));
    }

}
