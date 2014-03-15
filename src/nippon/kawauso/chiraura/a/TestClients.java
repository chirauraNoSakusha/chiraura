package nippon.kawauso.chiraura.a;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.bbs.Client;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class TestClients {

    private static final Logger LOG = Logger.getLogger(TestClients.class.getName());

    // インスタンス化防止。
    private TestClients() {}

    private static int getHeadNumber(final String string) {
        final int index = string.indexOf(' ');
        if (index < 0) {
            return Integer.parseInt(string);
        } else {
            return Integer.parseInt(string.substring(0, index));
        }
    }

    /**
     * スレの単位キャッシュ
     * @author chirauraNoSakusha
     */
    private static final class CacheEntry implements Comparable<CacheEntry> {
        // 板から得られる情報。
        long name;
        String title;
        int numOfComments;

        // スレ
        Client.BbsThread thread;

        CacheEntry(final long name, final String title, final int numOfComments, final Client.BbsThread thread) {
            if (title == null) {
                throw new IllegalArgumentException("Null title.");
            }
            this.name = name;
            this.title = title;
            this.numOfComments = numOfComments;
            this.thread = thread;
        }

        @Override
        public String toString() {
            final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                    .append('[').append(this.name)
                    .append(", ").append(this.title)
                    .append(", ").append(this.numOfComments);
            if (this.thread != null) {
                buff.append('[').append(this.thread.getName())
                        .append(", ").append(this.thread.getTitle())
                        .append(", ").append(this.thread.getEntries().size())
                        .append(']');
            }
            return buff.append(']').toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (this.name ^ (this.name >>> 32));
            result = prime * result + this.title.hashCode();
            result = prime * result + this.numOfComments;
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof CacheEntry)) {
                return false;
            }
            /*
             * スレタイ、スレ名、書き込み数しか見ない。
             */
            final CacheEntry other = (CacheEntry) obj;
            if (this.name != other.name || !this.title.equals(other.title) || this.numOfComments != other.numOfComments) {
                return false;
            } else if (this.thread == null || other.thread == null) {
                return true;
            } else {
                return this.thread.getName().equals(other.thread.getName()) && this.thread.getTitle().equals(other.thread.getTitle())
                        && this.thread.getEntries().size() == other.thread.getEntries().size();
            }
        }

        private int getNumOfComments() {
            if (this.thread == null) {
                return this.numOfComments;
            } else {
                return this.thread.getEntries().size();
            }
        }

        private boolean isFull() {
            return getNumOfComments() >= Client.BbsThread.ENTRY_LIMIT;
        }

        @Override
        public int compareTo(final CacheEntry o) {
            /*
             * 最新スレが一番値が大きい。
             * 埋まってる < 埋まってない。
             * 埋まってないスレタイ大 < 埋まってないスレタイ小。
             * 埋まってないスレ名大 < 埋まってないスレ名小。
             * 埋まってるスレタイ小 < 埋まってるスレタイ大。
             * 埋まってるスレ名小 < 埋まってるスレ名大。
             * 埋まってない書き込み少 < 埋まってない書き込み多。
             */
            if (this.getNumOfComments() < Client.BbsThread.ENTRY_LIMIT) {
                if (o.getNumOfComments() >= Client.BbsThread.ENTRY_LIMIT) {
                    return 1;
                }
                final int title1 = getHeadNumber(this.title);
                final int title2 = getHeadNumber(o.title);
                if (title1 < title2) {
                    return 1;
                } else if (title1 > title2) {
                    return -1;
                } else if (this.name < o.name) {
                    return 1;
                } else if (this.name > o.name) {
                    return -1;
                }
            } else {
                if (o.getNumOfComments() < Client.BbsThread.ENTRY_LIMIT) {
                    return -1;
                }
                final int title1 = getHeadNumber(this.title);
                final int title2 = getHeadNumber(o.title);
                if (title1 < title2) {
                    return -1;
                } else if (title1 > title2) {
                    return 1;
                } else if (this.name < o.name) {
                    return -1;
                } else if (this.name > o.name) {
                    return 1;
                }
            }
            final int flag = compare(this.thread, o.thread);
            if (flag != 0) {
                return flag;
            } else if (this.getNumOfComments() < o.getNumOfComments()) {
                return -1;
            } else if (this.name > o.getNumOfComments()) {
                return 1;
            } else if (this.thread == null || o.thread == null) {
                return 0;
            } else if (this.thread.getEntries().size() < o.thread.getEntries().size()) {
                return -1;
            } else if (this.thread.getEntries().size() > o.thread.getEntries().size()) {
                return 1;
            } else {
                return 0;
            }
        }

        private static int compare(final Client.BbsThread t1, final Client.BbsThread t2) {
            if (t1 == null || t2 == null) {
                return 0;
            }

            if (t1.getEntries().size() < Client.BbsThread.ENTRY_LIMIT) {
                if (t2.getEntries().size() >= Client.BbsThread.ENTRY_LIMIT) {
                    return 1;
                }
                final int title1 = getHeadNumber(t1.getTitle());
                final int title2 = getHeadNumber(t2.getTitle());
                if (title1 < title2) {
                    return 1;
                } else if (title1 > title2) {
                    return -1;
                }
                final long name1 = Long.parseLong(t1.getName());
                final long name2 = Long.parseLong(t2.getName());
                if (name1 < name2) {
                    return 1;
                } else if (name1 > name2) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                if (t2.getEntries().size() < Client.BbsThread.ENTRY_LIMIT) {
                    return -1;
                }
                final int title1 = getHeadNumber(t1.getTitle());
                final int title2 = getHeadNumber(t2.getTitle());
                if (title1 < title2) {
                    return -1;
                } else if (title1 > title2) {
                    return 1;
                }
                final long name1 = Long.parseLong(t1.getName());
                final long name2 = Long.parseLong(t2.getName());
                if (name1 < name2) {
                    return -1;
                } else if (name1 > name2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    private abstract static class TestClient extends Reporter<Void> {

        final InetSocketAddress server;
        final String boardName;

        // スレ名からそのスレ (の情報) へのキャッシュ。
        // スレは板にある分だけキャッシュする。
        private Map<Long, CacheEntry> nameToCache;

        private TestClient(final InetSocketAddress server, final String boardName) {
            super(Level.WARNING);
            if (server == null) {
                throw new IllegalArgumentException("Null server.");
            } else if (boardName == null) {
                throw new IllegalArgumentException("Null board name.");
            }
            this.server = server;
            this.boardName = boardName;
            this.nameToCache = new HashMap<>();
        }

        Map<Long, CacheEntry> getCache() {
            return this.nameToCache;
        }

        private void cacheUpdate(final Client.BbsBoard board) {
            final Map<Long, CacheEntry> oldCache = this.nameToCache;
            this.nameToCache = new HashMap<>();
            for (final Client.BbsBoard.Entry entry : board.getEntries()) {
                final long name = Long.parseLong(entry.getName());
                final CacheEntry old = oldCache.get(name);
                if (old == null) {
                    this.nameToCache.put(name, new CacheEntry(name, entry.getTitle(), Integer.parseInt(entry.getNumOfComments()), null));
                } else {
                    this.nameToCache.put(name, new CacheEntry(name, entry.getTitle(), Integer.parseInt(entry.getNumOfComments()), old.thread));
                }
            }
        }

        /**
         * @throws InterruptedException 割り込まれ
         */
        boolean updateBoard() throws MyRuleException, IOException, InterruptedException {
            final Client.BbsBoard board = Client.getBoard(this.server, this.boardName);
            if (board != null) {
                cacheUpdate(board);
                return true;
            } else {
                return false;
            }
        }

        /**
         * @throws InterruptedException 割り込まれ
         */
        boolean updateThread(final long name) throws MyRuleException, IOException, InterruptedException {
            // name はキャッシュされているもの (取得した板にあったもの) だけ可。
            final CacheEntry old = this.nameToCache.get(name);
            if (old.thread == null) {
                final Client.BbsThread thread = Client.getThread(this.server, this.boardName, Long.toString(name));
                if (thread != null) {
                    this.nameToCache.put(name, new CacheEntry(old.name, old.title, old.numOfComments, thread));
                    return true;
                } else {
                    return false;
                }
            } else {
                final Client.BbsThread thread = Client.updateThread(this.server, old.thread);
                if (thread != null) {
                    if (thread != old.thread) {
                        this.nameToCache.put(name, new CacheEntry(old.name, old.title, old.numOfComments, thread));
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }

    }

    /**
     * 最新スレに読み書きして、埋まったら新しいスレをつくっていくクライアントをつくる。
     * @param server 2ch サーバ
     * @param boardName 板名
     * @param interval 動作間隔
     * @param writeRate 書き込み率
     * @param author 書き込み時に名乗る名前
     * @return クライアント
     */
    static Callable<Void> newSequential(final InetSocketAddress server, final String boardName, final long interval, final long maintenanceInterval,
            final long cacheDuration, final double writeRate, final String author) {
        final long operationSleep = Math.min(Duration.SECOND, interval / 10);
        return new TestClient(server, boardName) {

            private final Set<CacheEntry> blacklist = new LinkedHashSet<>();

            /**
             * 現行のスレを返す。
             * @return 全スレが埋まっている場合は最後のスレ。
             *         そうでない場合は 埋まっていない最初のスレ
             */
            private CacheEntry getCurrent() {
                CacheEntry max = null;
                for (final Map.Entry<Long, CacheEntry> entry : getCache().entrySet()) {
                    final CacheEntry cur = entry.getValue();
                    if (max == null || max.compareTo(cur) < 0) {
                        max = cur;
                    }
                }
                return max;
            }

            private boolean addThread(final CacheEntry previous) throws MyRuleException, IOException, InterruptedException {
                final String title;
                if (previous == null) {
                    title = "1 スレ目";
                } else {
                    title = (getHeadNumber(previous.title) + 1) + " スレ目";
                }
                final String mail = "age";
                final String message = "1 レス目。" + getCache().size() + " スレ把握。";

                if (Client.addThread(this.server, this.boardName, title, author, mail, message)) {
                    Thread.sleep(operationSleep);
                    return true;
                } else {
                    Thread.sleep(operationSleep);
                    return false;
                }
            }

            private boolean addComment(final long name) throws MyRuleException, IOException, InterruptedException {
                final CacheEntry cache = getCache().get(name);
                final String mail = (ThreadLocalRandom.current().nextInt(2) == 0 ? "" : "sage");
                final String message;
                if (cache.thread == null) {
                    message = "1 レス目。レス把握せず。";
                } else {
                    final List<Client.BbsThread.Entry> entries = cache.thread.getEntries();
                    message = (getHeadNumber(entries.get(entries.size() - 1).getMessage()) + 1) + " レス目。" + entries.size() + " レス把握。";
                }

                if (Client.addComment(this.server, this.boardName, Long.toString(name), author, mail, message)) {
                    Thread.sleep(operationSleep);
                    return true;
                } else {
                    // 書き込めないなら除外。
                    getCache().remove(cache.name);
                    this.blacklist.add(cache);
                    LOG.log(Level.FINEST, "レス ( {0} )を追加できなかったので、{1} を除外します。", new Object[] { message, cache });
                    Thread.sleep(operationSleep);
                    return true;
                }
            }

            @Override
            boolean updateBoard() throws MyRuleException, IOException, InterruptedException {
                final boolean result = super.updateBoard();
                if (result) {
                    for (final Iterator<Map.Entry<Long, CacheEntry>> iterator = getCache().entrySet().iterator(); iterator.hasNext();) {
                        final Map.Entry<Long, CacheEntry> entry = iterator.next();
                        if (this.blacklist.contains(entry.getValue())) {
                            iterator.remove();
                        }
                    }
                    Thread.sleep(operationSleep);
                    return true;
                } else {
                    Thread.sleep(operationSleep);
                    return false;
                }
            }

            @Override
            boolean updateThread(final long name) throws MyRuleException, IOException, InterruptedException {
                final boolean result = super.updateThread(name);
                if (result) {
                    Thread.sleep(operationSleep);
                    return true;
                } else {
                    // 取得できないなら除外。
                    final CacheEntry cache = getCache().remove(name);
                    this.blacklist.add(cache);
                    LOG.log(Level.FINEST, "取得できなかったので、{0} を除外します。", new Object[] { cache });
                    Thread.sleep(operationSleep);
                    return false;
                }
            }

            private CacheEntry searchCurrent() throws MyRuleException, IOException, InterruptedException {
                // 現行スレを一から探す。
                this.blacklist.clear();
                updateBoard();

                CacheEntry pre = null;
                CacheEntry current;
                while (true) {
                    /*
                     * ループを回すことで取得できない現行スレがブラックリストに登録されていく。
                     */
                    current = getCurrent();
                    if (current == null || current.isFull()) {
                        // やっぱり現行スレは無かった。
                        break;
                    } else if (current.equals(pre)) {
                        // 現行スレがあった。
                        break;
                    }
                    updateThread(current.name);
                    pre = current;
                }
                return current;
            }

            @Override
            protected Void subCall() throws InterruptedException, MyRuleException, IOException {

                // 最初に板取得。
                while (!updateBoard()) {
                    Thread.sleep(interval);
                }

                while (!Thread.currentThread().isInterrupted()) {
                    // 現行スレを探す。
                    CacheEntry current = getCurrent();

                    if (current == null || current.isFull()) {
                        // スレが無い、または、現行スレが埋まった。
                        // 立てる前にしばし落ち着く。スレ立ては慎重に。
                        /*
                         * 充分に待つのは、新規参加個体が管理者のときに同期が終わるのを待つため。
                         * 無作為にずらすのは、同じ間隔で複数の個体が新スレを立てようとすると、
                         * 新スレが板に登録される前につくられた板の複製を見て、
                         * 別の個体も新スレを立ててしまう確率が高くなるから。
                         */
                        final long coolDownTime = 6 * maintenanceInterval / 5 + ThreadLocalRandom.current().nextLong(3 * cacheDuration);
                        boolean valid = true;
                        final CacheEntry answer = current;
                        for (final long start = System.currentTimeMillis(); System.currentTimeMillis() < start + coolDownTime;) {
                            current = searchCurrent();

                            if (current == null) {
                                if (answer != null) {
                                    valid = false;
                                    break;
                                }
                            } else if (!current.equals(answer)) {
                                valid = false;
                                break;
                            }
                            Thread.sleep(coolDownTime / 5);
                        }

                        if (valid) {
                            current = searchCurrent();
                            if (current == null) {
                                // やっぱりスレが無いなら 1 スレを立てる。
                                if (answer == null && addThread(null)) {
                                    Thread.sleep(interval);
                                }
                            } else if (current.isFull()) {
                                // やっぱり現行スレが埋まってるなら次スレを立てる。
                                if (current.equals(answer) && addThread(current)) {
                                    logState(current);
                                    Thread.sleep(interval);
                                }
                            }
                        }
                    } else if (current.thread == null) {
                        // 現行スレをまだ開いてない。
                        if (updateThread(current.name)) {
                            Thread.sleep(interval);
                        }
                    } else {
                        if (updateThread(current.name)) {
                            // 書き込む前に確認。
                            current = getCache().get(current.name);
                            if (current != null && !current.isFull()) {
                                final double flag = ThreadLocalRandom.current().nextDouble();
                                if (flag < writeRate) {
                                    // 書き込む。
                                    if (addComment(current.name)) {
                                        Thread.sleep(interval);
                                    }
                                } else if (flag < writeRate + (1 - writeRate) / 2) {
                                    // 書き込まずに周りを見て落ち着く。
                                    if (updateBoard()) {
                                        Thread.sleep(interval);
                                    }
                                } else {
                                    // 見るだけで止めとく。
                                    Thread.sleep(interval);
                                }
                            }
                        }
                    }

                }

                return null;
            }

            private void logState(final CacheEntry current) {
                final StringBuilder buff = (new StringBuilder(System.lineSeparator()))
                        .append("--------------------------------------------------").append(System.lineSeparator());
                for (final CacheEntry entry : getCache().values()) {
                    buff.append(entry).append(System.lineSeparator());
                }
                buff.append("--------------------------------------------------").append(System.lineSeparator());
                for (final CacheEntry entry : this.blacklist) {
                    buff.append(entry).append(System.lineSeparator());
                }
                buff.append("--------------------------------------------------").append(System.lineSeparator());
                buff.append(current).append(System.lineSeparator());
                buff.append("--------------------------------------------------");
                LOG.log(Level.FINEST, buff.toString());
            }
        };
    }

    /**
     * ROM 専のクライアントをつくる。
     * @param server 2ch サーバ
     * @param boardName 板名
     * @param interval 動作間隔
     * @param boardRate 板を読む割合
     * @return クライアント
     */
    static Callable<Void> newRom(final InetSocketAddress server, final String boardName, final long interval, final double boardRate) {
        return new TestClient(server, boardName) {

            private CacheEntry getRandom() {
                final List<CacheEntry> entries = new ArrayList<>(getCache().values());
                if (entries.isEmpty()) {
                    return null;
                } else {
                    return entries.get(ThreadLocalRandom.current().nextInt(entries.size()));
                }
            }

            @Override
            protected Void subCall() throws Exception {
                // 最初に板取得。
                updateBoard();

                while (!Thread.currentThread().isInterrupted()) {
                    if (ThreadLocalRandom.current().nextDouble() < boardRate) {
                        updateBoard();
                    } else {
                        final CacheEntry entry = getRandom();
                        if (entry != null) {
                            updateThread(entry.name);
                        }
                    }

                    Thread.sleep(interval);
                }

                return null;
            }
        };
    }

    /**
     * スレを乱立させるクライアントをつくる。
     * @param server 2ch サーバ
     * @param boardName 板名
     * @param author スレ立て時に名乗る名前
     * @return クライアント
     */
    static Callable<Void> newScatter(final InetSocketAddress server, final String boardName, final String author) {
        return new TestClient(server, boardName) {

            private CacheEntry getLatest() {
                CacheEntry current = null;
                int currentNumber = 0;
                for (final Map.Entry<Long, CacheEntry> entry : getCache().entrySet()) {
                    final CacheEntry cache = entry.getValue();
                    if (current == null) {
                        current = cache;
                        currentNumber = getHeadNumber(cache.title);
                    } else {
                        final int number = getHeadNumber(cache.title);
                        if (currentNumber < number) {
                            current = cache;
                            currentNumber = number;
                        }
                    }
                }
                return current;
            }

            private void addThread(final int number) throws MyRuleException, IOException {
                final String title = number + " スレ目";
                final String mail = "age";
                final String message = "1 レス目。" + getCache().size() + " スレ把握。";
                Client.addThread(this.server, this.boardName, title, author, mail, message);
            }

            @Override
            protected Void subCall() throws Exception {
                while (!Thread.currentThread().isInterrupted()) {
                    updateBoard();

                    final CacheEntry latest = getLatest();
                    if (latest == null) {
                        addThread(1);
                    } else {
                        addThread(getHeadNumber(latest.title));
                    }

                    Thread.sleep(1_001L);
                }

                return null;
            }
        };
    }

    public static void main(final String[] args) {
        final List<CacheEntry> entries = new ArrayList<>();
        entries.add(new CacheEntry(1385488408L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385488379L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385503660L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385482948L, "3 スレ目", 1000, null));
        entries.add(new CacheEntry(1385488401L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385488368L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385477873L, "2 スレ目", 1000, null));
        entries.add(new CacheEntry(1385503673L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385482960L, "3 スレ目", 1000, null));
        entries.add(new CacheEntry(1385488397L, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385482931L, "3 スレ目", 1000, null));
        entries.add(new CacheEntry(1385488365, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385503677, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385482965, "3 スレ目", 999, new Client.BbsThread() {
            @Override
            public String getTitle() {
                return "3 スレ目";
            }

            @Override
            public List<Client.BbsThread.Entry> getEntries() {
                final List<Client.BbsThread.Entry> list = new ArrayList<>(1001);
                for (int i = 0; i < 1001; i++) {
                    list.add(null);
                }
                return list;
            }

            @Override
            public String getName() {
                return Long.toString(1385482965L);
            }

            @Override
            public String getBoard() {
                return "test";
            }
        }));
        entries.add(new CacheEntry(1385488417, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385488389, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385503670, "4 スレ目", 1, null));
        entries.add(new CacheEntry(1385471969, "1 スレ目", 1000, null));

        Collections.sort(entries);

        for (final CacheEntry entry : entries) {
            System.out.println(entry);
        }

    }

}
