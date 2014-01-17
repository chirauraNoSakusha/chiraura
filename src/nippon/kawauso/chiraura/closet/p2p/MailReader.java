/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;

/**
 * 受信した手紙を捌く人。
 * @author chirauraNoSakusha
 */
final class MailReader extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(MailReader.class.getName());

    // 参照。
    private final NetworkWrapper source;
    private final SessionManager sessionManager;
    private final long operationTimeout;

    private final MessageDriverSet messageDrivers;
    private final ReplyDriverSet replyDrivers;

    MailReader(final BlockingQueue<? super Reporter.Report> reportSink, final NetworkWrapper source, final SessionManager sessionManager,
            final long operationTimeout, final MessageDriverSet messageDrivers, final ReplyDriverSet replyDrivers) {
        super(reportSink);

        if (source == null) {
            throw new IllegalArgumentException("Null source.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Invalid operation timeout ( " + operationTimeout + " ).");
        } else if (messageDrivers == null) {
            throw new IllegalArgumentException("Null message drivers.");
        } else if (replyDrivers == null) {
            throw new IllegalArgumentException("Null reply drivers.");
        }

        this.source = source;
        this.sessionManager = sessionManager;
        this.operationTimeout = operationTimeout;
        this.messageDrivers = messageDrivers;
        this.replyDrivers = replyDrivers;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final ReceivedMail receivedMail = this.source.takeReceivedMail();

            final List<Message> mail = receivedMail.getMail();

            /*
             * この中では addActivePeer しない。
             * なぜなら、プロトコル違反である可能性があるため。
             * 何ちゃら Driver の中で正しい返信を受け取ったときに行う。
             */

            if (mail.size() <= 0) {
                LOG.log(Level.WARNING, "{0} から空の手紙を受け取りました。", receivedMail.getSourcePeer());
                continue;
            }

            if (mail.get(mail.size() - 1) instanceof SessionReply) {
                // 返信だった。
                final SessionReply session = (SessionReply) mail.remove(mail.size() - 1);
                if (!this.sessionManager.setReply(session.get(), receivedMail)) {
                    boolean done = true;
                    if (mail.get(0) instanceof PeerAccessReply) {
                        this.replyDrivers.getPeerAccessReply().execute((PeerAccessReply) mail.get(0));
                    } else if (mail.get(0) instanceof AddressAccessReply) {
                        this.replyDrivers.getAddressAccessReply().execute((AddressAccessReply) mail.get(0));
                    } else if (mail.get(0) instanceof GetChunkReply) {
                        this.replyDrivers.getGetChunkReply().execute((GetChunkReply) mail.get(0));
                    } else if (mail.get(0) instanceof UpdateChunkReply) {
                        this.replyDrivers.getUpdateChunkReply().execute((UpdateChunkReply) mail.get(0));
                    } else if (mail.get(0) instanceof AddChunkReply) {
                        this.replyDrivers.getAddChunkReply().execute((AddChunkReply) mail.get(0));
                    } else if (mail.get(0) instanceof PatchChunkReply) {
                        this.replyDrivers.getPatchChunkReply().execute((PatchChunkReply) mail.get(0));
                    } else if (mail.get(0) instanceof PatchAndGetOrUpdateCacheReply) {
                        this.replyDrivers.getPatchAndGetOrUpdateCacheReply().execute((PatchAndGetOrUpdateCacheReply<?>) mail.get(0));
                    } else if (mail.get(0) instanceof GetCacheReply) {
                        this.replyDrivers.getGetCacheReply().execute((GetCacheReply) mail.get(0));
                    } else if (mail.get(0) instanceof GetOrUpdateCacheReply) {
                        this.replyDrivers.getGetOrUpdateCacheReply().execute((GetOrUpdateCacheReply<?>) mail.get(0));
                    } else if (mail.get(0) instanceof AddCacheReply) {
                        this.replyDrivers.getAddCacheReply().execute((AddCacheReply) mail.get(0));
                    } else if (mail.get(0) instanceof PatchOrAddAndGetCacheReply) {
                        this.replyDrivers.getPatchOrAddAndGetCacheReply().execute((PatchOrAddAndGetCacheReply) mail.get(0));
                    } else if (mail.get(0) instanceof CheckStockReply) {
                        this.replyDrivers.getCheckStockReply().execute((CheckStockReply) mail.get(0));
                    } else if (mail.get(0) instanceof CheckDemandReply) {
                        this.replyDrivers.getCheckDemandReply().execute((CheckDemandReply) mail.get(0));
                    } else if (mail.get(0) instanceof RecoveryReply) {
                        this.replyDrivers.getRecoveryReply().execute((RecoveryReply<?>) mail.get(0));
                    } else if (mail.get(0) instanceof BackupReply) {
                        this.replyDrivers.getBackupReply().execute((BackupReply) mail.get(0));
                    } else if (mail.get(0) instanceof SimpleRecoveryReply) {
                        this.replyDrivers.getSimpleRecoveryReply().execute((SimpleRecoveryReply) mail.get(0));
                    } else if (mail.get(0) instanceof CheckOneDemandReply) {
                        this.replyDrivers.getCheckOneDemandReply().execute((CheckOneDemandReply) mail.get(0));
                    } else {
                        done = false;
                    }

                    if (done) {
                        LOG.log(Level.FINER, "{0} からの返信 {1} を捌きました", new Object[] { receivedMail.getSourcePeer(), mail.get(0) });
                    } else {
                        LOG.log(Level.WARNING, "返信 {0} に対する処理は実装されていません。", mail.get(0).getClass().getName());
                    }
                }
            } else if (mail.get(mail.size() - 1) instanceof SessionMessage) {
                // 送信元が返信を待ってる。
                final SessionMessage session = (SessionMessage) mail.remove(mail.size() - 1);

                final InetSocketAddress sender = receivedMail.getSourcePeer();
                // System.out.println("Baka " + this.sessionManager + " " + mail.get(0).getClass().getSimpleName() + " session " + session.get() + " from "
                // + receivedMail.getSourcePeer());

                boolean done = true;
                if (mail.get(0) instanceof PeerAccessMessage) {
                    this.messageDrivers.getPeerAccessMessage().execute((PeerAccessMessage) mail.get(0), session.get(), sender);
                } else if (mail.get(0) instanceof AddressAccessMessage) {
                    this.messageDrivers.getAddressAccessMessage().execute((AddressAccessMessage) mail.get(0), session.get(), receivedMail.getSourceId(),
                            sender, this.operationTimeout);
                } else if (mail.get(0) instanceof GetChunkMessage) {
                    this.messageDrivers.getGetChunkMessage().execute((GetChunkMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender,
                            this.operationTimeout);
                } else if (mail.get(0) instanceof UpdateChunkMessage) {
                    this.messageDrivers.getUpdateChunkMessage().execute((UpdateChunkMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender,
                            this.operationTimeout);
                } else if (mail.get(0) instanceof AddChunkMessage) {
                    this.messageDrivers.getAddChunkMessage().execute((AddChunkMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender,
                            this.operationTimeout);
                } else if (mail.get(0) instanceof PatchChunkMessage) {
                    this.messageDrivers.getPatchChunkMessage().execute((PatchChunkMessage<?>) mail.get(0), session.get(), receivedMail.getSourceId(), sender,
                            this.operationTimeout);
                } else if (mail.get(0) instanceof PatchAndGetOrUpdateCacheMessage) {
                    this.messageDrivers.getPatchAndGetOrUpdateCacheMessage().execute((PatchAndGetOrUpdateCacheMessage<?>) mail.get(0), session.get(),
                            receivedMail.getSourceId(), sender, this.operationTimeout);
                } else if (mail.get(0) instanceof GetCacheMessage) {
                    this.messageDrivers.getGetCacheMessage().execute((GetCacheMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender,
                            this.operationTimeout);
                } else if (mail.get(0) instanceof GetOrUpdateCacheMessage) {
                    this.messageDrivers.getGetOrUpdateCacheMessage().execute((GetOrUpdateCacheMessage) mail.get(0), session.get(), receivedMail.getSourceId(),
                            sender, this.operationTimeout);
                } else if (mail.get(0) instanceof AddCacheMessage) {
                    this.messageDrivers.getAddCacheMessage().execute((AddCacheMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender,
                            this.operationTimeout);
                } else if (mail.get(0) instanceof PatchOrAddAndGetCacheMessage) {
                    this.messageDrivers.getPatchOrAddAndGetCacheMessage().execute((PatchOrAddAndGetCacheMessage) mail.get(0), session.get(),
                            receivedMail.getSourceId(), sender, this.operationTimeout);
                } else if (mail.get(0) instanceof CheckStockMessage) {
                    this.messageDrivers.getCheckStockMessage().execute((CheckStockMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender);
                } else if (mail.get(0) instanceof CheckDemandMessage) {
                    this.messageDrivers.getCheckDemandMessage().execute((CheckDemandMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender);
                } else if (mail.get(0) instanceof RecoveryMessage) {
                    this.messageDrivers.getRecoveryMessage().execute((RecoveryMessage) mail.get(0), session.get(), receivedMail.getSourceId(), sender);
                } else if (mail.get(0) instanceof BackupMessage) {
                    this.messageDrivers.getBackupMessage().execute((BackupMessage<?>) mail.get(0), session.get(), receivedMail.getSourceId(), sender);
                } else if (mail.get(0) instanceof SimpleRecoveryMessage) {
                    this.messageDrivers.getSimpleRecoveryMessage().execute((SimpleRecoveryMessage) mail.get(0), session.get(), receivedMail.getSourceId(),
                            sender);
                } else if (mail.get(0) instanceof CheckOneDemandMessage) {
                    this.messageDrivers.getCheckOneDemandMessage().execute((CheckOneDemandMessage) mail.get(0), session.get(), receivedMail.getSourceId(),
                            sender);
                } else {
                    done = false;
                }

                if (done) {
                    LOG.log(Level.FINER, "{0} から受信した {1} を捌きました。", new Object[] { sender, mail.get(0) });
                } else {
                    LOG.log(Level.WARNING, "返信待ちの {0} に対する処理は実装されていません。", mail.get(0).getClass().getName());
                }
            } else {
                LOG.log(Level.WARNING, "{0} に対する処理は実装されていません。", mail.get(0).getClass().getName());
            }
        }

        return null;
    }

}
