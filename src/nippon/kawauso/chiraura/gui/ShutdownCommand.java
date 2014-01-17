/**
 * 
 */
package nippon.kawauso.chiraura.gui;

/**
 * 終了命令。
 * @author chirauraNoSakusha
 */
public final class ShutdownCommand implements GuiCommand {

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[]").toString();
    }

}
