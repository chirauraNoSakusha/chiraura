/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.KeyPair;

/**
 * 公開鍵暗号用の鍵を管理する。
 * @author chirauraNoSakusha
 */
final class PublicKeyManager {

    private final long lifetime;

    // 以下、this で保護。
    private long updateDate;
    private KeyPair key;

    PublicKeyManager(final long lifetime) {
        if (lifetime < 0) {
            throw new IllegalArgumentException("Invalid lifetime ( " + lifetime + " ).");
        }
        this.lifetime = lifetime;
        this.updateDate = System.currentTimeMillis();
        this.key = CryptographicKeys.newPublicKeyPair();
    }

    KeyPair getPublicKeyPair() {
        synchronized (this) {
            if (System.currentTimeMillis() <= this.updateDate + this.lifetime) {
                return this.key;
            }
            // 先に日時を更新して冗長実行を避ける。
            this.updateDate = System.currentTimeMillis();
        }

        final KeyPair newKey = CryptographicKeys.newPublicKeyPair();

        synchronized (this) {
            this.key = newKey;
            return this.key;
        }
    }

}
