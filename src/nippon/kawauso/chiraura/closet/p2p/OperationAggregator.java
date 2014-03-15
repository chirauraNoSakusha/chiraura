package nippon.kawauso.chiraura.closet.p2p;

/**
 * 同じ操作をまとめる機構。
 * 並列対応。
 * @author chirauraNoSakusha
 */
final class OperationAggregator<K, V> {

    // private static final class Unit<K, V> {
    //
    private final CheckingStation<K, V> container;

    OperationAggregator() {
        this.container = new CheckingStation<>();
    }

    /**
     * 操作を登録する。
     * @param key 操作
     * @return 登録できたら null。
     *         既に登録してあったら、結果取得用の手形
     */
    CheckingStation.Instrument<V> register(final K key) {
        return this.container.register(key);
    }

    /**
     * 結果を渡して操作登録を削除する。
     * @param key 操作
     * @param value 結果
     */
    void free(final K key, final V value) {
        if (!this.container.free(key, value)) {
            throw new IllegalArgumentException("Not registered.");
        }
    }

    static final class TestOperation implements Operation {
        private final int value;

        TestOperation(final int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return this.value;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof TestOperation)) {
                return false;
            }
            final TestOperation other = (TestOperation) obj;
            return this.value == other.value;
        }
    }

    static final class TestResult {
        private final int value;

        TestResult(final int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return this.value;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof TestResult)) {
                return false;
            }
            final TestResult other = (TestResult) obj;
            return this.value == other.value;
        }
    }

}
