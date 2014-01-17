/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * 掲示板係用に取り繕った四次元押し入れ。
 * @author chirauraNoSakusha
 */
final class ClosetWrapper {

    private static final Logger LOG = Logger.getLogger(ClosetWrapper.class.getName());

    private static final String NOT_UPDATE_MAIL = "sage";

    private final Closet base;

    /*
     * 以下の ThreadKey, ThreadInfo とそれを利用する numOfCommentsCache は
     * スレが埋まった後に、それを板に反映するためだけにある。
     */
    private static final class ThreadKey {
        private final String board;
        private final long name;

        private ThreadKey(final String board, final long name) {
            if (board == null) {
                throw new IllegalArgumentException("Null name.");
            }
            this.board = board;
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.board.hashCode();
            result = prime * result + (int) (this.name ^ (this.name >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof ThreadKey)) {
                return false;
            }
            final ThreadKey other = (ThreadKey) obj;
            return this.board.equals(other.board) && this.name == other.name;
        }
    }

    private static final class ThreadInfo {
        private final int numOfComments;
        private final long date;

        private ThreadInfo(final int numOfComments, final long date) {
            this.numOfComments = numOfComments;
            this.date = date;
        }
    }

    private static final int CACHE_CAPACITY = 10_000;
    private final Map<ThreadKey, ThreadInfo> numOfCommentsCache;

    ClosetWrapper(final Closet base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }

        this.base = base;
        Register.init(this.base);

        @SuppressWarnings("serial")
        final Map<ThreadKey, ThreadInfo> map = new LinkedHashMap<ThreadKey, ThreadInfo>() {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<ThreadKey, ThreadInfo> eldest) {
                return this.size() > CACHE_CAPACITY;
            }
        };
        this.numOfCommentsCache = Collections.synchronizedMap(map);
    }

    /**
     * 板を得る。
     * @param boardName 板名
     * @param timeout 制限時間
     * @return 板。
     *         時間切れの場合は null
     * @throws InterruptedException 割り込まれた場合
     */
    BoardChunk getBoard(final String boardName, final long timeout) throws InterruptedException {
        /*
         * 板があればそれを返し、
         * 無ければ新しく作る。
         */
        final long start = System.currentTimeMillis();

        // 取得。
        final Chunk.Id<BoardChunk> id = new BoardChunk.Id(boardName);
        final BoardChunk board = this.base.getChunk(id, timeout);
        if (board != null) {
            final long date = System.currentTimeMillis();
            for (final BoardChunk.Entry entry : board.getEntries()) {
                this.numOfCommentsCache.put(new ThreadKey(boardName, entry.getName()), new ThreadInfo(entry.getNumOfComments(), date));
            }
            return board;
        }

        // 取得できなかったので新規作成。
        final BoardChunk newBoard = new BoardChunk(boardName);
        if (this.base.addChunk(newBoard, start + timeout - System.currentTimeMillis())) {
            return newBoard;
        } else {
            LOG.log(Level.WARNING, "板 ( {0} ) の作成に失敗しました。", new Object[] { boardName });
            return null;
        }
    }

    // void updateBoard(final String boardName, final long threadName, final String title, final int numOfComments, final long date, final long timeout)
    // throws InterruptedException {
    void updateBoard(final ThreadChunk thread, final long timeout)
            throws InterruptedException {
        final long start = System.currentTimeMillis();
        final Chunk.Id<BoardChunk> boardId = new BoardChunk.Id(thread.getBoard());
        final int numOfComments = (thread.isFull() ? thread.getNumOfComments() + 1 : thread.getNumOfComments());
        final Mountain.Dust<BoardChunk> boardEntry = new BoardChunk.Entry(thread.getDate(), thread.getName(), thread.getTitle(), numOfComments);
        final Closet.PatchResult<BoardChunk> result1 = this.base.patchChunk(boardId, boardEntry, start + timeout - System.currentTimeMillis());
        if (result1.isGivenUp()) {
            LOG.log(Level.WARNING, "{0} による板 {1} の更新を諦めました。", new Object[] { boardEntry, boardId });
        } else if (result1.isNotFound()) {
            // 板が無いならつくる。
            final BoardChunk board = new BoardChunk(thread.getBoard());
            board.patch(boardEntry);
            final Closet.PatchOrAddResult<BoardChunk> result2 = this.base.patchOrAddChunk(board, start + timeout - System.currentTimeMillis());
            if (result2.isGivenUp()) {
                LOG.log(Level.WARNING, "{0} による板 {1} の作成を諦めました。", new Object[] { boardEntry, boardId });
            } else {
                // this.numOfCommentsCache.put(new ThreadKey(thread.getBoard(), thread.getName()), new ThreadInfo(numOfComments, System.currentTimeMillis()));
                final long date = System.currentTimeMillis();
                for (final BoardChunk.Entry entry : result2.getChunk().getEntries()) {
                    this.numOfCommentsCache.put(new ThreadKey(thread.getBoard(), entry.getName()), new ThreadInfo(entry.getNumOfComments(), date));
                }
            }
        } else {
            if (!result1.isSuccess()) {
                LOG.log(Level.WARNING, "{0} による板 {1} の更新に失敗しました。", new Object[] { boardEntry, boardId });
            }
            // this.numOfCommentsCache.put(new ThreadKey(thread.getBoard(), thread.getName()), new ThreadInfo(numOfComments, System.currentTimeMillis()));
            final long date = System.currentTimeMillis();
            for (final BoardChunk.Entry entry : result1.getChunk().getEntries()) {
                this.numOfCommentsCache.put(new ThreadKey(thread.getBoard(), entry.getName()), new ThreadInfo(entry.getNumOfComments(), date));
            }
        }
    }

    /**
     * スレを得る。
     * @param boardName 板名
     * @param threadName スレ名
     * @param timeout 制限時間
     * @return スレ。
     *         無い場合、時間切れの場合は null
     * @throws InterruptedException 割り込まれた場合
     */
    ThreadChunk getThread(final String boardName, final long threadName, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        final Chunk.Id<ThreadChunk> id = new ThreadChunk.Id(boardName, threadName);
        final ThreadChunk thread = this.base.getChunk(id, timeout);

        if (thread == null) {
            return null;
        }

        if (thread.isFull()) {
            final ThreadInfo info = this.numOfCommentsCache.get(new ThreadKey(boardName, threadName));
            if (info != null && info.numOfComments < thread.getNumOfComments() + 1 && thread.getDate() < info.date) {
                // 埋まってるのに板に反映されていない。
                // updateBoard(boardName, threadName, thread.getTitle(), thread.getNumOfComments(), thread.getDate(), start + timeout -
                // System.currentTimeMillis());
                updateBoard(thread, start + timeout - System.currentTimeMillis());
            }
        }

        return thread;
    }

    /**
     * スレを作成する。
     * @param boardName 板名
     * @param threadTitle スレの題名
     * @param author 作成者
     * @param mail 作成者のメールアドレス
     * @param date 作成日時 (ミリ秒)
     * @param authorId 作成者の書き込みID
     * @param message 最初の書き込み内容
     * @param timeout 制限時間
     * @return 成功した場合のみ true。
     * @throws InterruptedException 割り込まれた場合
     */
    boolean addThread(final String boardName, final String threadTitle, final String author, final String mail, final long date, final long authorId,
            final String message, final long timeout) throws InterruptedException {
        /*
         * スレ名を取得し、
         * スレを新規作成し、
         * 板を更新する。
         */
        final long start = System.currentTimeMillis();

        ThreadChunk thread;
        long threadName;
        while (true) {
            final long before = System.currentTimeMillis();
            threadName = before / 1_000L;

            thread = new ThreadChunk(boardName, threadName, threadTitle, author, mail, date, authorId, message);
            if (this.base.addChunk(thread, start + timeout - before)) {
                // 成功。
                break;
            }

            final long after = System.currentTimeMillis();
            if (start + timeout <= after) {
                // 時間切れ。
                return false;
            } else if (after / 1_000L != threadName) {
                // 次の試行が可能。
                continue;
            }
            Thread.sleep(Math.max(1L, Math.min(start + timeout - after, 1_000L - after % 1_000L)));
        }

        // 板を更新。
        // updateBoard(boardName, threadName, threadTitle, 1, thread.getDate(), start + timeout - System.currentTimeMillis());
        updateBoard(thread, start + timeout - System.currentTimeMillis());

        return true;
    }

    /**
     * 書き込む。
     * @param boardName 板名
     * @param threadName スレ名
     * @param author 書き込み主
     * @param mail メールアドレス
     * @param date 書き込み日時 (ミリ秒)
     * @param authorId 書き込み ID
     * @param message 本文
     * @param timeout 制限時間
     * @return 成功した場合のみ true
     * @throws InterruptedException 割り込まれた場合
     */
    boolean addComment(final String boardName, final long threadName, final String author, final String mail, final long date, final long authorId,
            final String message, final long timeout) throws InterruptedException {
        /*
         * 書き込み、
         * 必要なら板を更新する。
         */
        final long start = System.currentTimeMillis();

        // スレの更新。
        final Chunk.Id<ThreadChunk> id = new ThreadChunk.Id(boardName, threadName);
        final Mountain.Dust<ThreadChunk> threadEntry = ThreadChunk.Entry.newInstance(author, mail, date, authorId, message);
        final Closet.PatchResult<ThreadChunk> result = this.base.patchChunk(id, threadEntry, timeout);
        if (result.isGivenUp() || result.isNotFound() || !result.isSuccess()) {
            return false;
        }

        // 板の更新。
        if (!mail.equals(NOT_UPDATE_MAIL)) {
            final ThreadChunk thread = result.getChunk();
            // updateBoard(boardName, threadName, thread.getTitle(), thread.getNumOfComments(), thread.getDate(), start + timeout - System.currentTimeMillis());
            updateBoard(thread, start + timeout - System.currentTimeMillis());
        }
        return true;
    }

}
