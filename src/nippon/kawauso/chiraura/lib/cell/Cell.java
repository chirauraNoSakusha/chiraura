/**
 * 
 */
package nippon.kawauso.chiraura.lib.cell;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 * @param <T> 中身のクラス
 */
public interface Cell<T> extends BytesConvertible {

    /**
     * @return 中身を返す
     */
    public T get();

}
