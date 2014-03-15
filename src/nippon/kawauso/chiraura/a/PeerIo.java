package nippon.kawauso.chiraura.a;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.Mosaic;
import nippon.kawauso.chiraura.lib.connection.PeerCell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * @author chirauraNoSakusha
 */
final class PeerIo {

    // インスタンス化防止。
    private PeerIo() {}

    private static final Logger LOG = Logger.getLogger(PeerIo.class.getName());

    private static <T extends BytesConvertible> void listToFile(final List<T> list, final File output) throws IOException {
        if (!output.exists()) {
            if (output.createNewFile()) {
                LOG.log(Level.INFO, "{0} を作成しました。", output.getPath());
            }
        }
        try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(output))) {
            BytesConversion.toStream(stream, "ao", list);
            stream.flush();
        }
    }

    static void peersToFile(final List<InetSocketAddress> peers, final File output) throws IOException {
        final List<PeerCell> list = new ArrayList<>();
        for (final InetSocketAddress peer : peers) {
            list.add(new PeerCell(peer));
        }
        listToFile(list, output);
    }

    private static <T extends BytesConvertible> List<T> listFromFile(final File input, final BytesConvertible.Parser<T> parser) {
        if (!input.exists()) {
            return new ArrayList<>(0);
        }
        final List<T> list = new ArrayList<>();
        try (InputStream stream = new BufferedInputStream(new FileInputStream(input))) {
            BytesConversion.fromStream(stream, (int) input.length(), "ao", list, parser);
        } catch (final IOException | MyRuleException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            LOG.log(Level.WARNING, "異常が発生しました", e);
            if (!input.renameTo(backup)) {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存しました。", new Object[] { input.getPath(), backup.getPath() });
            } else {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存することもできませんでした。", new Object[] { input.getPath(), backup.getPath() });
            }
        }
        return list;
    }

    static List<InetSocketAddress> peersFromFile(final File input) {
        final List<PeerCell> peers = listFromFile(input, PeerCell.getParser());
        final List<InetSocketAddress> result = new ArrayList<>(peers.size());
        for (final PeerCell peer : peers) {
            result.add(peer.get());
        }
        return result;
    }

    static void addressedPeersToFile(final List<AddressedPeer> peers, final File output) throws IOException {
        listToFile(peers, output);
    }

    static List<AddressedPeer> addressedPeersFromFile(final File input) {
        return listFromFile(input, AddressedPeer.getParser());
    }

    private static InetSocketAddress peerFromText(final String line) throws MyRuleException {
        final String str = line.trim();
        if (str.isEmpty() || str.startsWith("#")) {
            // 空行、コメント行は無視。
            return null;
        }

        if (str.charAt(0) == '^') {
            return Mosaic.peerFrom(str.substring(1));
        } else {
            final String[] terms = str.split("\\s+");
            return new InetSocketAddress(terms[0], Integer.parseInt(terms[1]));
        }
    }

    static List<InetSocketAddress> peersFromTextFile(final File input) {
        final List<InetSocketAddress> list = new ArrayList<>();

        if (!input.exists()) {
            return list;
        }

        try (BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(input), Global.INTERNAL_CHARSET))) {
            for (String line; (line = buff.readLine()) != null;) {
                try {
                    final InetSocketAddress peer = peerFromText(line);
                    if (peer != null) {
                        list.add(peer);
                    }
                } catch (final RuntimeException | MyRuleException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", e);
                    LOG.log(Level.INFO, "\"{0}\" からの個体情報の復元に失敗しました", line);
                }
            }
        } catch (final IOException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            LOG.log(Level.WARNING, "異常が発生しました", e);
            if (!input.renameTo(backup)) {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存しました。", new Object[] { input.getPath(), backup.getPath() });
            } else {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存することもできませんでした。", new Object[] { input.getPath(), backup.getPath() });
            }
        }
        return list;
    }
}
