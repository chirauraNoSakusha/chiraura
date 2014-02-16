/**
 * 
 */
package nippon.kawauso.chiraura.bbs;


/**
 * @author chirauraNoSakusha
 */
interface Content {

    long getUpdateDate();

    long getNetworkTag();

    String toNetworkString();

}
