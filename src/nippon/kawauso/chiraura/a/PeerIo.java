/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

    static <T extends BytesConvertible> List<T> listFromFile(final File input, final BytesConvertible.Parser<T> parser) {
        if (!input.exists()) {
            return new ArrayList<>(0);
        }
        final List<T> list = new ArrayList<>();
        try (InputStream stream = new BufferedInputStream(new FileInputStream(input))) {
            BytesConversion.fromStream(stream, (int) input.length(), "ao", list, parser);
        } catch (final IOException | MyRuleException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            if (!input.renameTo(backup)) {
                LOG.log(Level.WARNING, "壊れた " + input.getPath() + " を " + backup.getPath() + " として保存しました", e);
            } else {
                LOG.log(Level.WARNING, "壊れた " + input.getPath() + " を " + backup.getPath() + " として保存できませんでした", e);
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

    static InetSocketAddress peerFromText(final String line) throws MyRuleException {
        if (line.charAt(0) == '^') {
            return Mosaic.peerFrom(line.substring(1));
        } else {
            final String[] terms = line.split("\\s+");
            return new InetSocketAddress(terms[0], Integer.parseInt(terms[1]));
        }
    }

    static List<InetSocketAddress> peersFromTextFile(final File input) {
        if (!input.exists()) {
            return new ArrayList<>(0);
        }
        final List<InetSocketAddress> list = new ArrayList<>();
        try (BufferedReader buff = new BufferedReader(new FileReader(input))) {
            final Pattern empty = Pattern.compile("^\\s*$");
            for (String line; (line = buff.readLine()) != null;) {
                if (!empty.matcher(line).matches()) {
                    try {
                        list.add(peerFromText(line));
                    } catch (final RuntimeException | MyRuleException e) {
                        LOG.log(Level.WARNING, "\"" + line + "\" からの個体情報の復元に失敗しました", e);
                    }
                }
            }
        } catch (final IOException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            if (!input.renameTo(backup)) {
                LOG.log(Level.WARNING, "壊れた " + input.getPath() + " を " + backup.getPath() + " として保存しました", e);
            } else {
                LOG.log(Level.WARNING, "壊れた " + input.getPath() + " を " + backup.getPath() + " として保存できませんでした", e);
            }
        }
        return list;
    }
}
