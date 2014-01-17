/**
 * 
 */
package nippon.kawauso.chiraura.util;

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @author chirauraNoSakusha
 */
final class ImageCodeGenerator {

    /**
     * 白地に黒で描かれた画像から、その画像を作成するプログラムコードを作成する。
     * @param args 画像の場所
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    public static void main(final String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("画像が指定されていません。");
            System.exit(1);
        }

        final BufferedImage image = ImageIO.read(new File(args[0]));
        final int[] pixels = new int[image.getWidth() * image.getHeight()];
        final PixelGrabber pg = new PixelGrabber(image, 0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        pg.grabPixels();
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            System.err.println("画像の解析に失敗しました。");
            System.exit(1);
        }

        System.out.println("final BufferedImage image = new BufferedImage(" + image.getWidth() + ", " + image.getHeight() + ", BufferedImage.TYPE_INT_ARGB);");
        System.out.println("for (final Point point : new Point[] {");
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((pixels[y * 16 + x] & 0xff_00_00_00) != 0x00_00_00_00 && (pixels[y * 16 + x] & 0x00_ff_ff_ff) != 0x00_ff_ff_ff) {
                    System.out.print("new Point(" + x + "," + y + "), ");
                }
            }
        }
        System.out.println("}) {");
        System.out.println("image.setRGB(point.x, point.y, 0xff_00_00_00);");
        System.out.println("}");
    }
}
