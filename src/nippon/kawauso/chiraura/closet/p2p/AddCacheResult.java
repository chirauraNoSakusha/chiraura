package nippon.kawauso.chiraura.closet.p2p;

/**
 * 複製も考えたデータ片追加の結果。
 * 1. 成功。データ片を追加できた。
 * 2. 失敗。既にデータ片があった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class AddCacheResult {

    private final boolean givenUp;
    private final boolean success;
    private final long accessDate;

    private AddCacheResult(final boolean givenUp, final boolean success, final long accessDate) {
        this.givenUp = givenUp;
        this.success = success;
        this.accessDate = accessDate;
    }

    static AddCacheResult newGiveUp() {
        return new AddCacheResult(true, false, 0);
    }

    static AddCacheResult newFailure() {
        return new AddCacheResult(false, false, 0);
    }

    AddCacheResult(final long accessDate) {
        this(false, true, accessDate);
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isSuccess() {
        return this.success;
    }

    long getAccessDate() {
        return this.accessDate;
    }

}
