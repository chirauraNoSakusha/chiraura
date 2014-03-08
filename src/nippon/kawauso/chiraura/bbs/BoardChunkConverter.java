/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Storage;

/**
 * @author chirauraNoSakusha
 */
public final class BoardChunkConverter {

    private static final Logger LOG = Logger.getLogger(BoardChunkConverter.class.getName());

    // インスタンス化防止。
    private BoardChunkConverter() {}

    /**
     * SimpleBoardChunk を OrderingBoardChunk に変換する。
     * 非並列動作を想定。
     * @param storage 保管所
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 読み書き異常
     */
    public static void convert(final Storage storage) throws IOException, InterruptedException {
        storage.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
        storage.registerChunk(1L, ThreadChunk.class, ThreadChunk.getParser(), ThreadChunk.Id.class, ThreadChunk.Id.getParser());
        storage.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class, OrderingBoardChunk.Id.getParser());
        for (final Chunk.Id<?> id : storage.getIndices(Address.ZERO, Address.MAX).keySet()) {
            if (!(id instanceof SimpleBoardChunk.Id)) {
                continue;
            }
            final SimpleBoardChunk.Id oldId = (SimpleBoardChunk.Id) id;

            final SimpleBoardChunk oldBoard;
            try {
                oldBoard = storage.read(oldId);
            } catch (MyRuleException | IOException e) {
                LOG.log(Level.WARNING, oldId + " の読み込みに失敗しましたが、無視します。");
                continue;
            }
            if (oldBoard == null) {
                LOG.log(Level.WARNING, oldId + " がありませんでしたが、無視します。");
                continue;
            }

            final OrderingBoardChunk.Id newId = new OrderingBoardChunk.Id(oldId.getName());
            OrderingBoardChunk newBoard;
            try {
                newBoard = storage.read(newId);
            } catch (MyRuleException | IOException e) {
                LOG.log(Level.WARNING, newId + " の読み込みに失敗しましたが、無視します。");
                continue;
            }
            if (newBoard == null) {
                newBoard = new OrderingBoardChunk(oldId.getName());
            }

            for (final SimpleBoardChunk.Entry oldEntry : oldBoard.getEntries()) {
                newBoard.patch(new OrderingBoardChunk.Entry(oldEntry.getDate(), oldEntry.getDate(), oldEntry.getName(), oldEntry.getTitle(), oldEntry
                        .getNumOfComments()));
            }

            try {
                storage.write(newBoard);
            } catch (final IOException e) {
                LOG.log(Level.WARNING, newId + " の書き込みに失敗しましたが、無視します。");
                continue;
            }
        }
    }

    /**
     * SimpleBoardChunk を削除する。
     * 非並列動作を想定。
     * @param storage 保管所
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 読み書き異常
     */
    public static void remove(final Storage storage) throws IOException, InterruptedException {
        storage.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
        storage.registerChunk(1L, ThreadChunk.class, ThreadChunk.getParser(), ThreadChunk.Id.class, ThreadChunk.Id.getParser());
        storage.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class, OrderingBoardChunk.Id.getParser());
        for (final Chunk.Id<?> id : storage.getIndices(Address.ZERO, Address.MAX).keySet()) {
            if (!(id instanceof SimpleBoardChunk.Id)) {
                continue;
            }
            final SimpleBoardChunk.Id oldId = (SimpleBoardChunk.Id) id;

            try {
                if (storage.delete(oldId)) {
                    LOG.log(Level.WARNING, oldId + " の削除に失敗しましたが、無視します。");
                }
            } catch (final IOException e) {
                LOG.log(Level.WARNING, oldId + " の読み込みに失敗しましたが、無視します。");
                continue;
            }
        }
    }

}
