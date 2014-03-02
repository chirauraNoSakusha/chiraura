/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.cell.Cell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 個体の入れ物。
 * バイト列変換時に IP アドレスのみを用いる。
 * @author chirauraNoSakusha
 */
public final class PeerCell implements Cell<InetSocketAddress> {

    // 保持。
    private final InetSocketAddress peer;

    /**
     * 作成する。
     * @param peer 個体
     */
    public PeerCell(final InetSocketAddress peer) {
        this.peer = peer;
    }

    @Override
    public InetSocketAddress get() {
        return this.peer;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("abo", this.peer.getAddress().getAddress(), new PortCell(this.peer.getPort()));
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "abo", this.peer.getAddress().getAddress(), new PortCell(this.peer.getPort()));
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<PeerCell> getParser() {
        return new BytesConvertible.Parser<PeerCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PeerCell> output) throws MyRuleException, IOException {
                final byte[][] address = new byte[1][];
                final List<PortCell> port = new ArrayList<>(1);
                final int size = BytesConversion.fromStream(input, maxByteSize, "abo", address, port, PortCell.getParser());
                try {
                    output.add(new PeerCell(new InetSocketAddress(InetAddress.getByAddress(address[0]), port.get(0).get())));
                } catch (final UnknownHostException e) {
                    throw new MyRuleException(e);
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.peer.getAddress().getHostAddress())
                .append(':').append(this.peer.getPort())
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PeerCell)) {
            return false;
        }
        final PeerCell other = (PeerCell) obj;
        return this.peer.equals(other.peer);
    }

    @Override
    public int hashCode() {
        return this.peer.hashCode();
    }

}
