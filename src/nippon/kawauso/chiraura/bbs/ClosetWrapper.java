/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * 掲示板係用に取り繕った四次元押し入れ。
 * @author chirauraNoSakusha
 */
final class ClosetWrapper {

    private static final Logger LOG = Logger.getLogger(ClosetWrapper.class.getName());

    private static final String NOT_UPDATE_MAIL = "sage";

    private final Closet base;

    private final long updateThreshold;

    ClosetWrapper(final Closet base, final long updateThreshold) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        } else if (updateThreshold < 0) {
            throw new IllegalArgumentException("Negative threshold ( " + updateThreshold + " ).");
        }

        this.base = base;
        this.updateThreshold = updateThreshold;
        Register.init(this.base);
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
        final Chunk.Id<? extends BoardChunk> id = new OrderingBoardChunk.Id(boardName);
        final BoardChunk board = this.base.getChunk(id, timeout);
        if (board != null) {
            return board;
        }

        // 取得できなかったので新規作成。
        final BoardChunk newBoard = new OrderingBoardChunk(boardName);
        if (this.base.addChunk(newBoard, start + timeout - System.currentTimeMillis())) {
            return newBoard;
        } else {
            LOG.log(Level.WARNING, "板 ( {0} ) の作成に失敗しました。", new Object[] { boardName });
            return null;
        }
    }

    <T extends BoardChunk> void updateBoard(final ThreadChunk thread, final long order, final long timeout)
            throws InterruptedException {
        final long start = System.currentTimeMillis();
        final int numOfComments = thread.getNumOfComments() + (thread.isFull() ? 1 : 0);
        @SuppressWarnings("unchecked")
        final Chunk.Id<T> boardId = (Chunk.Id<T>) new OrderingBoardChunk.Id(thread.getBoard());
        @SuppressWarnings("unchecked")
        final BoardChunk.Entry<T> boardEntry = (BoardChunk.Entry<T>) new OrderingBoardChunk.Entry(System.currentTimeMillis(), order, thread.getName(),
                thread.getTitle(), numOfComments);
        final Closet.PatchResult<? extends BoardChunk> result1 = this.base.patchChunk(boardId, boardEntry, start + timeout - System.currentTimeMillis());
        if (result1.isGivenUp()) {
            LOG.log(Level.WARNING, "{0} による板 {1} の更新を諦めました。", new Object[] { boardEntry, boardId });
        } else if (result1.isNotFound()) {
            // 板が無いならつくる。
            final BoardChunk board = new OrderingBoardChunk(thread.getBoard());
            board.patch(boardEntry);
            final Closet.PatchOrAddResult<BoardChunk> result2 = this.base.patchOrAddChunk(board, start + timeout - System.currentTimeMillis());
            if (result2.isGivenUp()) {
                LOG.log(Level.WARNING, "{0} を含む板 {1} の作成を諦めました。", new Object[] { boardEntry, boardId });
            }
        } else if (!result1.isSuccess()) {
            LOG.log(Level.WARNING, "{0} による板 {1} の更新に失敗しました。", new Object[] { boardEntry, boardId });
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
            final Chunk.Id<? extends BoardChunk> boardId = new OrderingBoardChunk.Id(boardName);
            final BoardChunk board = this.base.getChunkImmediately(boardId);
            if (board != null && thread.getDate() <= board.getDate()) {
                final BoardChunk.Entry<?> entry = board.getEntry(thread.getName());
                if (entry != null && entry.getNumOfComments() < thread.getNumOfComments() + 1) {
                    // 埋まってるのに板に反映されていない。
                    updateBoard(thread, entry.getOrder(), start + timeout - System.currentTimeMillis());
                }
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
            threadName = before / Duration.SECOND;

            thread = new ThreadChunk(boardName, threadName, threadTitle, author, mail, date, authorId, message);
            if (this.base.addChunk(thread, start + timeout - before)) {
                // 成功。
                break;
            }

            final long after = System.currentTimeMillis();
            if (start + timeout <= after) {
                // 時間切れ。
                return false;
            } else if (after / Duration.SECOND != threadName) {
                // 次の試行が可能。
                continue;
            }
            Thread.sleep(Math.max(1L, Math.min(start + timeout - after, Duration.SECOND - after % Duration.SECOND)));
        }

        // 板を更新。
        updateBoard(thread, thread.getDate(), start + timeout - System.currentTimeMillis());

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

        /*
         * 板の更新。
         * 以下の場合だけ更新しない。
         * - sage、かつ、時間が経っていない。
         * - sage、かつ、板に登録してある書き込み数が分からない。
         * - sage、かつ、何らかの手違いにより、板に登録してある書き込み数が変わらない。
         */
        final ThreadChunk thread = result.getChunk();
        if (!mail.equals(NOT_UPDATE_MAIL)) {
            updateBoard(thread, thread.getDate(), start + timeout - System.currentTimeMillis());
        } else {
            final Chunk.Id<? extends BoardChunk> boardId = new OrderingBoardChunk.Id(boardName);
            final BoardChunk board = this.base.getChunkImmediately(boardId);
            if (board != null) {
                final BoardChunk.Entry<?> entry = board.getEntry(thread.getName());
                if (entry != null && entry.getDate() + this.updateThreshold < thread.getDate() && entry.getNumOfComments() < thread.getNumOfComments()) {
                    // 板に登録してある書き込み数は分かっているし、時間は経ってるし、書き込み数は増えてる。
                    updateBoard(thread, entry.getOrder(), start + timeout - System.currentTimeMillis());
                }
            }
        }

        return true;
    }

}
