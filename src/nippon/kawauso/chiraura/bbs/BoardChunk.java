/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * @author chirauraNoSakusha
 */
interface BoardChunk extends Mountain, Content {

    interface Entry<T extends BoardChunk> extends Mountain.Dust<T> {
        int getNumOfComments();

        long getDate();

        long getOrder();
    }

    Entry<?> getEntry(long thread);

    String toNetworkString();

}
