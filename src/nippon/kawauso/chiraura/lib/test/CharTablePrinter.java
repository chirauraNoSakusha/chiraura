/**
 * 
 */
package nippon.kawauso.chiraura.lib.test;

import java.util.Arrays;

import nippon.kawauso.chiraura.lib.StringFunctions;

/**
 * @author chirauraNoSakusha
 */
final class CharTablePrinter {

    public static void main(final String[] args) {
        final char[] array = CharTable.TABLE.toCharArray();
        Arrays.sort(array);
        final int line = 100;
        int n = 0;
        for (final char c : array) {
            if (n == 0) {
                System.out.print("+ \"");
            }
            final String s = String.valueOf(c);
            System.out.print(s);
            n += StringFunctions.getWidth(s);
            if (n > line) {
                n = 0;
                System.out.println("\"");
            }
        }
        System.out.println();
    }

}
