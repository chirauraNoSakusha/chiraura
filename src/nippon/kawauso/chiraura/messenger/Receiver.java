package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.net.InetSocketAddress;
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

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * せっせと受信する係。
 * @author chirauraNoSakusha
 */
final class Receiver implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Receiver.class.getName());

    // 接続プロトコルの通信量制限に対する重み。
    private static final int START_PROTOCOL_WEIGHT = 8192; // 8KB.

    // 参照
    // 入出力。
    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final Limiter<InetSocketAddress> limiter;

    // 通信周り。
    private final long timeout;
    private final Transceiver transceiver;

    private final Connection connection;

    // 暗号化周り。
    private final PrivateKey myKey;
    private final PublicKey destinationKey;

    private Key decryptionKey;

    Receiver(final BlockingQueue<ReceivedMail> receivedMailSink, final BlockingQueue<MessengerReport> messengerReportSink,
            final Limiter<InetSocketAddress> limiter, final long timeout, final Transceiver transceiver, final Connection connection, final PrivateKey myKey,
            final PublicKey destinationKey, final Key firstDecryptionKey) {
        if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (limiter == null) {
            throw new IllegalArgumentException("Null limiter.");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout ( " + timeout + " ).");
        } else if (transceiver == null) {
            throw new IllegalArgumentException("Null transceiver.");
        } else if (connection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (myKey == null) {
            throw new IllegalArgumentException("Null my key.");
        } else if (destinationKey == null) {
            throw new IllegalArgumentException("Null destination key.");
        } else if (firstDecryptionKey == null) {
            throw new IllegalArgumentException("Null first decryption key.");
        }

        this.receivedMailSink = receivedMailSink;
        this.messengerReportSink = messengerReportSink;
        this.limiter = limiter;
        this.timeout = timeout;
        this.transceiver = transceiver;
        this.connection = connection;
        this.myKey = myKey;
        this.destinationKey = destinationKey;
        this.decryptionKey = firstDecryptionKey;
    }

    @Override
    public Void call() {
        LOG.log(Level.FINE, "{0}: こんにちは。", this.connection);

        try {
            subCall();
        } catch (final SocketTimeoutException | InterruptedException e) {
            // 正常な終了信号。
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.connection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                LOG.log(Level.FINEST, "{0}: 異常発生: {1}", new Object[] { this.connection, e.toString() });
                ConcurrentFunctions.completePut(new CommunicationError(this.connection.getDestination(), e), this.messengerReportSink);
            }
        } finally {
            this.connection.close();
            try {
                this.limiter.remove(this.connection.getDestination());
            } catch (final InterruptedException ignored) {
                // 正常な終了信号。
            }
        }

        LOG.log(Level.FINE, "{0}: さようなら。", this.connection);
        return null;
    }

    private void subCall() throws IOException, MyRuleException, InterruptedException {
        // 接続プロトコル分。
        limitSleep(START_PROTOCOL_WEIGHT);

        while (!Thread.currentThread().isInterrupted()) {
            final List<Message> mail = new ArrayList<>();

            // 受信終了かどうか検査。
            try {
                if (this.transceiver.isEof()) {
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
                size = this.transceiver.fromStream(this.decryptionKey, mail);
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
                LOG.log(Level.FINER, "{0}: {1} バイト受信 ( 鍵更新含む )。", new Object[] { this.connection, size });
                final KeyUpdateMessage message = (KeyUpdateMessage) mail.remove(mail.size() - 1);
                this.decryptionKey = message.getKey(this.myKey, this.destinationKey);
            } else {
                LOG.log(Level.FINER, "{0}: {1} バイト受信。", new Object[] { this.connection, size });
            }

            final ReceivedMail receivedMail = new BasicReceivedMail(this.connection.getDestinationId(), this.connection.getDestination(),
                    this.connection.getType(), mail);
            ConcurrentFunctions.completePut(receivedMail, this.receivedMailSink);

            // 最終動作時刻を更新。
            this.connection.updateDate();

            limitSleep(size);
        }
    }

    private void limitSleep(final int size) throws InterruptedException {
        long sleep = this.limiter.addValueAndCheckPenalty(this.connection.getDestination(), size);
        while (sleep > 0) {
            ConcurrentFunctions.completePut(new TrafficOverflow(this.connection.getDestination()), this.messengerReportSink);
            LOG.log(Level.WARNING, "{0}: {1} ミリ秒さぼります。", new Object[] { this.connection, sleep });
            Thread.sleep(sleep);
            sleep = this.limiter.checkPenalty(this.connection.getDestination());
        }
    }

}
