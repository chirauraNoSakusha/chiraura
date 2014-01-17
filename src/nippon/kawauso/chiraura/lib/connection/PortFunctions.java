package nippon.kawauso.chiraura.lib.connection;

/**
 * @author chirauraNoSakusha
 */
public final class PortFunctions {

    // インスタンス化防止。
    private PortFunctions() {}

    /**
     * 最大値。
     */
    public static final int MAX_VALUE = (1 << 16) - 1;

    /**
     * 最小値。
     */
    public static final int MIN_VALUE = 0;

    /**
     * ポート番号の範囲に収まるかどうの検査。
     * @param value 検査値
     * @return 収まる場合 true
     */
    public static boolean isValid(final int value) {
        return MIN_VALUE <= value && value <= MAX_VALUE;
    }

    /**
     * ポート番号を short にエンコードする。
     * @param port ポート番号
     * @return ポート番号を表す short 値
     */
    public static short encodeToShort(final int port) {
        return (short) port;
    }

    /**
     * short にエンコードされたポート番号を復元する。
     * @param port short にエンコードされたポート番号
     * @return ポート番号
     */
    public static int decodeFromShort(final short port) {
        return port & ((1 << Short.SIZE) - 1);
    }

}
