/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

import java.util.Map;

/**
 * 一番挿入が古い要素の削除が可能なマップ。
 * @author chirauraNoSakusha
 * @param <K> キーのクラス
 * @param <V> 値のクラス
 */
public interface InputOrderedMap<K, V> extends Map<K, V> {

    /**
     * 一番挿入が古い要素を返す。
     * @return 一番挿入が古い要素。
     *         空の場合は null
     */
    public Map.Entry<K, V> getEldest();

    /**
     * 一番挿入が古い要素を削除する。
     * @return 一番挿入が古い要素。
     *         空の場合は null
     */
    public Map.Entry<K, V> removeEldest();

}
