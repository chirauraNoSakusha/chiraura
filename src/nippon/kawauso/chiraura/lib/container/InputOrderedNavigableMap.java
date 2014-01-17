/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

import java.util.NavigableMap;

/**
 * 一番挿入が古い要素の削除が可能な順序マップ。
 * @author chirauraNoSakusha
 * @param <K> キーのクラス
 * @param <V> 値のクラス
 */
public interface InputOrderedNavigableMap<K, V> extends NavigableMap<K, V>, InputOrderedMap<K, V> {}
