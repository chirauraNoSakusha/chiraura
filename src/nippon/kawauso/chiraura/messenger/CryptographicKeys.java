/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 暗号鍵関連。
 * @author chirauraNoSakusha
 */
public final class CryptographicKeys {

    // インスタンス化防止。
    private CryptographicKeys() {}

    private static final String PUBLIC_KEY_ALGORITHM = "RSA";
    private static final String COMMON_KEY_ALGORITHM = "AES";

    static final int COMMON_KEY_SIZE = 256;
    static final int PUBLIC_KEY_SIZE = 1024;

    private static final Key CONSTANT_KEY;
    static {
        final byte[] buf = new byte[] {
                53, -56, 2, 103, 86, -70, -123, 90,
                -19, -61, 48, 70, 74, -91, -19, 29,
                99, -112, -18, -35, 43, 90, 28, -1,
                -108, -51, -79, -99, -122, -66, 50, 60
        };
        CONSTANT_KEY = new SecretKeySpec(Arrays.copyOf(buf, (COMMON_KEY_SIZE + Byte.SIZE - 1) / Byte.SIZE), COMMON_KEY_ALGORITHM);
    }

    static Key getConstantKey() {
        return CONSTANT_KEY;
    }

    /**
     * バイト列を公開鍵形式にする。
     * @param bytes 公開鍵を表すバイト列
     * @return 公開鍵
     * @throws MyRuleException 入力バイト列がおかしかった場合
     */
    public static PublicKey getPublicKey(final byte[] bytes) throws MyRuleException {
        final KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(PUBLIC_KEY_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            // ここには来ないはず。
            throw new RuntimeException(e);
        }
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (final InvalidKeySpecException | RuntimeException e) {
            throw new MyRuleException(e);
        }
    }

    /**
     * バイト列を秘密鍵形式にする。
     * @param bytes 秘密鍵を表すバイト列
     * @return 秘密鍵
     * @throws MyRuleException 入力バイト列がおかしかった場合
     */
    public static PrivateKey getPrivateKey(final byte[] bytes) throws MyRuleException {
        final KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(PUBLIC_KEY_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            // ここには来ないはず。
            throw new RuntimeException(e);
        }
        try {
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (final InvalidKeySpecException e) {
            throw new MyRuleException(e);
        }
    }

    /**
     * バイト列を共有鍵形式にする。
     * @param bytes 共有鍵を表すバイト列
     * @return 共有鍵
     * @throws MyRuleException 入力バイト列がおかしかった場合
     */
    static Key getCommonKey(final byte[] bytes) {
        return new SecretKeySpec(bytes, COMMON_KEY_ALGORITHM);
    }

    private static final SecureRandom random = new SecureRandom();

    /**
     * 適当に共有鍵を作る。
     * @return 共有鍵
     */
    static Key newCommonKey() {
        final byte[] buf = new byte[(COMMON_KEY_SIZE + Byte.SIZE - 1) / Byte.SIZE];
        random.nextBytes(buf);
        return getCommonKey(buf);
    }

    /**
     * 適当に公開鍵と秘密鍵を作る。
     * @return 公開鍵と秘密鍵
     */
    public static KeyPair newPublicKeyPair() {
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance(PUBLIC_KEY_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            // ここには来ないはず。
            throw new RuntimeException(e);
        }
        keyGen.initialize(PUBLIC_KEY_SIZE, random);
        return keyGen.generateKeyPair();
    }

}
