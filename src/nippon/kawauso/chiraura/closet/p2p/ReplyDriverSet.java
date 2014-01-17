/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * @author chirauraNoSakusha
 */
interface ReplyDriverSet {

    PeerAccessReplyDriver getPeerAccessReply();

    AddressAccessReplyDriver getAddressAccessReply();

    GetChunkReplyDriver getGetChunkReply();

    UpdateChunkReplyDriver getUpdateChunkReply();

    AddChunkReplyDriver getAddChunkReply();

    PatchChunkReplyDriver getPatchChunkReply();

    PatchAndGetOrUpdateCacheReplyDriver getPatchAndGetOrUpdateCacheReply();

    GetCacheReplyDriver getGetCacheReply();

    GetOrUpdateCacheReplyDriver getGetOrUpdateCacheReply();

    AddCacheReplyDriver getAddCacheReply();

    PatchOrAddAndGetCacheReplyDriver getPatchOrAddAndGetCacheReply();

    CheckStockReplyDriver getCheckStockReply();

    CheckDemandReplyDriver getCheckDemandReply();

    RecoveryReplyDriver getRecoveryReply();

    BackupReplyDriver getBackupReply();

    SimpleRecoveryReplyDriver getSimpleRecoveryReply();

    CheckOneDemandReplyDriver getCheckOneDemandReply();

}
