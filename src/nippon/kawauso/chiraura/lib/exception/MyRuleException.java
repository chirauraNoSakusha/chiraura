package nippon.kawauso.chiraura.lib.exception;

/**
 * 俺様規約違反。
 * @author chirauraNoSakusha
 */
public final class MyRuleException extends Exception {

    private static final long serialVersionUID = -3236213225722478749L;

    /**
     * 内容を指定して作成。
     * @param message 内容
     */
    public MyRuleException(final String message) {
        super(message);
    }

    /**
     * 原因を指定して作成。
     * @param cause 原因
     */
    public MyRuleException(final Throwable cause) {
        super(cause);
    }

}
