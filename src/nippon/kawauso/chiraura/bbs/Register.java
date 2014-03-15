package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.closet.Closet;

/**
 * @author chirauraNoSakusha
 */
final class Register {

    // インスタンス化防止。
    private Register() {}

    static void init(final Closet base) {
        base.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser(), SimpleBoardChunk.Entry.class,
                SimpleBoardChunk.Entry.getParser());
        base.registerChunk(1L, ThreadChunk.class, ThreadChunk.getParser(), ThreadChunk.Id.class, ThreadChunk.Id.getParser(), ThreadChunk.Entry.class,
                ThreadChunk.Entry.getParser());
        base.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class, OrderingBoardChunk.Id.getParser(),
                OrderingBoardChunk.Entry.class, OrderingBoardChunk.Entry.getParser());
    }

}
