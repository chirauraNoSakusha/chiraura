package nippon.kawauso.chiraura.bbs;

import java.net.InetAddress;

import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * 2chブラウザからのスレ作成リクエスト。
 * @author chirauraNoSakusha
 */
final class AddThreadRequest implements Request {

    private final String board;
    private final String title;
    private final String author;
    private final String mail;
    private final long date;
    private final String comment;

    private final InetAddress source;

    AddThreadRequest(final String board, final String title, final String author, final String mail, final long date, final String comment,
            final InetAddress source) {
        if (board == null) {
            throw new IllegalArgumentException("Null board.");
        } else if (title == null) {
            throw new IllegalArgumentException("Null title.");
        } else if (author == null) {
            throw new IllegalArgumentException("Null author.");
        } else if (mail == null) {
            throw new IllegalArgumentException("Null mail.");
        } else if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        } else if (source == null) {
            throw new IllegalArgumentException("Null source.");
        }

        this.board = board;
        this.title = title;
        this.author = author;
        this.mail = mail;
        this.date = date;
        this.comment = comment;

        this.source = source;
    }

    String getBoard() {
        return this.board;
    }

    String getTitle() {
        return this.title;
    }

    String getAuthor() {
        return this.author;
    }

    String getMail() {
        return this.mail;
    }

    long getDate() {
        return this.date;
    }

    String getComment() {
        return this.comment;
    }

    InetAddress getSource() {
        return this.source;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.board)
                .append(", ").append(this.title)
                .append(", ").append(this.author)
                .append(", ").append(this.mail)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(", commentLength=").append(this.comment.length())
                .append(']').toString();
    }

}
