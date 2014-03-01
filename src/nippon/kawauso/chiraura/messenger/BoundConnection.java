/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author chirauraNoSakusha
 */
abstract class BoundConnection extends SkeletalConnection {

    private final InetSocketAddress destination;

    BoundConnection(final int idNumber, final Socket socket, final InetSocketAddress destination) {
        super(idNumber, socket);

        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }

        this.destination = destination;
    }

    @Override
    InetSocketAddress getDestination() {
        return this.destination;
    }

}
