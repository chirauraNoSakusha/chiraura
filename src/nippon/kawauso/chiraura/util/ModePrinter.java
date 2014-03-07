/**
 * 
 */
package nippon.kawauso.chiraura.util;

import nippon.kawauso.chiraura.Global;

/**
 * @author chirauraNoSakusha
 */
final class ModePrinter {

    /**
     * 非制限状態かどうかを標準出力に表示する。
     * @param args 使わない
     */
    public static void main(final String args[]) {
        if (Global.isDebug()) {
            System.out.println("DEBUG");
        } else {
            System.out.println("RELEASE");
        }
    }

}
