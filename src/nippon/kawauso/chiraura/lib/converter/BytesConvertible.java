/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * バイト列への変換をサポートする。
 * Serializable を理解し、準拠するのがめんどいため。他言語への移植性も無いし。
 * @author chirauraNoSakusha
 */
public interface BytesConvertible {

    /**
     * 変換後のバイト列のサイズ。
     * @return バイト数
     */
    public int byteSize();

    /**
     * バイト列に変換して書き込む。
     * @param output 出力先
     * @return 出力サイズ
     * @throws IOException 書き込みエラー
     */
    public int toStream(OutputStream output) throws IOException;

    /**
     * バイト列からの復号器。
     * @param <T> 復元対象の型
     */
    public interface Parser<T> {

        /**
         * バイト列から復元する。
         * @param input バイト列の読み込み元
         * @param maxByteSize 読み込む最大バイト数
         * @param output 復号した物の格納先
         * @return 読み込んだバイト数
         * @throws MyRuleException 不正なバイト列だった場合。
         *             maxByteSize 以内に復号が終わらなかった場合
         * @throws IOException 読み込みエラー
         */
        public int fromStream(InputStream input, int maxByteSize, List<? super T> output) throws MyRuleException, IOException;

    }

}
