/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.messenger.CryptographicKeys;

/**
 * @author chirauraNoSakusha
 */
final class IdIo {

    // インスタンス化防止。
    private IdIo() {}

    private static final Logger LOG = Logger.getLogger(IdIo.class.getName());

    private static void toStream(final KeyPair id, final OutputStream output) throws IOException {
        BytesConversion.toStream(output, "abab", id.getPublic().getEncoded(), id.getPrivate().getEncoded());
    }

    static void toFile(final KeyPair id, final File output) throws IOException {
        if (!output.exists()) {
            if (output.createNewFile()) {
                LOG.log(Level.INFO, "{0} を作成しました。", output.getPath());
            }
        }
        try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(output))) {
            toStream(id, stream);
        }
    }

    private static KeyPair fromStream(final InputStream input) throws MyRuleException, IOException {
        final byte[][] publicKey = new byte[1][];
        final byte[][] privateKey = new byte[1][];
        BytesConversion.fromStream(input, Integer.MAX_VALUE, "abab", publicKey, privateKey);
        return new KeyPair(CryptographicKeys.getPublicKey(publicKey[0]), CryptographicKeys.getPrivateKey(privateKey[0]));
    }

    static KeyPair fromFile(final File input) {
        if (!input.exists()) {
            return null;
        }
        try (InputStream stream = new BufferedInputStream(new FileInputStream(input))) {
            return fromStream(stream);
        } catch (final MyRuleException | IOException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            LOG.log(Level.WARNING, "異常が発生しました", e);
            if (!input.renameTo(backup)) {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存しました。", new Object[] { input.getPath(), backup.getPath() });
            } else {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存するこもできませんでした。", new Object[] { input.getPath(), backup.getPath() });
            }
            return null;
        }
    }

}
