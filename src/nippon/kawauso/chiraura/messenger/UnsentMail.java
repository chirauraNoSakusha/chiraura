/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 未送信の手紙。
 * @author chirauraNoSakusha
 */
public final class UnsentMail implements MessengerReport {

    private final InetSocketAddress destination;
    private final int connectionType;
    private final List<List<Message>> mails;

    UnsentMail(final InetSocketAddress destination, final int connectionType, final List<List<Message>> mails) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        } else if (mails == null) {
            throw new IllegalArgumentException("Null mails.");
        }
        this.destination = destination;
        this.connectionType = connectionType;
        this.mails = mails;
    }

    /**
     * 未送信の手紙の宛先を返す。
     * @return 未送信の手紙の宛先
     */
    public InetSocketAddress getDestination() {
        return this.destination;
    }

    /**
     * 未送信の手紙の送信接続種別を返す。
     * @return 未送信の手紙の送信接続種別
     */
    public int getConnectionType() {
        return this.connectionType;
    }

    /**
     * 未送信の手紙を返す。
     * @return 未送信の手紙
     */
    public List<List<Message>> getMails() {
        return this.mails;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(", ").append(this.connectionType)
                .append(", numOfMessages=").append(this.mails.size())
                .append(']').toString();
    }

}
