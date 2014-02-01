/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

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

    Communicator(final Connection connection, final ConnectionPool connectionPool, final ResponseMaker responseMaker, final long timeout) {
        if (connection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (responseMaker == null) {
            throw new IllegalArgumentException("Null response maker.");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout ( " + timeout + " ).");
        }

        this.connection = connection;
        this.connectionPool = connectionPool;
        this.responseMaker = responseMaker;
        this.timeout = timeout;
    }

    @Override
    public Void call() throws Exception {
        LOG.log(Level.FINE, "こんにちは。");

        try {
            subCall();
        } catch (final SocketTimeoutException e) {
            // 正常な終了信号。
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.connection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                LOG.log(Level.WARNING, "異常発生", e);
            }
        } finally {
            // 登録の削除。
            this.connectionPool.remove(this.connection);
            this.connection.close();
        }

        LOG.log(Level.FINE, "さようなら。");
        return null;
    }

    private void subCall() throws IOException, MyRuleException {
        final InputStreamWrapper input = new InputStreamWrapper(this.connection.getSocket().getInputStream(), Http.HEADER_CHARSET, Http.SEPARATOR, LINE_SIZE);
        final OutputStream output = new BufferedOutputStream(this.connection.getSocket().getOutputStream());

        while (!Thread.currentThread().isInterrupted()) {
            // リクエストの受信。
            final HttpRequest httpRequest = HttpRequest.fromStream(input);
            if (httpRequest == null) {
                break;
            }
            LOG.log(Level.FINEST, "リクエストを受信: {0}", httpRequest);

            // if (httpRequest.getContent() != null) {
            // System.out.println("AhoBakaChinaDebuEroFunGeroHageIboJiKuso[" + new String(httpRequest.getContent(), Constants.CONTENT_CHARSET) + "]");
            // }

            Response response;
            try {
                final Request request = Requests.fromHttpRequest(httpRequest);

                // レスポンスの準備。
                response = this.responseMaker.make(request, this.timeout);
            } catch (final Exception e) {
                LOG.log(Level.WARNING, "異常発生", e);
                response = new InternalServerErrorResponse("ごめんなさい。");
            }

            // レスポンスの送信。
            response.toStream(output);
            output.flush();
            LOG.log(Level.FINEST, "応答を送信: {0}", response);

            final String connectField = httpRequest.getFields().get(Http.Field.CONNECTION);
            if (connectField != null && connectField.toLowerCase().equals("close")) {
                // 閉じ宣言されてたら即閉じ。
                break;
            }
        }
    }
}
