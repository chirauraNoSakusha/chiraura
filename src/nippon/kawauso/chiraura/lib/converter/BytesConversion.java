/**
 * 
 */
package nippon.kawauso.chiraura.lib.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * <pre>
 * a 続く指定子の配列であることを示す
 * c 続く s,i,l が固定長形式であることを示す
 * b byte
 * s short
 * i int
 * l long
 * o BytesConvertible
 * </pre>
 * 
 * 指定子に対する引数の型は以下の通り。
 * 
 * <pre>
 * 指定子     変換元  変換先
 * b          byte    byte[1]
 * s,cs       short   short[1]
 * i,ci       int     int[1]
 * l,cl       long    long[1]
 * o          T       List<? super T> と BytesConvertible.Parser<T>
 * ab         byte[]  byte[1][]
 * as,acs,cas short[] short[1][]
 * ai,aci,cai int[]   int[1][]
 * al,acl,cal long[]  long[1][]
 * ao         Collection<T> List<? super T> と BytesConvertible.Parser<T>
 * </pre>
 * @author chirauraNoSakusha
 */
public final class BytesConversion {

    /**
     * long値をl指定子で変換した場合のバイト数。
     */
    public static final int LONG_BYTE_SIZE = LongBytesConversion.BYTE_SIZE;

    // インスタンス化防止。
    private BytesConversion() {}

    private static abstract class Skeleton {
        abstract void byteAction() throws Exception;

        abstract void shortAction() throws Exception;

        abstract void intAction() throws Exception;

        abstract void longAction() throws Exception;

        abstract void fixedLengthShortAction() throws Exception;

        abstract void fixedLengthIntAction() throws Exception;

        abstract void fixedLengthLongAction() throws Exception;

        abstract void objectAction() throws Exception;

        abstract void byteArrayAction() throws Exception;

        abstract void shortArrayAction() throws Exception;

        abstract void intArrayAction() throws Exception;

        abstract void longArrayAction() throws Exception;

        abstract void fixedLengthShortArrayAction() throws Exception;

        abstract void fixedLengthIntArrayAction() throws Exception;

        abstract void fixedLengthLongArrayAction() throws Exception;

        abstract void objectArrayAction() throws Exception;

        void beforeArray() throws Exception {}

        int size;

        int execute(final String fmt) throws Exception {
            this.size = 0;
            boolean isArray = false;
            boolean isConstant = false;
            for (int i = 0; i < fmt.length(); i++) {
                final char c = fmt.charAt(i);
                if (c == 'a') {
                    isArray = true;
                    continue;
                } else if (c == 'c') {
                    isConstant = true;
                    continue;
                }

                if (!isArray) {
                    if (c == 'b') {
                        if (!isConstant) {
                            byteAction();
                        } else {
                            throw new IllegalArgumentException("Byte label 'b' does not support constant flag 'c'.");
                        }
                    } else if (c == 's') {
                        if (!isConstant) {
                            shortAction();
                        } else {
                            fixedLengthShortAction();
                            isConstant = false;
                        }
                    } else if (c == 'i') {
                        if (!isConstant) {
                            intAction();
                        } else {
                            fixedLengthIntAction();
                            isConstant = false;
                        }
                    } else if (c == 'l') {
                        if (!isConstant) {
                            longAction();
                        } else {
                            fixedLengthLongAction();
                            isConstant = false;
                        }
                    } else if (c == 'o') {
                        if (!isConstant) {
                            objectAction();
                        } else {
                            throw new IllegalArgumentException("Object label 'o' does not support constant flag 'c'.");
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown label '" + c + "'.");
                    }
                } else {
                    beforeArray();

                    if (c == 'b') {
                        if (!isConstant) {
                            byteArrayAction();
                        } else {
                            throw new IllegalArgumentException("Byte label 'b' does not support constant flag 'c'.");
                        }
                    } else if (c == 's') {
                        if (!isConstant) {
                            shortArrayAction();
                        } else {
                            fixedLengthShortArrayAction();
                            isConstant = false;
                        }
                    } else if (c == 'i') {
                        if (!isConstant) {
                            intArrayAction();
                        } else {
                            fixedLengthIntArrayAction();
                            isConstant = false;
                        }
                    } else if (c == 'l') {
                        if (!isConstant) {
                            longArrayAction();
                        } else {
                            fixedLengthLongArrayAction();
                            isConstant = false;
                        }
                    } else if (c == 'o') {
                        if (!isConstant) {
                            objectArrayAction();
                        } else {
                            throw new IllegalArgumentException("Object label 'o' does not support constant flag 'c'.");
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown label '" + c + "'");
                    }

                    isArray = false;
                }
            }
            return this.size;
        }
    }

    /**
     * @param fmt 指定子
     * @param inputs データ
     * @return データを指定子に従って変換したバイト列のサイズ
     */
    public static int byteSize(final String fmt, final Object... inputs) {
        return new Skeleton() {
            int index = 0;

            @Override
            void byteAction() {
                this.index++;
                this.size++;
            }

            private void numberAction(final long value) {
                this.size += NumberBytesConversion.byteSize(value);
            }

            @Override
            void shortAction() {
                numberAction((short) inputs[this.index++]);
            }

            @Override
            void intAction() {
                numberAction((int) inputs[this.index++]);
            }

            @Override
            void longAction() {
                numberAction((long) inputs[this.index++]);
            }

            @Override
            void fixedLengthShortAction() {
                this.index++;
                this.size += ShortBytesConversion.BYTE_SIZE;
            }

            @Override
            void fixedLengthIntAction() {
                this.index++;
                this.size += IntBytesConversion.BYTE_SIZE;
            }

            @Override
            void fixedLengthLongAction() {
                this.index++;
                this.size += LongBytesConversion.BYTE_SIZE;
            }

            @Override
            void objectAction() {
                this.size += ((BytesConvertible) inputs[this.index++]).byteSize();
            }

            @Override
            void byteArrayAction() {
                final byte[] array = (byte[]) inputs[this.index++];
                numberAction(array.length);
                this.size += array.length;
            }

            @Override
            void shortArrayAction() {
                final short[] array = (short[]) inputs[this.index++];
                numberAction(array.length);
                for (final short value : array) {
                    numberAction(value);
                }
            }

            @Override
            void intArrayAction() {
                final int[] array = (int[]) inputs[this.index++];
                numberAction(array.length);
                for (final int value : array) {
                    numberAction(value);
                }
            }

            @Override
            void longArrayAction() {
                final long[] array = (long[]) inputs[this.index++];
                numberAction(array.length);
                for (final long value : array) {
                    numberAction(value);
                }
            }

            @Override
            void fixedLengthShortArrayAction() {
                final short[] array = (short[]) inputs[this.index++];
                numberAction(array.length);
                this.size += ShortBytesConversion.BYTE_SIZE * array.length;
            }

            @Override
            void fixedLengthIntArrayAction() {
                final int[] array = (int[]) inputs[this.index++];
                numberAction(array.length);
                this.size += IntBytesConversion.BYTE_SIZE * array.length;
            }

            @Override
            void fixedLengthLongArrayAction() {
                final long[] array = (long[]) inputs[this.index++];
                numberAction(array.length);
                this.size += LongBytesConversion.BYTE_SIZE * array.length;
            }

            @Override
            void objectArrayAction() {
                @SuppressWarnings("unchecked")
                final Collection<? extends BytesConvertible> array = (Collection<? extends BytesConvertible>) (inputs[this.index++]);
                numberAction(array.size());
                for (final BytesConvertible object : array) {
                    this.size += object.byteSize();
                }
            }

            @Override
            int execute(final String arg) {
                try {
                    return super.execute(arg);
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    // ここには来ない。
                    throw new RuntimeException(e);
                }
            }
        }.execute(fmt);
    }

    /**
     * データを指定子に従ってバイト列に変換する。
     * @param output 書き込み先
     * @param fmt 指定子
     * @param inputs データ
     * @return 書き込みサイズ
     * @throws IOException 書き込みエラー
     */
    public static int toStream(final OutputStream output, final String fmt, final Object... inputs) throws IOException {
        return new Skeleton() {
            private int index = 0;

            @Override
            void byteAction() throws IOException {
                output.write((byte) inputs[this.index++]);
                this.size++;
            }

            private void numberAction(final long value) throws IOException {
                this.size += NumberBytesConversion.toStream(value, output);
            }

            @Override
            void shortAction() throws IOException {
                numberAction((short) inputs[this.index++]);
            }

            @Override
            void intAction() throws IOException {
                numberAction((int) inputs[this.index++]);
            }

            @Override
            void longAction() throws IOException {
                numberAction((long) inputs[this.index++]);
            }

            @Override
            void fixedLengthShortAction() throws IOException {
                this.size += ShortBytesConversion.toStream((short) inputs[this.index++], output);
            }

            @Override
            void fixedLengthIntAction() throws IOException {
                this.size += IntBytesConversion.toStream((int) inputs[this.index++], output);
            }

            @Override
            void fixedLengthLongAction() throws IOException {
                this.size += LongBytesConversion.toStream((long) inputs[this.index++], output);
            }

            @Override
            void objectAction() throws IOException {
                this.size += ((BytesConvertible) inputs[this.index++]).toStream(output);
            }

            @Override
            void byteArrayAction() throws IOException {
                final byte[] array = (byte[]) inputs[this.index++];
                numberAction(array.length);
                output.write(array);
                this.size += array.length;
            }

            @Override
            void shortArrayAction() throws IOException {
                final short[] array = (short[]) inputs[this.index++];
                numberAction(array.length);
                for (final short value : array) {
                    numberAction(value);
                }
            }

            @Override
            void intArrayAction() throws IOException {
                final int[] array = (int[]) inputs[this.index++];
                numberAction(array.length);
                for (final int value : array) {
                    numberAction(value);
                }
            }

            @Override
            void longArrayAction() throws IOException {
                final long[] array = (long[]) inputs[this.index++];
                numberAction(array.length);
                for (final long value : array) {
                    numberAction(value);
                }
            }

            @Override
            void fixedLengthShortArrayAction() throws IOException {
                final short[] array = (short[]) inputs[this.index++];
                numberAction(array.length);
                for (final short value : array) {
                    this.size += ShortBytesConversion.toStream(value, output);
                }
            }

            @Override
            void fixedLengthIntArrayAction() throws IOException {
                final int[] array = (int[]) inputs[this.index++];
                numberAction(array.length);
                for (final int value : array) {
                    this.size += IntBytesConversion.toStream(value, output);
                }
            }

            @Override
            void fixedLengthLongArrayAction() throws IOException {
                final long[] array = (long[]) inputs[this.index++];
                numberAction(array.length);
                for (final long value : array) {
                    this.size += LongBytesConversion.toStream(value, output);
                }
            }

            @Override
            void objectArrayAction() throws IOException {
                @SuppressWarnings("unchecked")
                final Collection<? extends BytesConvertible> array = (Collection<? extends BytesConvertible>) (inputs[this.index++]);
                numberAction(array.size());
                for (final BytesConvertible object : array) {
                    this.size += object.toStream(output);
                }
            }

            @Override
            int execute(final String arg) throws IOException {
                try {
                    return super.execute(arg);
                } catch (final RuntimeException | IOException e) {
                    throw e;
                } catch (final Exception e) {
                    // ここには来ない。
                    throw new RuntimeException(e);
                }
            }
        }.execute(fmt);
    }

    private static <T> int objectFromStream(final InputStream input, final int maxByteSize, final Object outputObject, final Object parserObject)
            throws MyRuleException, IOException {
        @SuppressWarnings("unchecked")
        final BytesConvertible.Parser<T> parserObject0 = (BytesConvertible.Parser<T>) parserObject;
        @SuppressWarnings("unchecked")
        final List<? super T> outputObject0 = (List<? super T>) outputObject;
        return parserObject0.fromStream(input, maxByteSize, outputObject0);
    }

    /**
     * データを指定子に従ってバイト列から読み込む。
     * @param input データが指定子に従って変換されているバイト列が流れてくるストリーム
     * @param maxByteSize 読み込む最大バイト数
     * @param fmt 指定子
     * @param outputs 結果を格納する長さが1以上の配列と復号器
     * @return 読み込みサイズ
     * @throws MyRuleException 入力バイト列の規約違反
     * @throws IOException 読み込みエラー
     */
    public static int fromStream(final InputStream input, final int maxByteSize, final String fmt, final Object... outputs) throws MyRuleException, IOException {
        return new Skeleton() {
            private int index = 0;

            private void checkSize(final long targetSize) throws MyRuleException {
                if (targetSize > maxByteSize) {
                    throw new MyRuleException("Too large read size ( " + targetSize + " ) over limit ( " + maxByteSize + " ).");
                }
            }

            @Override
            void byteAction() throws MyRuleException, IOException {
                checkSize(this.size + 1);
                this.size += StreamFunctions.completeRead(input, (byte[]) outputs[this.index++], 0, 1);
            }

            @Override
            void shortAction() throws MyRuleException, IOException {
                this.size += NumberBytesConversion.shortFromStream(input, maxByteSize - this.size, (short[]) outputs[this.index++]);
            }

            @Override
            void intAction() throws MyRuleException, IOException {
                this.size += NumberBytesConversion.intFromStream(input, maxByteSize - this.size, (int[]) outputs[this.index++]);
            }

            @Override
            void longAction() throws MyRuleException, IOException {
                this.size += NumberBytesConversion.fromStream(input, maxByteSize - this.size, (long[]) outputs[this.index++]);
            }

            @Override
            void fixedLengthShortAction() throws MyRuleException, IOException {
                checkSize(this.size + ShortBytesConversion.BYTE_SIZE);
                this.size += ShortBytesConversion.fromStream(input, (short[]) outputs[this.index++]);
            }

            @Override
            void fixedLengthIntAction() throws MyRuleException, IOException {
                checkSize(this.size + IntBytesConversion.BYTE_SIZE);
                this.size += IntBytesConversion.fromStream(input, (int[]) outputs[this.index++]);
            }

            @Override
            void fixedLengthLongAction() throws MyRuleException, IOException {
                checkSize(this.size + LongBytesConversion.BYTE_SIZE);
                this.size += LongBytesConversion.fromStream(input, (long[]) outputs[this.index++]);
            }

            @Override
            void objectAction() throws MyRuleException, IOException {
                this.size += objectFromStream(input, maxByteSize - this.size, outputs[this.index++], outputs[this.index++]);
            }

            private int length = 0;

            @Override
            void beforeArray() throws MyRuleException, IOException {
                final int[] buff = new int[1];
                this.size += NumberBytesConversion.intFromStream(input, maxByteSize - this.size, buff);
                this.length = buff[0];
                if (this.length < 0) {
                    throw new MyRuleException("Negative array length ( " + this.length + " ).");
                }
            }

            @Override
            void byteArrayAction() throws MyRuleException, IOException {
                checkSize(this.size + this.length);
                ((byte[][]) outputs[this.index++])[0] = StreamFunctions.completeRead(input, this.length);
                this.size += this.length;
            }

            @Override
            void shortArrayAction() throws MyRuleException, IOException {
                final short[] array = ((short[][]) outputs[this.index++])[0] = new short[this.length];
                final short[] buff = new short[1];
                for (int i = 0; i < this.length; i++) {
                    this.size += NumberBytesConversion.shortFromStream(input, maxByteSize - this.size, buff);
                    array[i] = buff[0];
                }
            }

            @Override
            void intArrayAction() throws MyRuleException, IOException {
                final int[] array = ((int[][]) outputs[this.index++])[0] = new int[this.length];
                final int[] buff = new int[1];
                for (int i = 0; i < this.length; i++) {
                    this.size += NumberBytesConversion.intFromStream(input, maxByteSize - this.size, buff);
                    array[i] = buff[0];
                }
            }

            @Override
            void longArrayAction() throws MyRuleException, IOException {
                final long[] array = ((long[][]) outputs[this.index++])[0] = new long[this.length];
                final long[] buff = new long[1];
                for (int i = 0; i < this.length; i++) {
                    this.size += NumberBytesConversion.fromStream(input, maxByteSize - this.size, buff);
                    array[i] = buff[0];
                }
            }

            @Override
            void fixedLengthShortArrayAction() throws MyRuleException, IOException {
                checkSize(this.size + ShortBytesConversion.BYTE_SIZE * (long) this.length);
                final short[] array = ((short[][]) outputs[this.index++])[0] = new short[this.length];
                final short[] buff = new short[1];
                for (int i = 0; i < this.length; i++) {
                    this.size += ShortBytesConversion.fromStream(input, buff);
                    array[i] = buff[0];
                }
            }

            @Override
            void fixedLengthIntArrayAction() throws MyRuleException, IOException {
                checkSize(this.size + IntBytesConversion.BYTE_SIZE * (long) this.length);
                final int[] array = ((int[][]) outputs[this.index++])[0] = new int[this.length];
                final int[] buff = new int[1];
                for (int i = 0; i < this.length; i++) {
                    this.size += IntBytesConversion.fromStream(input, buff);
                    array[i] = buff[0];
                }
            }

            @Override
            void fixedLengthLongArrayAction() throws MyRuleException, IOException {
                checkSize(this.size + LongBytesConversion.BYTE_SIZE * (long) this.length);
                final long[] array = ((long[][]) outputs[this.index++])[0] = new long[this.length];
                final long[] buff = new long[1];
                for (int i = 0; i < this.length; i++) {
                    this.size += LongBytesConversion.fromStream(input, buff);
                    array[i] = buff[0];
                }
            }

            @Override
            void objectArrayAction() throws MyRuleException, IOException {
                final Object output = outputs[this.index++];
                final Object parser = outputs[this.index++];
                for (int i = 0; i < this.length; i++) {
                    this.size += objectFromStream(input, maxByteSize - this.size, output, parser);
                }
            }

            @Override
            int execute(final String arg) throws MyRuleException, IOException {
                try {
                    return super.execute(arg);
                } catch (final RuntimeException | IOException | MyRuleException e) {
                    throw e;
                } catch (final Exception e) {
                    // ここには来ない。
                    throw new RuntimeException(e);
                }
            }
        }.execute(fmt);
    }

    /**
     * データを指定子に従ってバイト列に変換する。
     * @param fmt 指定子
     * @param inputs データ
     * @return バイト列
     */
    public static byte[] toBytes(final String fmt, final Object... inputs) {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try {
            toStream(buff, fmt, inputs);
        } catch (final IOException e) {
            // ByteArrayOutputStream だから、ここには来ない。
            throw new RuntimeException(e);
        }
        return buff.toByteArray();
    }

    /**
     * バイト列に変換する。
     * @param inputs データ
     * @return バイト列
     */
    public static byte[] toBytes(final BytesConvertible... inputs) {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try {
            for (final BytesConvertible input : inputs) {
                input.toStream(buff);
            }
        } catch (final IOException e) {
            // ByteArrayOutputStream だから、ここには来ない。
            throw new RuntimeException(e);
        }
        return buff.toByteArray();
    }

    /**
     * データを指定子に従ってバイト列から読み込む。
     * @param input データが指定子に従って変換されているバイト列
     * @param maxByteSize 読み込む最大バイト数
     * @param fmt 指定子
     * @param outputs 結果を格納する長さが1以上の配列と復号器
     * @return 読み込みサイズ
     * @throws MyRuleException 入力バイト列の規約違反
     */
    public static int fromBytes(final byte[] input, final int maxByteSize, final String fmt, final Object... outputs) throws MyRuleException {
        try {
            return fromStream(new ByteArrayInputStream(input), maxByteSize, fmt, outputs);
        } catch (final MyRuleException e) {
            throw e;
        } catch (final IOException e) {
            // ByteArrayInputStream だから、ここには来ない。
            throw new RuntimeException(e);
        }
    }

    /**
     * バイト列から復号する。
     * @param <T> 復元するものの型
     * @param input 入力バイト列
     * @param parser 復号器
     * @return 復号したもの
     * @throws MyRuleException 入力バイト列の規約違反
     */
    public static <T> T fromBytes(final byte[] input, final BytesConvertible.Parser<T> parser) throws MyRuleException {
        final List<T> output = new ArrayList<>(1);
        try {
            parser.fromStream(new ByteArrayInputStream(input), input.length, output);
        } catch (final IOException e) {
            // ByteArrayInputStream だから、ここには来ない。
            throw new RuntimeException(e);
        }
        return output.get(0);
    }

}
