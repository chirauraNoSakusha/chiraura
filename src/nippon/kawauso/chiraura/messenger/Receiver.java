/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * せっせと受信する係。
 * @author chirauraNoSakusha
 */
final class Receiver implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Receiver.class.getName());

    // 参照
    // 入出力。
    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final BlockingQueue<MessengerReport> messengerReportSink;

    // 通信周り。
    private final long timeout;
    private final Connection connection;
    private final Transceiver transceiver;
    private final InputStream input;

    // 暗号化周り。
    private final PrivateKey myKey;
    private final PublicKey destinationKey;
    private Key decryptionKey;

    Receiver(final BlockingQueue<ReceivedMail> receivedMailSink, final BlockingQueue<MessengerReport> messengerReportSink, final long timeout,
            final Connection connection, final Transceiver transceiver, final InputStream input, final PrivateKey myKey, final PublicKey destinationKey,
            final Key firstDecryptionKey) {
        if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout ( " + timeout + " ).");
        } else if (connection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (transceiver == null) {
            throw new IllegalArgumentException("Null transceiver.");
        } else if (input == null) {
            throw new IllegalArgumentException("Null input.");
        } else if (myKey == null) {
            throw new IllegalArgumentException("Null my key.");
        } else if (destinationKey == null) {
            throw new IllegalArgumentException("Null destination key.");
        } else if (firstDecryptionKey == null) {
            throw new IllegalArgumentException("Null first decryption key.");
        }

        this.receivedMailSink = receivedMailSink;
        this.messengerReportSink = messengerReportSink;
        this.timeout = timeout;
        this.connection = connection;
        this.transceiver = transceiver;
        this.input = input;
        this.myKey = myKey;
        this.destinationKey = destinationKey;
        this.decryptionKey = firstDecryptionKey;
    }

    @Override
    public Void call() {
        LOG.log(Level.FINE, "{0}: こんにちは。", this.connection);

        try {
            subCall();
        } catch (final SocketTimeoutException e) {
            // 正常な終了信号。
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.connection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                LOG.log(Level.FINEST, "{0}: 異常発生: {1}", new Object[] { this.connection, e.toString() });
                ConcurrentFunctions.completePut(new CommunicationError(this.connection.getDestination(), e), this.messengerReportSink);
            }
        } finally {
            this.connection.close();
        }

        LOG.log(Level.FINE, "{0}: さようなら。", this.connection);
        return null;
    }

    private void subCall() throws IOException, MyRuleException {
        while (!Thread.currentThread().isInterrupted()) {
            final List<Message> mail = new ArrayList<>();

            // 受信終了かどうか検査。
            try {
                if (StreamFunctions.isEof(this.input)) {
                    LOG.log(Level.FINEST, "{0}: 受信終了。", this.connection);
                    break;
                }
            } catch (final SocketTimeoutException e) {
                // 送信側が最終動作時刻を更新していないか検査。
                if (this.connection.getDate() + this.timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0}: 時間切れ 1。", this.connection);
                    break;
                } else {
                    continue;
                }
            }

            // 最終動作時刻を更新。
            this.connection.updateDate();

            // 受信。
            final int size;
            try {
                size = this.transceiver.fromStream(this.input, this.decryptionKey, mail);
            } catch (final SocketTimeoutException e) {
                // 送信側が最終動作時刻を更新していないか検査。
                if (this.connection.getDate() + this.timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0}: 時間切れ 2。", this.connection);
                    break;
                } else {
                    continue;
                }
            }

            if (!mail.isEmpty() && mail.get(mail.size() - 1) instanceof KeyUpdateMessage) {
                // 暗号鍵の更新。
                LOG.log(Level.FINER, "{0}: {1} バイト受信 ( 鍵更新含む )。", new Object[] { this.connection, Integer.toString(size) });
                final KeyUpdateMessage message = (KeyUpdateMessage) mail.remove(mail.size() - 1);
                this.decryptionKey = message.getKey(this.myKey, this.destinationKey);
            } else {
                LOG.log(Level.FINER, "{0}: {1} バイト受信。", new Object[] { this.connection, Integer.toString(size) });
            }

            final ReceivedMail receivedMail = new BasicReceivedMail(this.connection.getDestinationId(), this.connection.getDestination(),
                    this.connection.getType(), mail);
            ConcurrentFunctions.completePut(receivedMail, this.receivedMailSink);

            // 最終動作時刻を更新。
            this.connection.updateDate();
        }
    }

}
