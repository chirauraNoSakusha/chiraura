package nippon.kawauso.chiraura.messenger;

import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * メッセージを入れる封筒。
 * @author chirauraNoSakusha
 */
interface Envelope extends BytesConvertible {

    /**
     * メッセージの中身を得る。
     * @return メッセージの中身
     */
    List<Message> getMail();

}
