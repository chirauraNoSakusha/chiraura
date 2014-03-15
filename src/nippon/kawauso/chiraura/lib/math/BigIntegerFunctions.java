package nippon.kawauso.chiraura.lib.math;

import java.math.BigInteger;

/**
 * @author chirauraNoSakusha
 */
public final class BigIntegerFunctions {

    // インスタンス化防止。
    private BigIntegerFunctions() {}

    /**
     * 立っている最上位ビットの位置を返す。
     * 立っているビットがある場合は 0 以上の値が返る。
     * 例えば、value が 1 なら 0、value が 2 なら 1。
     * @param value 値
     * @return 立っている最上位ビットの位置。
     *         立っているビットがない場合は -1
     */
    public static int highestSetBit(final BigInteger value) {
        return value.bitLength() - 1;
    }

}
