/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.KeyPair;
import java.util.Arrays;

/**
 * 通信係の作成とか。
 * @author chirauraNoSakusha
 */
public final class Messengers {

    // インスタンス化防止。
    private Messengers() {}

    /**
     * 通信係を作成する。
     * @param port 受け付けポート番号
     * @param receveBufferSize 受信バッファのバイトサイズ
     * @param sendBufferSize 送信バッファのバイトサイズ
     * @param connectionTimeout 無通信接続の切断猶予時間 (ミリ秒)
     * @param operationTimeout 規約通信の応答制限時間 (ミリ秒)
     * @param messageSizeLimit メッセージの最大バイトサイズ
     * @param version バージョン番号
     * @param versionGapThreshold 弾く
     * @param id 自身の識別用鍵
     * @param publicKeyLifetime 通信用公開鍵の使い回し期間 (ミリ秒)
     * @param commonKeyLifetime 通信用共通鍵の使い回し期間 (ミリ秒)
     * @return 通信係
     */
    public static Messenger newInstance(final int port, final int receveBufferSize, final int sendBufferSize, final long connectionTimeout,
            final long operationTimeout, final int messageSizeLimit, final long version, final long versionGapThreshold, final KeyPair id,
            final long publicKeyLifetime, final long commonKeyLifetime) {
        return new ThreadMessenger(port, receveBufferSize, sendBufferSize, connectionTimeout, operationTimeout, messageSizeLimit, version, versionGapThreshold,
                id, publicKeyLifetime, commonKeyLifetime);
    }

    /**
     * JCE が制限されているどうか検査。
     * @return 制限されていれば true
     */
    public static boolean isLimitedJce() {
        final byte[] before = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, };
        final byte[] after;
        try {
            final byte[] encrypted = CryptographicFunctions.encrypt(CryptographicKeys.getConstantKey(), before);
            after = CryptographicFunctions.decrypt(CryptographicKeys.getConstantKey(), encrypted);
        } catch (final Exception e) {
            return true;
        }
        return !Arrays.equals(before, after);
    }

    // public static void main(final String[] args) {
    // System.out.println(isLimitedJce());
    // }

}
