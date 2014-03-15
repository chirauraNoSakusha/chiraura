package nippon.kawauso.chiraura.messenger;

/**
 * 接続種別の指針。
 * @author chirauraNoSakusha
 */
public final class ConnectionTypes {

    /**
     * 素。
     */
    public static final int DEFAULT = 0;

    /**
     * 制御。
     */
    public static final int CONTROL = DEFAULT;

    /**
     * データ。
     */
    public static final int DATA = CONTROL;
    // TODO CONTROL と変えてもいいか？でも、接続数が。

}
