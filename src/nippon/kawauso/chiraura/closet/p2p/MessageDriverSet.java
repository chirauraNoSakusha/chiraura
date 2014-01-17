/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * @author chirauraNoSakusha
 */
interface MessageDriverSet {

    ConnectReportDriver getConnectReport();

    PeerAccessMessageDriver getPeerAccessMessage();

    AddressAccessMessageDriver getAddressAccessMessage();

    GetChunkMessageDriver getGetChunkMessage();

    UpdateChunkMessageDriver getUpdateChunkMessage();

    AddChunkMessageDriver getAddChunkMessage();

    PatchChunkMessageDriver getPatchChunkMessage();

    PatchAndGetOrUpdateCacheMessageDriver getPatchAndGetOrUpdateCacheMessage();

    GetCacheMessageDriver getGetCacheMessage();

    GetOrUpdateCacheMessageDriver getGetOrUpdateCacheMessage();

    AddCacheMessageDriver getAddCacheMessage();

    PatchOrAddAndGetCacheMessageDriver getPatchOrAddAndGetCacheMessage();

    CheckStockMessageDriver getCheckStockMessage();

    CheckDemandMessageDriver getCheckDemandMessage();

    RecoveryMessageDriver getRecoveryMessage();

    BackupMessageDriver getBackupMessage();

    SimpleRecoveryMessageDriver getSimpleRecoveryMessage();

    CheckOneDemandMessageDriver getCheckOneDemandMessage();

}
