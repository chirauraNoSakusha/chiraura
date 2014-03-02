/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 自分から他の個体へ接続を開いている最中の接続。
 * @author chirauraNoSakusha
 */
final class ContactingConnection extends BoundConnection {

    private final int type;

    ContactingConnection(final int idNumber, final InetSocketAddress destination, final int type) {
        super(idNumber, new Socket(), destination);

        this.type = type;
    }

    int getType() {
        return this.type;
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
        System.out.println(new ContactingConnection(1134, new InetSocketAddress("localhost", 4632), ConnectionTypes.CONTROL));
    }

}
