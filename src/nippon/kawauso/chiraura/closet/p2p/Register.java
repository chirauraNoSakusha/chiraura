package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class Register {

    // インスタンス化防止。
    private Register() {}

    static void init(final NetworkWrapper network, final StorageWrapper storage) {
        final TypeRegistry<Chunk> chunkRegistry = storage.getChunkRegistry();
        final TypeRegistry<Chunk.Id<?>> idRegistry = storage.getIdRegistry();
        final TypeRegistry<Mountain.Dust<?>> diffRegistry = storage.getDiffRegistry();

        long id;

        id = 16L;
        network.registerMessage(id++, SessionMessage.class, SessionMessage.getParser());
        network.registerMessage(id++, SessionReply.class, SessionReply.getParser());
        network.registerMessage(id++, PeerAccessMessage.class, PeerAccessMessage.getParser());
        network.registerMessage(id++, PeerAccessReply.class, PeerAccessReply.getParser());
        network.registerMessage(id++, AddressAccessMessage.class, AddressAccessMessage.getParser());
        network.registerMessage(id++, AddressAccessReply.class, AddressAccessReply.getParser());

        id = 32L;
        network.registerMessage(id++, CheckStockMessage.class, CheckStockMessage.getParser(idRegistry));
        network.registerMessage(id++, CheckStockReply.class, CheckStockReply.getParser(idRegistry));
        network.registerMessage(id++, CheckDemandMessage.class, CheckDemandMessage.getParser(idRegistry));
        network.registerMessage(id++, CheckDemandReply.class, CheckDemandReply.getParser(idRegistry));
        network.registerMessage(id++, RecoveryMessage.class, RecoveryMessage.getParser(idRegistry));
        network.registerMessage(id++, RecoveryReply.class, RecoveryReply.getParser(chunkRegistry, idRegistry, diffRegistry));
        network.registerMessage(id++, BackupMessage.class, BackupMessage.getParser(chunkRegistry, idRegistry, diffRegistry));
        network.registerMessage(id++, BackupReply.class, BackupReply.getParser(chunkRegistry));
        network.registerMessage(id++, SimpleRecoveryMessage.class, SimpleRecoveryMessage.getParser(idRegistry));
        network.registerMessage(id++, SimpleRecoveryReply.class, SimpleRecoveryReply.getParser(chunkRegistry));
        network.registerMessage(id++, CheckOneDemandMessage.class, CheckOneDemandMessage.getParser(idRegistry));
        network.registerMessage(id++, CheckOneDemandReply.class, CheckOneDemandReply.getParser(idRegistry));

        id = 64L;
        network.registerMessage(id++, GetChunkMessage.class, GetChunkMessage.getParser(idRegistry));
        network.registerMessage(id++, GetChunkReply.class, GetChunkReply.getParser(chunkRegistry));
        network.registerMessage(id++, UpdateChunkMessage.class, UpdateChunkMessage.getParser(idRegistry));
        network.registerMessage(id++, UpdateChunkReply.class, UpdateChunkReply.getParser(diffRegistry));
        network.registerMessage(id++, AddChunkMessage.class, AddChunkMessage.getParser(chunkRegistry));
        network.registerMessage(id++, AddChunkReply.class, AddChunkReply.getParser());
        network.registerMessage(id++, PatchChunkMessage.class, PatchChunkMessage.getParser(idRegistry, diffRegistry));
        network.registerMessage(id++, PatchChunkReply.class, PatchChunkReply.getParser());
        network.registerMessage(id++, GetCacheMessage.class, GetCacheMessage.getParser(idRegistry));
        network.registerMessage(id++, GetCacheReply.class, GetCacheReply.getParser(chunkRegistry, idRegistry));
        network.registerMessage(id++, GetOrUpdateCacheMessage.class, GetOrUpdateCacheMessage.getParser(idRegistry));
        network.registerMessage(id++, GetOrUpdateCacheReply.class, GetOrUpdateCacheReply.getParser(chunkRegistry, idRegistry, diffRegistry));
        network.registerMessage(id++, AddCacheMessage.class, AddCacheMessage.getParser(chunkRegistry));
        network.registerMessage(id++, AddCacheReply.class, AddCacheReply.getParser());
        network.registerMessage(id++, PatchOrAddAndGetCacheMessage.class, PatchOrAddAndGetCacheMessage.getParser(chunkRegistry));
        network.registerMessage(id++, PatchOrAddAndGetCacheReply.class, PatchOrAddAndGetCacheReply.getParser(chunkRegistry));
        network.registerMessage(id++, PatchAndGetOrUpdateCacheMessage.class, PatchAndGetOrUpdateCacheMessage.getParser(idRegistry, diffRegistry));
        network.registerMessage(id++, PatchAndGetOrUpdateCacheReply.class, PatchAndGetOrUpdateCacheReply.getParser(chunkRegistry, idRegistry, diffRegistry));

    }
}
