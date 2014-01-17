/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.ConstantChunk;
import nippon.kawauso.chiraura.storage.VariableChunk;

/**
 * @author chirauraNoSakusha
 */
abstract class UsingRegistryTest<T extends BytesConvertible> extends BytesConvertibleTest<T> {

    final TypeRegistry<Chunk> chunkRegistry;
    final TypeRegistry<Chunk.Id<?>> idRegistry;
    final TypeRegistry<Mountain.Dust<?>> diffRegistry;

    public UsingRegistryTest() {
        super();

        this.chunkRegistry = TypeRegistries.newRegistry();
        this.idRegistry = TypeRegistries.newRegistry();
        this.diffRegistry = TypeRegistries.newRegistry();

        this.chunkRegistry.register(0, ConstantChunk.class, ConstantChunk.getParser());
        this.idRegistry.register(0, ConstantChunk.Id.class, ConstantChunk.Id.getParser());

        this.chunkRegistry.register(1, VariableChunk.class, VariableChunk.getParser());
        this.idRegistry.register(1, VariableChunk.Id.class, VariableChunk.Id.getParser());

        this.chunkRegistry.register(2, GrowingBytes.class, GrowingBytes.getParser());
        this.idRegistry.register(2, GrowingBytes.Id.class, GrowingBytes.Id.getParser());
        this.diffRegistry.register(2, GrowingBytes.Entry.class, GrowingBytes.Entry.getParser());
    }

    @Override
    protected int getNumOfExceptionLoops() {
        return 10 * getNumOfLoops();
    }

}
