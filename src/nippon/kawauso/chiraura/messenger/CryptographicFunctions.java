/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author chirauraNoSakusha
 */
final class CryptographicFunctions {

    // インスタンス化防止。
    private CryptographicFunctions() {}

    static byte[] encrypt(final Key key, final byte[] input) {
        try {
            final Cipher encrypter = Cipher.getInstance(key.getAlgorithm());
            encrypter.init(Cipher.ENCRYPT_MODE, key);
            return encrypter.doFinal(input);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static byte[] decrypt(final Key key, final byte[] input) {
        try {
            final Cipher decrypter = Cipher.getInstance(key.getAlgorithm());
            decrypter.init(Cipher.DECRYPT_MODE, key);
            return decrypter.doFinal(input);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
