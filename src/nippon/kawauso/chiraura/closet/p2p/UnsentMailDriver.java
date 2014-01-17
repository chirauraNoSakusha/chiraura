/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.UnsentMail;

/**
 * 未送信の手紙への対処。
 * @author chirauraNoSakusha
 */
final class UnsentMailDriver {

    private static final Logger LOG = Logger.getLogger(UnsentMailDriver.class.getName());

    private final SessionManager sessionManager;

    UnsentMailDriver(final SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    void execute(final UnsentMail unsent) {
        for (final List<Message> mail : unsent.getMails()) {
            if (mail.get(mail.size() - 1) instanceof SessionMessage) {
                // 未送信のセッションを解放。
                final SessionMessage session = (SessionMessage) mail.get(mail.size() - 1);
                if (this.sessionManager.setNull(session.get(), unsent.getDestination())) {
                    LOG.log(Level.FINER, "未達セッション ( {0} ) を放棄しました。", session.get());
                }
            }

            final StringBuilder buff = new StringBuilder("{");
            for (final Message message : mail) {
                if (buff.length() > 1) {
                    buff.append(", ");
                }
                buff.append(message.getClass().getName());
            }
            buff.append('}');
            LOG.log(Level.FINER, "{0} を {1} に送れませんでした。", new Object[] { buff, unsent.getDestination() });
        }
    }

}
