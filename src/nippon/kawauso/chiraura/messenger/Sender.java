package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;

/**
 * せっせと送信する係。
 * @author chirauraNoSakusha
 */
final class Sender implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Sender.class.getName());

    // 参照。
    // 入出力。
    private final SendQueuePool mailSource;
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final ConnectionPool<Connection> connectionPool;

    // 通信周り。
    private final long timeout;
    private final Transceiver transceiver;

    private final Connection connection;

    // 暗号化周り。
    private final long keyLifetime;

    private final PrivateKey myKey;
    private final PublicKey destinationKey;

    private Key encryptionKey;

    Sender(final SendQueuePool mailSource, final BlockingQueue<MessengerReport> messengerReportSink, final ConnectionPool<Connection> connectionPool,
            final long timeout, final Transceiver transceiver, final Connection connection, final long keyLifetime, final PrivateKey myKey,
            final PublicKey destinationKey, final Key firstEncryptionKey) {
        if (mailSource == null) {
            throw new IllegalArgumentException("Null mail source.");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout ( " + timeout + " ).");
        } else if (transceiver == null) {
            throw new IllegalArgumentException("Null transceiver.");
        } else if (connection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (keyLifetime < 0) {
            throw new IllegalArgumentException("Invalid key lifetime ( " + keyLifetime + " ).");
        } else if (myKey == null) {
            throw new IllegalArgumentException("Null my key.");
        } else if (destinationKey == null) {
            throw new IllegalArgumentException("Null destination key.");
        } else if (firstEncryptionKey == null) {
            throw new IllegalArgumentException("Null first encryption key.");
        }

        this.mailSource = mailSource;
        this.messengerReportSink = messengerReportSink;
        this.connectionPool = connectionPool;
        this.timeout = timeout;
        this.transceiver = transceiver;
        this.connection = connection;
        this.keyLifetime = keyLifetime;
        this.myKey = myKey;
        this.destinationKey = destinationKey;
        this.encryptionKey = firstEncryptionKey;
    }

    @Override
    public Void call() {
        LOG.log(Level.FINE, "{0}: こんにちは。", this.connection);

        // キューの登録。
        if (this.mailSource.addQueue(this.connection.getDestination(), this.connection.getType(), this.connection.getIdNumber())) {
            LOG.log(Level.FINEST, "{0}: 郵便ポストを作成しました。", this.connection);
        }

        try {
            subCall();
        } catch (final InterruptedException e) {
            // 正常な終了信号。
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.connection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                // 通信異常はさして珍しいものではない。
                LOG.log(Level.FINER, (new StringBuilder()).append(this.connection).append(": 異常が発生しました").toString(), e);
                ConcurrentFunctions.completePut(new CommunicationError(this.connection.getDestination(), e), this.messengerReportSink);
            }
        } finally {
            // 登録の削除。
            this.connectionPool.remove(this.connection.getIdNumber());
            this.connection.close();
            // キューの削除。
            final List<List<Message>> remains = this.mailSource.removeQueue(this.connection.getDestination(), this.connection.getType(),
                    this.connection.getIdNumber());
            if (remains != null) {
                LOG.log(Level.FINEST, "{0}: 郵便ポストを破棄しました。", this.connection);
                if (!remains.isEmpty()) {
                    ConcurrentFunctions.completePut(new UnsentMail(this.connection.getDestination(), this.connection.getType(), remains),
                            this.messengerReportSink);
                }
            }
        }

        LOG.log(Level.FINE, "{0}: さようなら。", this.connection);
        return null;
    }

    private void subCall() throws IOException, InterruptedException {
        long keyUpdateDate = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            final long waitTime = Math.max(1L, this.connection.getDate() + this.timeout - System.currentTimeMillis());
            List<Message> mail = this.mailSource.take(this.connection.getDestination(), this.connection.getType(), waitTime);

            if (mail == null) {
                // 受信側が最終動作時刻を更新していないか検査。
                if (this.connection.getDate() + this.timeout <= System.currentTimeMillis()) {
                    break;
                } else {
                    continue;
                }
            }

            // 最終動作時刻を更新。
            this.connection.updateDate();

            // 送信。
            final long date = System.currentTimeMillis();
            if (date < keyUpdateDate + this.keyLifetime) {
                final int size = this.transceiver.toStream(mail, GZippedEncryptedEnvelope.class, this.encryptionKey);
                LOG.log(Level.FINER, "{0}: {1} バイト送信。", new Object[] { this.connection, size });
            } else {
                // 暗号鍵の更新。
                final Key newEncryptionKey = CryptographicKeys.newCommonKey();
                mail.add(KeyUpdateMessage.newInstance(this.destinationKey, this.myKey, newEncryptionKey));

                final int size = this.transceiver.toStream(mail, GZippedEncryptedEnvelope.class, this.encryptionKey);
                LOG.log(Level.FINER, "{0}: {1} バイト送信 ( 鍵更新含む )。", new Object[] { this.connection, size });

                keyUpdateDate = date;
                this.encryptionKey = newEncryptionKey;
            }

            // 最終動作時刻を更新。
            this.connection.updateDate();

            while (!Thread.currentThread().isInterrupted()) {
                // 送信待ちがある場合はついでに送信する。
                mail = this.mailSource.takeIfExists(this.connection.getDestination(), this.connection.getType());
                if (mail == null) {
                    break;
                }
                final int size = this.transceiver.toStream(mail, GZippedEncryptedEnvelope.class, this.encryptionKey);
                LOG.log(Level.FINER, "{0}: ついでに {1} バイト送信。", new Object[] { this.connection, size });

                // 最終動作時刻を更新。
                this.connection.updateDate();
            }

            this.transceiver.flush();

            // 最終動作時刻を更新。
            this.connection.updateDate();
        }
    }

}
