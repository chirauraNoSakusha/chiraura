/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class StartingProtocol {

    private static final Logger LOG = Logger.getLogger(StartingProtocol.class.getName());

    // インスタンス化防止。
    private StartingProtocol() {}

    static void sendFirst(final Transceiver transceiver, final OutputStream output, final PublicKey contactorPublicKey) throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new FirstMessage(contactorPublicKey));
        final int size = transceiver.toStream(output, mail, EncryptedWithRandomEnvelope.class, CryptographicKeys.getConstantKey());
        output.flush();
        LOG.log(Level.FINEST, "一言目を {0} バイト送信。", Integer.toString(size));
    }

    static Message receiveFirst(final Transceiver transceiver, final InputStream input) throws MyRuleException, IOException {
        final List<Message> mail = new ArrayList<>(1);
        final int size = transceiver.fromStream(input, CryptographicKeys.getConstantKey(), mail);
        if (mail.get(0) instanceof FirstMessage) {
            LOG.log(Level.FINEST, "一言目を {0} バイト受信。", Integer.toString(size));
        } else if (mail.get(0) instanceof PortCheckMessage) {
            LOG.log(Level.FINEST, "ポート検査を {0} バイト受信。", Integer.toString(size));
        } else {
            throw new MyRuleException("Not first message ( " + mail.get(0).getClass().getName() + " ).");
        }
        return mail.get(0);
    }

    static void sendFirstReply(final Transceiver transceiver, final OutputStream output, final PublicKey contactorPublicKey, final Key communicationKey)
            throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new FirstReply(communicationKey));
        final int size = transceiver.toStream(output, mail, EncryptedEnvelope.class, contactorPublicKey);
        output.flush();
        LOG.log(Level.FINEST, "一言目への相槌を {0} バイト送信。", Integer.toString(size));
    }

    static FirstReply receiveFirstReply(final Transceiver transceiver, final InputStream input, final PrivateKey contactorPrivateKey) throws MyRuleException,
            IOException {
        final List<Message> mail = new ArrayList<>(1);
        final int size = transceiver.fromStream(input, contactorPrivateKey, mail);
        LOG.log(Level.FINEST, "一言目への相槌を {0} バイト受信。", Integer.toString(size));
        if (mail.get(0) instanceof FirstReply) {
            return (FirstReply) mail.get(0);
        } else {
            throw new MyRuleException("Not first reply ( " + mail.get(0).getClass().getName() + " ).");
        }
    }

    static void sendSecond(final Transceiver transceiver, final OutputStream output, final KeyPair contactorIdPair, final Key communicationKey,
            final byte[] watchword, final long contactorVersion, final int contactorPort, final int connectionType, final InetSocketAddress acceptor)
            throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new SecondMessage(contactorIdPair, communicationKey, watchword, contactorVersion, contactorPort, connectionType, acceptor));
        final int size = transceiver.toStream(output, mail, EncryptedEnvelope.class, communicationKey);
        output.flush();
        LOG.log(Level.FINEST, "二言目を {0} バイト送信。", Integer.toString(size));
    }

    static SecondMessage receiveSecond(final Transceiver transceiver, final InputStream input, final Key communicationKey) throws MyRuleException, IOException {
        final List<Message> mail = new ArrayList<>(1);
        final int size = transceiver.fromStream(input, communicationKey, mail);
        LOG.log(Level.FINEST, "二言目を {0} バイト受信。", Integer.toString(size));
        if (mail.get(0) instanceof SecondMessage) {
            return (SecondMessage) mail.get(0);
        } else {
            throw new MyRuleException("Not second message ( " + mail.get(0).getClass().getName() + " ).");
        }
    }

    static void sendSecondReply(final Transceiver transceiver, final OutputStream output, final Key communicationKey, final KeyPair acceptorId,
            final byte[] watchword, final PublicKey acceptorPublicKey, final long acceptorVersion, final InetSocketAddress contactor) throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new SecondReply(acceptorId, watchword, acceptorPublicKey, acceptorVersion, contactor));
        final int size = transceiver.toStream(output, mail, EncryptedEnvelope.class, communicationKey);
        output.flush();
        LOG.log(Level.FINEST, "二言目への相槌を {0} バイト送信。", Integer.toString(size));
    }

    static Message receiveSecondReply(final Transceiver transceiver, final InputStream input, final Key communicationKey) throws MyRuleException, IOException {
        final List<Message> mail = new ArrayList<>(1);
        final int size = transceiver.fromStream(input, communicationKey, mail);
        if (mail.get(0) instanceof SecondReply) {
            LOG.log(Level.FINEST, "二言目への相槌を {0} バイト受信。", Integer.toString(size));
        } else if (mail.get(0) instanceof PortErrorMessage) {
            LOG.log(Level.FINEST, "ポート異常通知を {0} バイト受信。", Integer.toString(size));
        } else {
            throw new MyRuleException("Not second message ( " + mail.get(0).getClass().getName() + " ).");
        }
        return mail.get(0);
    }

    static void sendPortCheck(final Transceiver transceiver, final OutputStream output, final PublicKey contactorId, final Key communicationKey)
            throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new PortCheckMessage(contactorId, communicationKey));
        final int size = transceiver.toStream(output, mail, EncryptedWithRandomEnvelope.class, CryptographicKeys.getConstantKey());
        output.flush();
        LOG.log(Level.FINEST, "ポート検査を {0} バイト送信。", Integer.toString(size));
    }

    // receivePortCheck は receiveFirst で。

    static void sendPortCheckReply(final Transceiver transceiver, final OutputStream output, final Key communicationKey) throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new PortCheckReply(communicationKey.getEncoded()));
        final int size = transceiver.toStream(output, mail, EncryptedEnvelope.class, communicationKey);
        output.flush();
        LOG.log(Level.FINEST, "ポート検査への応答を {0} バイト送信。", Integer.toString(size));
    }

    static PortCheckReply receivePortCheckReply(final Transceiver transceiver, final InputStream input, final Key communicationKey) throws MyRuleException,
            IOException {
        final List<Message> mail = new ArrayList<>(1);
        final int size = transceiver.fromStream(input, communicationKey, mail);
        LOG.log(Level.FINEST, "ポート検査への応答を {0} バイト受信。", Integer.toString(size));
        if (mail.get(0) instanceof PortCheckReply) {
            return (PortCheckReply) mail.get(0);
        } else {
            throw new MyRuleException("Not port check reply ( " + mail.get(0).getClass().getName() + " ).");
        }
    }

    static void sendPortError(final Transceiver transceiver, final OutputStream output, final Key communicatioKey) throws IOException {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new PortErrorMessage());
        final int size = transceiver.toStream(output, mail, EncryptedEnvelope.class, communicatioKey);
        output.flush();
        LOG.log(Level.FINEST, "ポート異常通知を {0} バイト送信。", Integer.toString(size));
    }

    // receivePortError は receiveSecond で。

}
