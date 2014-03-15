package nippon.kawauso.chiraura.bbs;

import java.net.InetSocketAddress;

import nippon.kawauso.chiraura.bbs.Client.BbsBoard;
import nippon.kawauso.chiraura.bbs.Client.BbsThread;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
public final class ClientTest {

    /**
     * @throws Exception 異常
     */
    // @Test
    public void test() throws Exception {
        LoggingFunctions.startDebugLogging();

        // // 厨房板
        // final InetSocketAddress server = new InetSocketAddress("toro.2ch.net", 80);
        // final String boardName = "kitchen";
        // ニュース速報+
        final InetSocketAddress server = new InetSocketAddress("uni.2ch.net", 80);
        final String boardName = "newsplus";

        final BbsBoard board = Client.getBoard(server, boardName);
        // for (final BoardDat.Entry entry : board.getEntries()) {
        // System.out.println("BOARD [ " + entry.getName() + " <> " + entry.getTitle() + " <> " + entry.getNumOfComments() + " ]");
        // }

        Thread.sleep(10 * Duration.SECOND);

        final String threadName = board.getEntries().get(0).getName();
        final BbsThread thread = Client.getThread(server, boardName, threadName);
        // for (final ThreadDat.Entry entry : thread.getEntries()) {
        // System.out.println("THREAD [ " + entry.getAuthor() + " <> " + entry.getMail() + " <> " + entry.getDate() + " <> " + entry.getMessage() + " ]");
        // }

        while (true) {
            Thread.sleep(Duration.MINUTE);

            if (Client.updateBoard(server, board) != board) {
                break;
            }
        }

        BbsThread thread2 = null;
        while (true) {
            Thread.sleep(Duration.MINUTE);
            thread2 = Client.updateThread(server, thread);
            if (thread2 != thread) {
                break;
            }
        }
        if (thread2 == null) {
            System.out.println("ぬるぽ");
        } else {
            for (final Client.BbsThread.Entry entry : thread2.getEntries()) {
                System.out.println("THREAD [ " + entry.getAuthor() + " <> " + entry.getMail() + " <> " + entry.getDate() + " <> " + entry.getMessage() + " ]");
            }
        }
    }

}
