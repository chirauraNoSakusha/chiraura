/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

/**
 * 受信メッセージの実装。
 * @author chirauraNoSakusha
 */
final class BasicReceivedMail implements ReceivedMail {

    private final PublicKey sourceId;
    private final InetSocketAddress sourcePeer;
    private final int connectionType;
    private final List<Message> mail;

    BasicReceivedMail(final PublicKey sourceId, final InetSocketAddress sourcePeer, final int connectionType, final List<Message> mail) {
        if (sourceId == null) {
            throw new IllegalArgumentException("Null source id.");
        } else if (sourcePeer == null) {
            throw new IllegalArgumentException("Null source peer.");
        } else if (mail == null) {
            throw new IllegalArgumentException("Null mail.");
        }

        this.sourceId = sourceId;
        this.sourcePeer = sourcePeer;
        this.connectionType = connectionType;
        this.mail = mail;
    }

    @Override
    public PublicKey getSourceId() {
        return this.sourceId;
    }

    @Override
    public InetSocketAddress getSourcePeer() {
        return this.sourcePeer;
    }

    @Override
    public int getConnectionType() {
        return this.connectionType;
    }

    @Override
    public List<Message> getMail() {
        return this.mail;
    }

    @Override
    public String toString() {
        final StringBuilder buff = new StringBuilder(this.getClass().getSimpleName())
                .append("[from=(").append(this.sourcePeer)
                .append(", ").append(Integer.toString(this.connectionType))
                .append("), {");
        boolean first = true;
        for (final Message message : this.mail) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(message);
        }
        return buff.append("}]").toString();
    }

}
