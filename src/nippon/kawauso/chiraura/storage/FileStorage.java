package nippon.kawauso.chiraura.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.Base64;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.Hexadecimal;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 操作毎にファイルを読み書きする倉庫。
 * @author chirauraNoSakusha
 */
final class FileStorage implements Storage {

    private static final Logger LOG = Logger.getLogger(FileStorage.class.getName());

    private static final int INDEX_READ_BUFFER_SIZE = 512;

    /*
     * データ片は、その論理位置を BASE64 エンコードした名前のファイルに保存する。
     */

    /*
     * データ片に対するロックをファイルロックとしても使う。
     * id が指すデータ片に対応するファイルを読み書きする前後で lock(id) と unlock(id) を呼ぶ。
     * ファイルの存在検査等、ファイル本体ではなくディレクトリの読み書きでは呼ばない。
     */

    // 参照。
    private final File root;
    private final int fileSizeLimit;
    private final int directoryBitSize;

    // 保持。
    private final LockPool<String> locks; // ファイル名でロックする。
    private final TypeRegistry<Chunk> chunkRegistry;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private static final String TRASH = "%%trash%%"; // 有効ディレクトリと混同する可能性が無いように Base64 に使わない文字を含むこと。
    private final File trash;

    FileStorage(final File root, final int fileSizeLimit, final int directoryBitSize) {
        final int maxDirectoryBitSize = (Address.SIZE / 6) * 6;// ディレクトリ名が論理位置以外の影響を受けない長さ。
        if (root == null) {
            throw new IllegalArgumentException("Null root.");
        } else if (fileSizeLimit < 0) {
            throw new IllegalArgumentException("Invalid file size limit ( " + fileSizeLimit + " ).");
        } else if (directoryBitSize <= 0 || maxDirectoryBitSize < directoryBitSize) {
            throw new IllegalArgumentException("Invalid directory bit size ( " + directoryBitSize + " ) not in [ 1, " + maxDirectoryBitSize + " ].");
        }

        this.root = root;
        loadDirectory(this.root);
        this.directoryBitSize = directoryBitSize;
        this.fileSizeLimit = fileSizeLimit;

        this.locks = new LockPool<>();
        this.chunkRegistry = TypeRegistries.newRegistry();
        this.idRegistry = TypeRegistries.newRegistry();

        this.trash = new File(root, TRASH);
        loadDirectory(this.trash);
    }

    private static void loadDirectory(final File directory) {
        // ディレクトリの存在。
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalStateException("Cannot make directory ( " + directory.getPath() + " ).");
            }
            LOG.log(Level.INFO, "{0} を作成しました。", directory.getPath());
        } else if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not directory ( " + directory.getPath() + " ).");
        }
        // 読み書き属性。
        if (!directory.canRead() && !directory.setReadable(true)) {
            throw new IllegalStateException("Not readable directory ( " + directory.getPath() + " ).");
        } else if (!directory.canWrite() && !directory.setWritable(true)) {
            throw new IllegalStateException("Not writable directory ( " + directory.getPath() + " ).");
        }
    }

    @Override
    public <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser) {
        this.chunkRegistry.register(type, chunkClass, chunkParser);
        this.idRegistry.register(type, idClass, idParser);
    }

    @Override
    public TypeRegistry<Chunk> getChunkRegistry() {
        return TypeRegistries.unregisterableRegistry(this.chunkRegistry);
    }

    @Override
    public TypeRegistry<Chunk.Id<?>> getIdRegistry() {
        return TypeRegistries.unregisterableRegistry(this.idRegistry);
    }

    /**
     * バイト列の前方から任意のビット数分を切り取り、返す。
     * @param bytes 元のバイト列
     * @param nBits 切り取るビット数
     * @return bytes の前方から nBits 分のみを含む最短のバイト列
     */
    private static byte[] getFront(final byte[] bytes, final int nBits) {
        final byte[] buff = new byte[(nBits + Byte.SIZE - 1) / Byte.SIZE];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = bytes[i];
        }
        final int remainder = nBits % Byte.SIZE;
        if (remainder != 0) {
            final int mask = ((1 << Byte.SIZE) - 1) << (Byte.SIZE - remainder);
            buff[buff.length - 1] &= mask;
        }
        return buff;
    }

    /**
     * バイト列の前方から任意のビット数分を切り取り、残った後方部分を返す。
     * @param bytes 元のバイト列
     * @param nBits 切り取るビット数
     * @return bytes の前方から nBits 分を除いた最短のバイト列
     */
    private static byte[] getBack(final byte[] bytes, final int nBits) {
        final byte[] buff = new byte[bytes.length - nBits / Byte.SIZE];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = bytes[bytes.length - buff.length + i];
        }
        final int remainder = nBits % Byte.SIZE;
        if (remainder != 0) {
            final int mask = ((1 << Byte.SIZE) - 1) >> remainder;
            buff[0] &= mask;
        }
        return buff;
    }

    /*
     * ファイル名に使う記号。
     * '/' は Unix のディレクトリ区切りなので '+' に。
     * '-' はファイル名の先頭だとコマンドラインオプションと混同するので '_' に。
     */
    private static final char BASE64_63 = '+';
    private static final char BASE64_64 = '_';

    /**
     * バイト列をファイル名にする。
     * @param bytes バイト列
     * @return ファイル名
     */
    private static String toFileString(final byte[] bytes) {
        return Base64.toBase64(bytes, BASE64_63, BASE64_64);
    }

    /**
     * ファイル名をバイト列にする。
     * @param string ファイル名
     * @return バイト列
     * @throws MyRuleException おかしなファイル名だった場合
     */
    private static byte[] fromFileString(final String string) throws MyRuleException {
        return Base64.fromBase64(string, BASE64_63, BASE64_64);
    }

    static String getBase(final int directoryBitSize, final Chunk.Id<?> id, final long type) {
        final byte[] buff = BytesConversion.toBytes("ol", id.getAddress(), type);
        final String dir = toFileString(getFront(buff, directoryBitSize));
        final String file = toFileString(getBack(buff, directoryBitSize));
        return (new StringBuilder(dir)).append(File.separator).append(file).toString();
    }

    /**
     * データ片が保存されるファイルを得る。
     * @param id データ片の識別子
     * @return データ片が保存されるファイル
     */
    private File getFile(final Chunk.Id<?> id) {
        return new File(this.root, getBase(this.directoryBitSize, id, this.idRegistry.getId(id)));
    }

    @Override
    public void lock(final Chunk.Id<?> id) throws InterruptedException {
        this.locks.lock(getBase(this.directoryBitSize, id, this.idRegistry.getId(id)));
    }

    @Override
    public boolean tryLock(final Chunk.Id<?> id) {
        return this.locks.tryLock(getBase(this.directoryBitSize, id, this.idRegistry.getId(id)));
    }

    @Override
    public void unlock(final Chunk.Id<?> id) {
        this.locks.unlock(getBase(this.directoryBitSize, id, this.idRegistry.getId(id)));
    }

    @Override
    public boolean contains(final Chunk.Id<?> id) throws InterruptedException {
        return getFile(id).exists();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Index getIndex(final Chunk.Id<?> id) throws MyRuleException, IOException, InterruptedException {
        final BytesConvertible.Parser<SimpleIndex> parser = SimpleIndex.getParser(this.idRegistry.getParser((Class<? extends Chunk.Id<?>>) id.getClass()));
        if (parser == null) {
            throw new IllegalArgumentException("Not registered chunk id type ( " + id.getClass() + " ).");
        }

        final String base = getBase(this.directoryBitSize, id, this.idRegistry.getId(id));
        final File file = new File(this.root, base);
        this.locks.lock(base);
        try {
            if (!file.exists()) {
                return null;
            }

            final List<Index> index = new ArrayList<>(1);
            try (final InputStream input = new BufferedInputStream(new FileInputStream(file), INDEX_READ_BUFFER_SIZE)) {
                SimpleIndex.getParser(this.idRegistry.getParser((Class<? extends Chunk.Id<?>>) id.getClass())).fromStream(input, this.fileSizeLimit, index);
            }
            return index.get(0);
        } finally {
            this.locks.unlock(base);
        }
    }

    /**
     * バイト列をつなげる。
     * @param front 前になるバイト列
     * @param back 後ろになるバイト列
     * @param bitPos front の末尾バイトと back の先頭バイトをつなげるビット位置 (上位から数える)。
     *            0 の場合は front の末尾バイトと back の先頭バイトは重ならない。
     * @return つなげたバイト列
     */
    private static byte[] conjugate(final byte[] front, final byte[] back, final int bitPos) {
        if (bitPos == 0) {
            final byte[] buff = new byte[front.length + back.length];
            System.arraycopy(front, 0, buff, 0, front.length);
            System.arraycopy(back, 0, buff, front.length, back.length);
            return buff;
        } else {
            final byte[] buff = new byte[front.length + back.length - 1];
            System.arraycopy(front, 0, buff, 0, front.length - 1);
            System.arraycopy(back, 1, buff, front.length, back.length - 1);
            final int frontMask = (1 << Byte.SIZE) - 1 - ((1 << (Byte.SIZE - bitPos)) - 1);
            final int backMask = (1 << (Byte.SIZE - bitPos)) - 1;
            buff[front.length - 1] = (byte) ((front[front.length - 1] & frontMask) | (back[0] & backMask));
            // System.out.printf("DEBUG %d %x %x %n", bitPos, frontMask, backMask);
            return buff;
        }
    }

    /**
     * 指定したディレクトリの直下にファイルがあるデータ片を列挙する
     * @param dirBytes ディレクトリを示すバイト列
     * @param min 列挙するデータ片の最小論理位置
     * @param max 列挙するデータ片の最大論理位置
     * @return ディレクトリにあった全データ片の概要。
     *         ディレクトリが無い場合は null
     * @throws IOException 読み込み異常
     * @throws InterruptedException 割り込まれた場合
     */
    private Map<Chunk.Id<?>, Storage.Index> getDirIndices(final byte[] dirBytes, final Address min, final Address max) throws IOException, InterruptedException {
        final String dirStr = toFileString(dirBytes);
        final File dir = new File(this.root, dirStr);
        final File[] files = dir.listFiles();
        if (files == null || files.length <= 0) {
            return null;
        }
        final Map<Chunk.Id<?>, Storage.Index> indices = new HashMap<>();
        for (final File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            try {
                final byte[] fileBytes = fromFileString(file.getName());
                final byte[] buff = conjugate(dirBytes, fileBytes, this.directoryBitSize % Byte.SIZE);

                final List<Address> address = new ArrayList<>(1);
                final long[] type = new long[1];
                BytesConversion.fromBytes(buff, buff.length, "ol", address, Address.getParser(), type);

                if (address.get(0).compareTo(min) < 0 || max.compareTo(address.get(0)) < 0) {
                    // 範囲外。
                    continue;
                }
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = this.idRegistry.getParser(type[0]);
                if (parser == null) {
                    LOG.log(Level.WARNING, "{0} のデータ片の型 ( {1} ) は未登録です。", new Object[] { file.getPath(), type[0] });
                    continue;
                }

                final List<Index> index = new ArrayList<>(1);
                String base;
                if (this.directoryBitSize == 0) {
                    base = file.getName();
                } else {
                    base = (new StringBuilder(file.getParentFile().getName())).append(File.separator).append(file.getName()).toString();
                }

                this.locks.lock(base);
                try {
                    if (!file.exists()) {
                        LOG.log(Level.WARNING, "{0} を読むには一足遅かったようです。", file.getPath());
                        continue;
                    }
                    try (final InputStream input = new BufferedInputStream(new FileInputStream(file), INDEX_READ_BUFFER_SIZE)) {
                        SimpleIndex.getParser(parser).fromStream(input, this.fileSizeLimit, index);
                    }
                } finally {
                    this.locks.unlock(base);
                }

                indices.put(index.get(0).getId(), index.get(0));
            } catch (final MyRuleException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                if (moveToTrash(file)) {
                    LOG.log(Level.INFO, "{0} を除外しました。", file.getPath());
                } else {
                    LOG.log(Level.INFO, "{0} を無視します。", file.getPath());
                }
            }
        }
        return indices;
    }

    /**
     * バイト列の末尾バイトの指定したビット位置に 1 を足す。
     * @param bytes バイト列
     * @param digit ビット位置
     */
    private static void addBitOnLastByte(final byte[] bytes, final int digit) {
        final int value = (((1 << Byte.SIZE) - 1) & bytes[bytes.length - 1]) + (1 << digit);
        bytes[bytes.length - 1] = (byte) value;
        if ((1 << Byte.SIZE) <= value) {
            for (int i = bytes.length - 2; i >= 0; i--) {
                bytes[i]++;
                if (bytes[i] != 0) {
                    break;
                }
            }
        }
    }

    @Override
    public Map<Chunk.Id<?>, Storage.Index> getIndices(final Address min, final Address max) throws IOException, InterruptedException {

        final byte[] minBytes = getFront(BytesConversion.toBytes(min), this.directoryBitSize);
        final byte[] maxBytes = getFront(BytesConversion.toBytes(max), this.directoryBitSize);

        Map<Chunk.Id<?>, Storage.Index> indices = null;

        // ディレクトリを表すバイト列の最下位ビットは最下位バイトの 1 << digit の位置。
        final int digit = (this.directoryBitSize % Byte.SIZE == 0 ? 0 : Byte.SIZE - this.directoryBitSize % Byte.SIZE);
        final byte[] buff = minBytes.clone(); // minBytes を使っても問題無いけど。
        while (true) {
            final Map<Chunk.Id<?>, Storage.Index> dirIndices = getDirIndices(buff, min, max);
            if (dirIndices != null) {
                if (indices == null) {
                    indices = dirIndices;
                } else {
                    indices.putAll(dirIndices);
                }
            }

            if (Arrays.equals(buff, maxBytes)) {
                break;
            }

            addBitOnLastByte(buff, digit);
        }

        if (indices == null) {
            return new HashMap<>();
        } else {
            return indices;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Chunk> T read(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException {
        final BytesConvertible.Parser<SimpleIndex> parser = SimpleIndex.getParser(this.idRegistry.getParser((Class<? extends Chunk.Id<?>>) id.getClass()));
        if (parser == null) {
            throw new IllegalArgumentException("Not registered chunk id type ( " + id.getClass() + " ).");
        }

        // 読み込み。
        final List<Storage.Index> index = new ArrayList<>(1);
        final List<Chunk> chunk = new ArrayList<>(1);
        final String base = getBase(this.directoryBitSize, id, this.idRegistry.getId(id));
        final File file = new File(this.root, base);
        this.locks.lock(base);
        try {
            if (!file.exists()) {
                return null;
            }
            try (final InputStream input = new BufferedInputStream(new FileInputStream(file))) {
                BytesConversion.fromStream(input, this.fileSizeLimit, "oo", index, parser, chunk, this.chunkRegistry.getParser(id.getChunkClass()));
            }
        } finally {
            this.locks.unlock(base);
        }

        // 検査。
        final HashValue hashValue = chunk.get(0).getHashValue();
        if (!hashValue.equals(index.get(0).getHashValue())) {
            LOG.log(Level.WARNING, "再計算したハッシュ値 ( {0} ) が読み込んだハッシュ値 ( {1} ) と異なります。", new Object[] { hashValue, index.get(0).getHashValue() });
        }

        return (T) chunk.get(0);
    }

    @Override
    public boolean write(final Chunk chunk) throws IOException, InterruptedException {
        final String base = getBase(this.directoryBitSize, chunk.getId(), this.idRegistry.getId(chunk.getId()));
        final File file = new File(this.root, base);

        // ディレクトリ存在検査。
        final File dir = file.getParentFile();
        if (!dir.exists()) {
            if (dir.mkdir()) {
                LOG.log(Level.FINEST, "ディレクトリ {0} を作成しました。", dir.getPath());
            } else if (dir.exists()) {
                // exists と mkdir の間に他の誰かが作成した。
            } else {
                throw new IOException("Cannot make directory ( " + dir.getPath() + " ).");
            }
        }

        final Index index = new SimpleIndex(chunk);
        Index old = null;
        try {
            old = getIndex(chunk.getId());
        } catch (final MyRuleException e) {
            LOG.log(Level.WARNING, "異常が発生しました", e);
            if (moveToTrash(file)) {
                LOG.log(Level.INFO, "保存されていた {0} を除外しました。", chunk.getId());
            } else {
                LOG.log(Level.INFO, "保存されていた {0} を無視します。", chunk.getId());
            }
        }
        if (index.equals(old)) {
            // TODO 本当に同じとみなして良いか？一応ハッシュ値も比べてる。
            return false;
        }

        final int size;
        this.locks.lock(base);
        try {
            try (final OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                size = BytesConversion.toStream(output, "oo", index, chunk);
                output.flush();
            }
        } finally {
            this.locks.unlock(base);
        }
        LOG.log(Level.FINEST, "{0} のデータ片を {1} に書き込みました。", new Object[] { chunk.getId().getAddress(), base });
        if (this.fileSizeLimit < size) {
            throw new IllegalArgumentException("Too large file size ( " + size + " ) over limit ( " + this.fileSizeLimit + " ).");
        }
        return true;
    }

    @Override
    public void forceWrite(final Chunk chunk) throws IOException, InterruptedException {
        final String base = getBase(this.directoryBitSize, chunk.getId(), this.idRegistry.getId(chunk.getId()));
        final File file = new File(this.root, base);

        // ディレクトリ存在検査。
        final File dir = file.getParentFile();
        if (!dir.exists()) {
            if (dir.mkdir()) {
                LOG.log(Level.FINEST, "ディレクトリ {0} を作成しました。", dir.getPath());
            } else if (dir.exists()) {
                // exists と mkdir の間に他の誰かが作成した。
            } else {
                throw new IOException("Cannot make directory ( " + dir.getPath() + " ).");
            }
        }

        final Index index = new SimpleIndex(chunk);
        final int size;
        this.locks.lock(base);
        try {
            try (final OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                size = BytesConversion.toStream(output, "oo", index, chunk);
                output.flush();
            }
        } finally {
            this.locks.unlock(base);
        }
        LOG.log(Level.FINEST, "{0} のデータ片を {1} に書き込みました。", new Object[] { chunk.getId().getAddress(), base });
        if (this.fileSizeLimit < size) {
            throw new IllegalArgumentException("Too large file size ( " + size + " ) over limit ( " + this.fileSizeLimit + " ).");
        }

    }

    @Override
    public boolean delete(final Chunk.Id<?> id) throws IOException, InterruptedException {
        final String base = getBase(this.directoryBitSize, id, this.idRegistry.getId(id));
        final File file = new File(this.root, base);
        this.locks.lock(base);
        try {
            if (!file.exists()) {
                return false;
            }
            if (file.delete()) {
                LOG.log(Level.FINEST, "{0} のデータ片 ( {1} ) を消しました。", new Object[] { id.getAddress(), base });
                return true;
            } else {
                return false;
            }
        } finally {
            this.locks.unlock(base);
        }
    }

    private boolean moveToTrash(final File file) throws InterruptedException {
        String base;
        if (this.directoryBitSize == 0) {
            base = file.getName();
        } else {
            base = (new StringBuilder(file.getParentFile().getName())).append(File.separator).append(file.getName()).toString();
        }

        final File dest = new File(this.trash, base);
        final File destDir = dest.getParentFile();
        if (!destDir.exists()) {
            if (dest.getParentFile().mkdir()) {
                LOG.log(Level.FINEST, "{0} を作成しました。", destDir.getPath());
            } else if (destDir.exists()) {
                // exists と mkdir の間に他の誰かが作成した。
            } else {
                LOG.log(Level.WARNING, "{0} を作成できませんでした。", destDir.getPath());
                return false;
            }
        }

        this.locks.lock(base);
        try {
            Files.move(file.toPath(), dest.toPath());
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "{0} を除外できませんでした。", base);
            return false;
        } finally {
            this.locks.unlock(base);
        }
        return true;
    }

    @Override
    public void close() {}

    private static void addBitOnLastByteTest() {
        final int loop = 100_000;
        final Random random = new Random();
        final byte[] bytes = new byte[2];
        random.nextBytes(bytes);
        bytes[0] &= (1 << (Byte.SIZE - 1)) - 1; // 正の BigInteger にするため。
        BigInteger value = new BigInteger(bytes);
        for (int i = 0; i < loop; i++) {
            // System.out.println(Hexadecimal.toHexadecimal(bytes));
            final byte[] valueBytes = value.toByteArray();
            final byte[] buff = new byte[bytes.length];
            if (buff.length <= valueBytes.length) {
                System.arraycopy(valueBytes, valueBytes.length - buff.length, buff, 0, buff.length);
            } else {
                System.arraycopy(valueBytes, 0, buff, buff.length - valueBytes.length, valueBytes.length);
            }
            if (!Arrays.equals(bytes, buff)) {
                System.out.println("Aho  " + Hexadecimal.toHexadecimal(bytes));
                System.out.println("Baka " + Hexadecimal.toHexadecimal(buff));
            }
            addBitOnLastByte(bytes, 3);
            value = value.add(BigInteger.valueOf(1 << 3));
        }
        System.out.println(".");
    }

    private static void fileNameTest() {
        final int loop = 100_000;
        final Random random = new Random();
        for (int i = 0; i < loop; i++) {
            final byte[] buff = new byte[(Address.SIZE + Byte.SIZE - 1) / Byte.SIZE];
            random.nextBytes(buff);
            for (int j = 1; j < Address.SIZE - 1; j++) {
                final byte[] front = getFront(buff, j);
                final byte[] back = getBack(buff, j);
                final byte[] buff2 = conjugate(front, back, j % Byte.SIZE);
                if (!Arrays.equals(buff, buff2)) {
                    System.out.println("Aho   " + Hexadecimal.toHexadecimal(buff));
                    System.out.println("Baka  " + Hexadecimal.toHexadecimal(front));
                    System.out.println("China " + Hexadecimal.toHexadecimal(back));
                    System.out.println("Debu  " + Hexadecimal.toHexadecimal(buff2));
                    return;
                }
            }
        }
        System.out.println(".");
    }

    public static void main(final String[] args) {
        addBitOnLastByteTest();
        fileNameTest();
    }

}
