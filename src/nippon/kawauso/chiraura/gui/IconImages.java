/**
 * 
 */
package nippon.kawauso.chiraura.gui;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * @author chirauraNoSakusha
 */
final class IconImages {

    // インスタンス化防止。
    private IconImages() {}

    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;

    static Image getLogo() {
        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (final Point point : new Point[] {
                new Point(5, 1),
                new Point(6, 1),
                new Point(6, 2),
                new Point(7, 2),
                new Point(6, 3),
                new Point(7, 3),
                new Point(9, 3),
                new Point(10, 3),
                new Point(11, 3),
                new Point(2, 4),
                new Point(3, 4),
                new Point(6, 4),
                new Point(7, 4),
                new Point(8, 4),
                new Point(9, 4),
                new Point(3, 5),
                new Point(4, 5),
                new Point(5, 5),
                new Point(6, 5),
                new Point(7, 5),
                new Point(5, 6),
                new Point(6, 6),
                new Point(5, 7),
                new Point(6, 7),
                new Point(9, 7),
                new Point(10, 7),
                new Point(11, 7),
                new Point(5, 8),
                new Point(6, 8),
                new Point(7, 8),
                new Point(8, 8),
                new Point(9, 8),
                new Point(11, 8),
                new Point(12, 8),
                new Point(4, 9),
                new Point(5, 9),
                new Point(6, 9),
                new Point(7, 9),
                new Point(12, 9),
                new Point(13, 9),
                new Point(4, 10),
                new Point(5, 10),
                new Point(12, 10),
                new Point(13, 10),
                new Point(12, 11),
                new Point(13, 11),
                new Point(11, 12),
                new Point(12, 12),
                new Point(9, 13),
                new Point(10, 13),
                new Point(11, 13),
                new Point(6, 14),
                new Point(7, 14),
                new Point(8, 14),
                new Point(9, 14),
        }) {
            image.setRGB(point.x, point.y, 0xff_00_00_00);
        }
        return image;
    }

}
