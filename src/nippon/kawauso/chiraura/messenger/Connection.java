/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接続。
 * @author chirauraNoSakusha
 */
final class Connection extends BoundConnection {

    private final PublicKey detinationId;
    private final int type;

    private Future<Void> sender;
    private final AtomicLong date;

    Connection(final int idNumber, final InetSocketAddress destination, final PublicKey destinationId, final int type, final Socket socket) {
        super(idNumber, socket, destination);

        if (destinationId == null) {
            throw new IllegalArgumentException("Null destination id.");
        }

        this.detinationId = destinationId;
        this.type = type;
        this.date = new AtomicLong(System.currentTimeMillis());
    }

    PublicKey getDestinationId() {
        return this.detinationId;
    }

    int getType() {
        return this.type;
    }

    long getDate() {
        return this.date.get();
    }

    void updateDate() {
        this.date.set(System.currentTimeMillis());
    }

    synchronized void setSender(final Future<Void> sender) {
        this.sender = sender;
    }

    @Override
    synchronized void close() {
        super.close();
        if (this.sender != null) {
            this.sender.cancel(true);
        }
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.getIdNumber())
                .append(", ").append(this.getDestination())
                .append(", ").append(this.type)
                .append(']').toString();
    }

    public static void main(final String[] args) {
        System.out.println(new Connection(1134, new InetSocketAddress("localhost", 4632), CryptographicKeys.newPublicKeyPair().getPublic(),
                ConnectionTypes.CONTROL, new Socket()));
    }

}
