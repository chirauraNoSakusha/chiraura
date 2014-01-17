/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;

/**
 * 在庫確認の結果。
 * 1. 在庫のあるデータ片の一覧。
 * 2. 諦め。
 * @author chirauraNoSakusha
 */
final class CheckStockResult {

    private final boolean giveUp;
    private final List<StockEntry> stocked;

    private CheckStockResult(final boolean giveUp, final List<StockEntry> stocked) {
        this.giveUp = giveUp;
        this.stocked = stocked;
    }

    static CheckStockResult newGiveUp() {
        return new CheckStockResult(true, null);
    }

    CheckStockResult(final List<StockEntry> stocked) {
        this(false, stocked);
        if (stocked == null) {
            throw new IllegalArgumentException("Null stocked entries.");
        }
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    List<StockEntry> getStockedEntries() {
        return this.stocked;
    }

}
