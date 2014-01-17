/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

/**
 * 受信した手紙の容器。
 * @author chirauraNoSakusha
 */
public interface ReceivedMail {

    /**
     * 手紙送信元の識別用鍵を得る。
     * @return 手紙送信元の識別用鍵
     */
    public PublicKey getSourceId();

    /**
     * 手紙の送信元を得る。
     * @return 送信元
     */
    public InetSocketAddress getSourcePeer();

    /**
     * 受信した通信路の種類を得る。
     * @return 通信路の種類
     */
    public int getConnectionType();

    /**
     * 手紙の中身を得る。
     * @return 手紙の中身
     */
    public List<Message> getMail();

}
