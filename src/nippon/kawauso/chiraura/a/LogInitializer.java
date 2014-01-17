/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.util.logging.LogManager;

/**
 * @author chirauraNoSakusha
 */
public final class LogInitializer {

    /**
     * 終了前に handler がリセットされてしまうのを防ぐための場当たり的な措置。
     * @author chirauraNoSakusha
     */
    public static final class ManualResetLogManager extends LogManager {
        @Override
        public void reset() {}

        void manualReset() {
            super.reset();
        }
    }

    static {
        System.setProperty("java.util.logging.manager", ManualResetLogManager.class.getName());
    }

    static void init() {}

    static synchronized void reset() {
        final LogManager logManager = LogManager.getLogManager();
        if (logManager instanceof ManualResetLogManager) {
            ((ManualResetLogManager) logManager).manualReset();
        }
    }

}
