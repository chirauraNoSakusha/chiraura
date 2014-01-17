/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.regex.Pattern;

/**
 * @author chirauraNoSakusha
 */
final class ContentConstants {

    // インスタンス化防止。
    private ContentConstants() {}

    /**
     * 本文中での改行。
     */
    static final String SEPARATOR_SYMBOL = "<BR>";

    /**
     * 不正な文字の表現。
     */
    static final Pattern INVALID_PATTERN = Pattern.compile("[\\p{Cntrl}<>]"); // 制御文字と'<'と'>'。

    /**
     * 中身の無い状態の表現。
     */
    static final Pattern EMPTY_PATTERN = Pattern.compile("^\\s*$");

    /**
     * 板名の最大文字数。
     */
    static final int BOARD_LENGTH_LIMIT = 40; // 原稿用紙 2 行分。

    /**
     * スレタイトルの最大文字数。
     */
    static final int TITLE_LENGTH_LIMIT = 400; // 原稿用紙 1 枚分。

    /**
     * 名前の最大文字数。
     */
    static final int AUTHOR_LENGTH_LIMIT = 100; // 原稿用紙 5 行分。

    /**
     * メールアドレスの最大文字数。
     */
    static final int MAIL_LENGTH_LIMIT = 100; // 原稿用紙 5 行分。

    /**
     * 本文の最大文字数。
     */
    static final int MESSAGE_LENGTH_LIMIT = 4_000; // 原稿用紙 10 枚分。

    /**
     * 板の最大出力行数。
     */
    static final int BOARD_OUTPUT_LIMIT = 1_000;

    /**
     * 最大バイト数。
     */
    static final int BYTE_SIZE_LIMIT = 1024 * 1024; // 1 MB.

}
