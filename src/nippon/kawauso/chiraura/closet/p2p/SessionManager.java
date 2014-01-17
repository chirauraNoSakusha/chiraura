/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.messenger.ReceivedMail;

/**
 * 他の個体とやり取りするための印を管理する機構。
 * @author chirauraNoSakusha
 */
final class SessionManager {

    private static final Logger LOG = Logger.getLogger(SessionManager.class.getName());

    private static final class Parameters {

        private final InetSocketAddress destination;
        private ReceivedMail reply;

        private final CountDownLatch barrier;

        private Parameters(final InetSocketAddress destination) {
            this.destination = destination;
            this.reply = null;
            this.barrier = new CountDownLatch(1);
        }

        private InetSocketAddress getDestination() {
            return this.destination;
        }

        private ReceivedMail getReply() {
            return this.reply;
        }

        private boolean waitReply(final long timeout) throws InterruptedException {
            return this.barrier.await(timeout, TimeUnit.MILLISECONDS);
        }

        private void setReply(final ReceivedMail result) {
            this.reply = result;
            this.barrier.countDown();
        }

    }

    private final AtomicInteger serialGenerator;
    private final ConcurrentMap<Session, Parameters> container;

    SessionManager() {
        this.serialGenerator = new AtomicInteger();
        this.container = new ConcurrentHashMap<>();
    }

    Session newSession(final InetSocketAddress destination) {
        final Session session = new Session(this.serialGenerator.getAndIncrement());
        this.container.put(session, new Parameters(destination));
        return session;
    }

    /**
     * 返信を待つ。
     * 終了時にセッションは削除される。
     * @param session セッション
     * @param timeout 制限時間
     * @return 返信
     * @throws InterruptedException 割り込まれた場合
     */
    ReceivedMail waitReply(final Session session, final long timeout) throws InterruptedException {
        final Parameters parameters = this.container.get(session);
        if (parameters == null) {
            throw new IllegalStateException("Not registered session ( " + session + " ).");
        }

        parameters.waitReply(timeout);
        synchronized (parameters) {
            this.container.remove(session);
            return parameters.getReply();
        }
    }

    private boolean setReply(final Session session, final InetSocketAddress destination, final ReceivedMail result) {
        final Parameters parameters = this.container.get(session);
        if (parameters == null) {
            // もう getResult されてた。
            return false;
        }
        synchronized (parameters) {
            if (this.container.get(session) == null) {
                // ついさっき waitResult が終わった。
                return false;
            } else if (!parameters.getDestination().equals(destination)) {
                LOG.log(Level.WARNING, "セッション ( {0} ) の相手 ( {1} ) が登録 ( {2} ) と違います。", new Object[] { session, destination, parameters.getDestination() });
                return false;
            }
            parameters.setReply(result);
            return true;
        }
    }

    /**
     * セッションの返信を設定する。
     * 設定できるのは、セッションが発行されており、かつ、まだ削除されておらず、
     * かつ、セッション相手がセッション発行時と合致している場合のみ。
     * @param session セッション
     * @param reply 返信
     * @return 設定できた場合のみ true
     */
    boolean setReply(final Session session, final ReceivedMail reply) {
        return setReply(session, reply.getSourcePeer(), reply);
    }

    /**
     * セッションの返信が取得できなかったことを報告する。
     * 報告できるのは、セッションが発行されており、かつ、まだ削除されておらず、
     * かつ、セッション相手がセッション発行時と合致している場合のみ。
     * @param session セッション
     * @param destination セッション相手
     * @return 報告できた場合のみ true
     */
    boolean setNull(final Session session, final InetSocketAddress destination) {
        return setReply(session, destination, null);
    }

}
