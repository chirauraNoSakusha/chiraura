/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class PeerCellTest extends BytesConvertibleTest<PeerCell> {

    @Override
    protected PeerCell[] getInstances() {
        final List<PeerCell> list = new ArrayList<>();
        list.add(new PeerCell(new InetSocketAddress("192.168.0.1", 13413)));
        list.add(new PeerCell(new InetSocketAddress("localhost", 63)));
        list.add(new PeerCell(new InetSocketAddress("2001:db8::1234:0:0:9abc", 7433)));
        return list.toArray(new PeerCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<PeerCell> getParser() {
        return PeerCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

    @Override
    protected PeerCell getInstance(final int seed) {
        final byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (seed + i);
        }
        try {
            return new PeerCell(new InetSocketAddress(InetAddress.getByAddress(bytes), seed % (1 << 16)));
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
