package nippon.kawauso.chiraura.bbs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.http.Http;
import nippon.kawauso.chiraura.lib.http.InputStreamWrapper;

/**
 * 応対係。
 * @author chirauraNoSakusha
 */
final class Communicator implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Communicator.class.getName());

    private static final int LINE_SIZE = 8192;

    private final Connection connection;
    private final ConnectionPool connectionPool;
    private final ResponseMaker responseMaker;
    private final long timeout;

    private final Limiter<InetSocketAddress> limiter;

    Communicator(final Connection connection, final ConnectionPool connectionPool, final ResponseMaker responseMaker, final long timeout,
            final Limiter<InetSocketAddress> limiter) {
        if (connection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (responseMaker == null) {
            throw new IllegalArgumentException("Null response maker.");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout ( " + timeout + " ).");
        } else if (limiter == null) {
            throw new IllegalArgumentException("Null limiter.");
        }

        this.connection = connection;
        this.connectionPool = connectionPool;
        this.responseMaker = responseMaker;
        this.timeout = timeout;

        this.limiter = limiter;
    }

    @Override
    public Void call() {
        LOG.log(Level.FINE, "こんにちは。");

        try {
            subCall();
        } catch (final SocketTimeoutException e) {
            // 正常な終了信号。
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.connection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                LOG.log(Level.WARNING, "異常が発生しました", e);
            }
        } finally {
            // 登録の削除。
            this.connectionPool.remove(this.connection);
            this.connection.close();
            try {
                this.limiter.remove(this.connection.getDestination());
            } catch (final InterruptedException ignored) {
                // 正常な終了信号。
            }
        }

        LOG.log(Level.FINE, "さようなら。");
        return null;
    }

    private void subCall() throws IOException, MyRuleException, InterruptedException {
        final InputStreamWrapper input = new InputStreamWrapper(this.connection.getSocket().getInputStream(), Http.HEADER_CHARSET, Http.SEPARATOR, LINE_SIZE);
        final OutputStream output = new BufferedOutputStream(this.connection.getSocket().getOutputStream());

        limitSleep(false);

        while (!Thread.currentThread().isInterrupted()) {
            // リクエストの受信。
            final HttpRequest httpRequest = HttpRequest.fromStream(input);
            if (httpRequest == null) {
                break;
            }
            LOG.log(Level.FINEST, "リクエストを受信: {0}", httpRequest);

            limitSleep(true);

            // if (httpRequest.getContent() != null) {
            // System.out.println("AhoBakaChinaDebuEroFunGeroHageIboJiKuso[" + new String(httpRequest.getContent(), Constants.CONTENT_CHARSET) + "]");
            // }

            Response response;
            try {
                final Request request = Requests.fromHttpRequest(httpRequest, this.connection.getSocket().getInetAddress());

                // レスポンスの準備。
                response = this.responseMaker.make(request, this.timeout);
            } catch (final Exception e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                response = new InternalServerErrorResponse("ごめんなさい。");
            }

            // レスポンスの送信。
            response.toStream(output);
            output.flush();
            LOG.log(Level.FINEST, "応答を送信: {0}", response);

            final String connectField = httpRequest.getFields().get(Http.Field.CONNECTION);
            if (connectField != null && connectField.toLowerCase().equals("close") || httpRequest.getVersion().equals("HTTP/1.0")) {
                // 閉じ宣言されてたら即閉じ。
                break;
            }

        }
    }

    private void limitSleep(final boolean received) throws InterruptedException {
        long sleep;
        if (received) {
            // 回数制限だけだからサイズは 0 で報告。
            sleep = this.limiter.addValueAndCheckPenalty(this.connection.getDestination(), 0);
        } else {
            sleep = this.limiter.checkPenalty(this.connection.getDestination());
        }
        while (sleep > 0) {
            LOG.log(Level.WARNING, "{0}: {1} ミリ秒さぼります。", new Object[] { this.connection, sleep });
            Thread.sleep(sleep);
            sleep = this.limiter.checkPenalty(this.connection.getDestination());
        }
    }

}
