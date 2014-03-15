package nippon.kawauso.chiraura.bbs;

/**
 * @author chirauraNoSakusha
 */
class BoardResponse extends ContentResponse {

    BoardResponse(final BoardChunk board) {
        super(board, board.toNetworkString().getBytes(Constants.CONTENT_CHARSET));
    }

}
