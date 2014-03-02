/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.Chunk;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BasicBbsTest {

    private static final InetSocketAddress server = new InetSocketAddress("localhost", 11111);
    private static final long clientTimeout = 30 * Duration.SECOND;
    private static final long workTimeout = 10 * Duration.SECOND;
    private static final long updateThreshold = 5 * Duration.MINUTE;
    private static final Menu menu = new Menu();

    private static final long trafficDuration = Duration.SECOND;
    private static final int trafficCountLimit = 10;

    private final Closet closet;
    private final ExecutorService executor;

    private final long start;
    private final String boardName;
    private final Set<ThreadChunk> initialThreads;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public BasicBbsTest() throws Exception {
        this.closet = new Closet() {
            private final Map<Chunk.Id<?>, Chunk> chunks = new HashMap<>();

            @Override
            public <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(final long type, final Class<C> chunkClass,
                    final BytesConvertible.Parser<? extends C> chunkParser,
                    final Class<I> idClass,
                    final BytesConvertible.Parser<? extends I> idParser) {
                return;
            }

            @Override
            public <C extends Mountain, I extends Chunk.Id<C>, D extends Mountain.Dust<C>> void registerChunk(final long type, final Class<C> chunkClass,
                    final BytesConvertible.Parser<? extends C> chunkParser,
                    final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser, final Class<D> diffClass,
                    final BytesConvertible.Parser<? extends D> diffParser) {
                return;
            }

            @SuppressWarnings("unchecked")
            @Override
            public synchronized <T extends Chunk> T getChunk(final Chunk.Id<T> id, final long timeout) {
                return (T) this.chunks.get(id);
            }

            @SuppressWarnings("unchecked")
            @Override
            public synchronized <T extends Chunk> T getChunkImmediately(final Chunk.Id<T> id) {
                return (T) this.chunks.get(id);
            }

            @Override
            public synchronized boolean addChunk(final Chunk chunk, final long timeout) {
                if (this.chunks.containsKey(chunk.getId())) {
                    return false;
                } else {
                    this.chunks.put(chunk.getId(), chunk);
                    return true;
                }
            }

            @Override
            public synchronized <T extends Mountain> Closet.PatchResult<T> patchChunk(final Chunk.Id<T> id, final Mountain.Dust<T> diff, final long timeout) {
                @SuppressWarnings("unchecked")
                final T chunk = (T) this.chunks.get(id);
                if (chunk == null) {
                    return new Closet.PatchResult<T>() {
                        @Override
                        public boolean isGivenUp() {
                            return false;
                        }

                        @Override
                        public boolean isNotFound() {
                            return true;
                        }

                        @Override
                        public boolean isSuccess() {
                            return false;
                        }

                        @Override
                        public T getChunk() {
                            return null;
                        }
                    };
                } else {
                    final boolean success = chunk.patch(diff);
                    return new Closet.PatchResult<T>() {
                        @Override
                        public boolean isGivenUp() {
                            return false;
                        }

                        @Override
                        public boolean isNotFound() {
                            return false;
                        }

                        @Override
                        public boolean isSuccess() {
                            return success;
                        }

                        @Override
                        public T getChunk() {
                            return chunk;
                        }
                    };
                }
            }

            @Override
            public synchronized <T extends Mountain> Closet.PatchOrAddResult<T> patchOrAddChunk(final T chunk, final long timeout) {
                @SuppressWarnings("unchecked")
                final T chunk0 = (T) this.chunks.get(chunk.getId());
                if (chunk0 == null) {
                    this.chunks.put(chunk.getId(), chunk);
                    return new Closet.PatchOrAddResult<T>() {
                        @Override
                        public boolean isGivenUp() {
                            return false;
                        }

                        @Override
                        public T getChunk() {
                            return chunk;
                        }
                    };
                } else {
                    if (chunk0.baseEquals(chunk)) {
                        for (final Mountain.Dust<?> diff : chunk.getDiffsAfter(Long.MIN_VALUE)) {
                            chunk0.patch(diff);
                        }
                    }
                    return new Closet.PatchOrAddResult<T>() {
                        @Override
                        public boolean isGivenUp() {
                            return false;
                        }

                        @Override
                        public T getChunk() {
                            return chunk0;
                        }
                    };
                }
            }

            @Override
            public ClosetReport takeError() throws InterruptedException {
                throw new UnsupportedOperationException("Not implemented.");
            }

            @Override
            public void start(final ExecutorService executor1) {}

            @Override
            public void close() {}
        };
        this.executor = Executors.newCachedThreadPool();

        this.start = System.currentTimeMillis() - 100 * Duration.SECOND;
        this.boardName = "test";
        this.initialThreads = new HashSet<>();
        {
            final long threadName = this.start / Duration.SECOND;
            final String threadTitle = "くそスレ";
            final ThreadChunk thread = new ThreadChunk(this.boardName, threadName, threadTitle, "創造神", "age", this.start, 0, "崇めよ");
            this.initialThreads.add(thread);
            final BoardChunk board = new OrderingBoardChunk(this.boardName);
            board.patch(new OrderingBoardChunk.Entry(this.start, this.start, threadName, threadTitle, 1));
            this.closet.addChunk(board, 100L);
            this.closet.addChunk(thread, 100L);
        }

        // TestFunctions.testLogging(Level.ALL, Level.OFF);
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * 板の取得検査。
     * @throws Exception 異常
     */
    @Test
    public void testGetExistBoard() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        final Client.BbsBoard board = Client.getBoard(server, this.boardName);
        Assert.assertNotNull(board);
        Assert.assertEquals(this.initialThreads.size(), board.getEntries().size());

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * 新しい板の取得検査。
     * @throws Exception 異常
     */
    @Test
    public void testGetNewBoard() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        final Client.BbsBoard board = Client.getBoard(server, this.boardName + "1");
        Assert.assertNotNull(board);
        Assert.assertEquals(0, board.getEntries().size());

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * スレの取得検査。
     * @throws Exception 異常
     */
    @Test
    public void testGetExistTread() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        for (final ThreadChunk thread : this.initialThreads) {
            final Client.BbsThread result = Client.getThread(server, this.boardName, Long.toString(thread.getName()));
            Assert.assertEquals(thread.getTitle(), result.getTitle());
            Assert.assertEquals(thread.getNumOfComments(), result.getEntries().size());
        }

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * 存在しないスレの取得検査。
     * @throws Exception 異常
     */
    @Test
    public void testGetNotExistTread() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        final Client.BbsThread result = Client.getThread(server, this.boardName, "0");
        Assert.assertNull(result);

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * スレの作成検査。
     * @throws Exception 異常
     */
    @Test
    public void testAddThread() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        final String newThreadTitle = "てすと";
        Assert.assertTrue(Client.addThread(server, this.boardName, newThreadTitle, "名無し", "sage", "てすつ"));

        final Client.BbsBoard board = Client.getBoard(server, this.boardName);
        Assert.assertNotNull(board);
        final List<Client.BbsBoard.Entry> boardEntries = board.getEntries();
        final Set<String> titles = new HashSet<>();
        for (final Client.BbsBoard.Entry entry : boardEntries) {
            titles.add(entry.getTitle());
        }
        Assert.assertTrue(titles.contains(newThreadTitle));

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * 書き込み検査。
     * @throws Exception 異常
     */
    @Test
    public void testAddComment() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        final ThreadChunk info = (new ArrayList<>(this.initialThreads)).get(0);
        final String author = "俺様";
        final String mail = "sage";
        final String message = "くそスレ乙";
        Assert.assertTrue(Client.addComment(server, info.getBoard(), Long.toString(info.getName()), author, mail, message));

        final Client.BbsThread thread = Client.getThread(server, info.getBoard(), Long.toString(info.getName()));
        Assert.assertNotNull(thread);
        final List<Client.BbsThread.Entry> entries = thread.getEntries();
        Assert.assertEquals(author, entries.get(entries.size() - 1).getAuthor());
        Assert.assertEquals(mail, entries.get(entries.size() - 1).getMail());
        Assert.assertEquals(message, entries.get(entries.size() - 1).getMessage());

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * 実際に 2ch ブラウザから検査。
     * @throws Exception 異常
     */
    // @Test
    public void testCommunication() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);
        instance.start(this.executor);
        Thread.sleep(10 * Duration.MINUTE);

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    /**
     * 通信制限の発動検査。
     * @throws Exception 異常
     */
    @Test
    public void testConnectionLimit() throws Exception {
        final BasicBbs instance = new BasicBbs(server.getPort(), clientTimeout, workTimeout, this.closet, updateThreshold, menu, trafficDuration,
                trafficCountLimit);

        instance.start(this.executor);
        Thread.sleep(100L);

        final ThreadChunk thread = (new ArrayList<>(this.initialThreads)).get(0);

        final long start0 = System.currentTimeMillis();
        for (int i = 0; i <= trafficCountLimit; i++) {
            final Client.BbsThread result = Client.getThread(server, this.boardName, Long.toString(thread.getName()));
            Assert.assertEquals(thread.getTitle(), result.getTitle());
            Assert.assertEquals(thread.getNumOfComments(), result.getEntries().size());
        }
        final long end = System.currentTimeMillis();

        if (end - start0 <= trafficDuration) {
            Assert.fail("" + (end - start0));
        }

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

}
