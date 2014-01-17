/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.security.PublicKey;

import nippon.kawauso.chiraura.lib.base.Address;

/**
 * 個体の論理位置の計算機。
 * @author chirauraNoSakusha
 */
public interface AddressCalculator {

    /**
     * 個体の論理位置を計算する。
     * @param id 個体の識別子
     * @return 個体の論理位置
     */
    Address calculate(final PublicKey id);

}
