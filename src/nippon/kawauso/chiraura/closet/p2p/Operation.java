/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * 操作の識別子。
 * @author chirauraNoSakusha
 */
interface Operation {

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

}
