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
import nippon.kawauso.chiraura.lib.cell.Utf8Cell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 個体の入れ物。
 * 公開用。
 * バイト列変換時にできるだけホスト名を用いる。
 * @author chirauraNoSakusha
 */
public final class PublicPeerCell implements Cell<InetSocketAddress> {

    // 保持。
    private final InetSocketAddress peer;

    private Boolean hasHostname;

    /**
     * 作成する。
     * @param peer 個体
     */
    public PublicPeerCell(final InetSocketAddress peer) {
        if (peer == null) {
            throw new IllegalArgumentException("Null peer.");
        }
        this.peer = peer;
        this.hasHostname = null;
    }

    @Override
    public InetSocketAddress get() {
        return this.peer;
    }

    /*
     * ホスト名が分からないなら、先頭バイトを 0 にして IP アドレスをバイト列で続ける。
     * 分かるなら、先頭バイトを 0 以外にしてホスト名を続ける。
     */

    @Override
    public int byteSize() {
        if (this.hasHostname == null) {
            this.hasHostname = !this.peer.getHostName().equals(this.peer.getAddress().getHostAddress());
        }
        if (this.hasHostname) {
            return BytesConversion.byteSize("loo", 1L, new Utf8Cell(this.peer.getHostName()), new PortCell(this.peer.getPort()));
        } else {
            return BytesConversion.byteSize("labo", 0L, this.peer.getAddress().getAddress(), new PortCell(this.peer.getPort()));
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.hasHostname == null) {
            this.hasHostname = !this.peer.getHostName().equals(this.peer.getAddress().getHostAddress());
        }
        if (this.hasHostname) {
            return BytesConversion.toStream(output, "loo", 1L, new Utf8Cell(this.peer.getHostName()), new PortCell(this.peer.getPort()));
        } else {
            return BytesConversion.toStream(output, "labo", 0L, this.peer.getAddress().getAddress(), new PortCell(this.peer.getPort()));
        }
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<PublicPeerCell> getParser() {
        return new BytesConvertible.Parser<PublicPeerCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PublicPeerCell> output) throws MyRuleException,
                    IOException {
                final long[] type = new long[1];
                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                if (type[0] == 0) {
                    // バイト列の IP アドレス。
                    final byte[][] address = new byte[1][];
                    final List<PortCell> port = new ArrayList<>(1);
                    size += BytesConversion.fromStream(input, maxByteSize - size, "abo", address, port, PortCell.getParser());
                    try {
                        output.add(new PublicPeerCell(new InetSocketAddress(InetAddress.getByAddress(address[0]), port.get(0).get())));
                    } catch (final UnknownHostException e) {
                        throw new MyRuleException(e);
                    }
                } else {
                    // ホスト名。
                    final List<Utf8Cell> hostname = new ArrayList<>(1);
                    final List<PortCell> port = new ArrayList<>(1);
                    size += BytesConversion.fromStream(input, maxByteSize, "oo", hostname, Utf8Cell.getParser(), port, PortCell.getParser());
                    output.add(new PublicPeerCell(new InetSocketAddress(hostname.get(0).get(), port.get(0).get())));
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.peer.getHostString())
                .append(":").append(this.peer.getPort())
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PublicPeerCell)) {
            return false;
        }
        final PublicPeerCell other = (PublicPeerCell) obj;
        return this.peer.equals(other.peer);
    }

    @Override
    public int hashCode() {
        return this.peer.hashCode();
    }

}
