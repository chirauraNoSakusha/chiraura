package nippon.kawauso.chiraura.closet.p2p;

/**
 * @author chirauraNoSakusha
 */
interface NonBlockingDriverSet {

    PeerAccessNonBlockingDriver getPeerAccessNonBlocking();

    AddressAccessNonBlockingDriver getAddressAccessNonBlocking();

    GetChunkNonBlockingDriver getGetChunkNonBlocking();

    GetCacheNonBlockingDriver getGetCacheNonBlocking();

    UpdateChunkNonBlockingDriver getUpdateChunkNonBlocking();

    GetOrUpdateCacheNonBlockingDriver getGetOrUpdateCacheNonBlocking();

    AddChunkNonBlockingDriver getAddChunkNonBlocking();

    AddCacheNonBlockingDriver getAddCacheNonBlocking();

    PatchChunkNonBlockingDriver getPatchChunkNonBlocking();

    PatchOrAddAndGetCacheNonBlockingDriver getPatchOrAddAndGetCacheNonBlocking();

    PatchAndGetOrUpdateCacheNonBlockingDriver getPatchAndGetOrUpdateCacheNonBlocking();

    SimpleRecoveryNonBlockingDriver getSimpleRecoveryNonBlocking();

    BackupOneNonBlockingDriver getBackupOneNonBlocking();

}
