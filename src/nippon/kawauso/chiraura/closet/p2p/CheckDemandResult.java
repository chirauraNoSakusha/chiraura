/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;

/**
 * 需要確認の結果。
 * 1. 需要のあるデータ片の一覧。
 * 2. 諦め。
 * @author chirauraNoSakusha
 */
final class CheckDemandResult {

    private final boolean giveUp;
    private final List<DemandEntry> demanded;

    private CheckDemandResult(final boolean giveUp, final List<DemandEntry> demanded) {
        this.giveUp = giveUp;
        this.demanded = demanded;
    }

    static CheckDemandResult newGiveUp() {
        return new CheckDemandResult(true, null);
    }

    CheckDemandResult(final List<DemandEntry> demanded) {
        this(false, demanded);
        if (demanded == null) {
            throw new IllegalArgumentException("Null demanded entries.");
        }
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    List<DemandEntry> getDemandedEntries() {
        return this.demanded;
    }

}
