package nippon.kawauso.chiraura.closet.p2p;

/**
 * 1 つデータ片の需要確認の結果。
 * 1. 需要の詳細。
 * 2. 需要無し。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class CheckOneDemandResult {

    private final boolean giveUp;

    private final DemandEntry demand;

    private CheckOneDemandResult(final boolean giveUp, final DemandEntry demand) {
        this.giveUp = giveUp;
        this.demand = demand;
    }

    static CheckOneDemandResult newGiveUp() {
        return new CheckOneDemandResult(true, null);
    }

    static CheckOneDemandResult newNoDemand() {
        return new CheckOneDemandResult(false, null);
    }

    CheckOneDemandResult(final DemandEntry demand) {
        this(false, demand);
        if (demand == null) {
            throw new IllegalArgumentException("Null demand.");
        }
    }

    boolean isGiveUp() {
        return this.giveUp;
    }

    boolean isNoDemand() {
        return this.demand == null;
    }

    DemandEntry getDemand() {
        return this.demand;
    }

}
