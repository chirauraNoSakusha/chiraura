package nippon.kawauso.chiraura.network;

import java.math.BigInteger;

import nippon.kawauso.chiraura.lib.base.Address;


/**
 * @author chirauraNoSakusha
 */
final class CcFunctions {

    /**
     * 部分的観測から個体数を推定する。
     * @param sampleWidth 検知範囲の幅
     * @param sampleNumOfPeers 検知個体数
     * @return 推定個体数
     */
    private static BigInteger estimateNumOfPeers(final BigInteger sampleWidth, final int sampleNumOfPeers) {
        /*
         * sampleNumOfPeers * (globalWidth / sampleWidth)
         */
        return BigInteger.valueOf(sampleNumOfPeers).shiftLeft(Address.SIZE).divide(sampleWidth);
    }

    /**
     * 適切な近傍個体数を推定する。
     * 近傍個体数は、厳密には、個体数を n として、ceil(LOG(n)) が望ましい。
     * @param sampleWidth 検知範囲の幅
     * @param sampleNumOfPeers 検知個体数
     * @return 適切な近傍個体数の推定値
     */
    static int estimateNumOfNeighbors(final BigInteger sampleWidth, final int sampleNumOfPeers) {
        /*
         * 個体数 n が 2 のべき乗の場合だけ、ceil(LOG(n)) は n.bitLength() ではなく、n.bitLength() - 1 であるが、
         * ここでは本来実数であるべき推定個体数を用いているため、
         * 推定個体数 en が整数で 2^k 乗であっても、実際は 2^k 以上 2^k + 1 未満であり、
         * ceil(LOG(en)) は (2^k).bitLength() - 1 より (2^k).bitLength() である可能性の方が高い。
         */
        return estimateNumOfPeers(sampleWidth, sampleNumOfPeers).bitLength();
    }

    /**
     * 2^(n - 1) < distance <= 2^n なる整数 n を返す。
     * @param distance
     * @return 2^(n - 1) < distance <= 2^n なる n
     */
    static int distanceLevel(final Address distance) {
        if (distance.bitCount() == 1) {
            return distance.highestSetBit();
        } else {
            return distance.highestSetBit() + 1;
        }
    }

}
