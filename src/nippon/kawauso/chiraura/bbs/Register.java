/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.closet.Closet;

/**
 * @author chirauraNoSakusha
 */
final class Register {

    // インスタンス化防止。
    private Register() {}

    static void init(final Closet base) {
        base.registerChunk(0L, BoardChunk.class, BoardChunk.getParser(), BoardChunk.Id.class, BoardChunk.Id.getParser(), BoardChunk.Entry.class,
                BoardChunk.Entry.getParser());
        base.registerChunk(1L, ThreadChunk.class, ThreadChunk.getParser(), ThreadChunk.Id.class, ThreadChunk.Id.getParser(), ThreadChunk.Entry.class,
                ThreadChunk.Entry.getParser());
    }

}
