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

    /**
     * @return text/plain など
     */
    String getContentType();

}
