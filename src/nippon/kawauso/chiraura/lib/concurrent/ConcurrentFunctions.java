/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author chirauraNoSakusha
 */
public final class ConcurrentFunctions {

    // インスタンス化防止。
    private ConcurrentFunctions() {}

    /**
     * インタラプトで中止することなく、ブロッキングキューにものを突っ込む。
     * @param <T> 要素のクラス
     * @param object 入れる要素
     * @param queue 対象のキュー
     */
    public static <T> void completePut(final T object, final BlockingQueue<T> queue) {
        boolean isInterrupted = false;

        while (true) {
            try {
                queue.put(object);
                break;
            } catch (final InterruptedException e) {
                isInterrupted = true;
            }
        }

        if (isInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * インタラプトで中止することなく、エグゼキュータの終了を待つ。
     * @param executor 終了するエグゼキュータ
     */
    public static void completeAwaitTermination(final ExecutorService executor) {
        boolean isInterrupted = false;
        while (true) {
            try {
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                    break;
                }
            } catch (final InterruptedException e) {
                isInterrupted = true;
            }
        }
        if (isInterrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
