package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class DriverSet implements MessageDriverSet, ReplyDriverSet, NonBlockingDriverSet, BackupDriverSet, MessengerReportDriverSet {

    private final PeerAccessDriver peerAccess;
    private final PeerAccessNonBlockingDriver peerAccessNonBlocking;
    private final PeerAccessSelectDriver peerAccessSelect;
    private final PeerAccessMessageDriver peerAccessMessage;
    private final PeerAccessReplyDriver peerAccessReply;

    private final AddressAccessDriver addressAccess;
    private final AddressAccessBlockingDriver addressAccessBlocking;
    private final AddressAccessNonBlockingDriver addressAccessNonBlocking;
    private final AddressAccessMessageDriver addressAccessMessage;
    private final AddressAccessReplyDriver addressAccessReply;

    private final GetChunkDriver getChunk;
    private final GetChunkBlockingDriver getChunkBlocking;
    private final GetChunkNonBlockingDriver getChunkNonBlocking;
    private final GetChunkMessageDriver getChunkMessage;
    private final GetChunkReplyDriver getChunkReply;

    private final UpdateChunkDriver updateChunk;
    private final UpdateChunkBlockingDriver updateChunkBlocking;
    private final UpdateChunkNonBlockingDriver updateChunkNonBlocking;
    private final UpdateChunkMessageDriver updateChunkMessage;
    private final UpdateChunkReplyDriver updateChunkReply;

    private final AddChunkDriver addChunk;
    private final AddChunkBlockingDriver addChunkBlocking;
    private final AddChunkNonBlockingDriver addChunkNonBlocking;
    private final AddChunkMessageDriver addChunkMessage;
    private final AddChunkReplyDriver addChunkReply;

    private final PatchChunkDriver patchChunk;
    private final PatchChunkBlockingDriver patchChunkBlocking;
    private final PatchChunkNonBlockingDriver patchChunkNonBlocking;
    private final PatchChunkMessageDriver patchChunkMessage;
    private final PatchChunkReplyDriver patchChunkReply;

    private final GetCacheDriver getCache;
    private final GetCacheBlockingDriver getCacheBlocking;
    private final GetCacheNonBlockingDriver getCacheNonBlocking;
    private final GetCacheMessageDriver getCacheMessage;
    private final GetCacheReplyDriver getCacheReply;

    private final GetOrUpdateCacheDriver getOrUpdateCache;
    private final GetOrUpdateCacheBlockingDriver getOrUpdateCacheBlocking;
    private final GetOrUpdateCacheNonBlockingDriver getOrUpdateCacheNonBlocking;
    private final GetOrUpdateCacheMessageDriver getOrUpdateCacheMessage;
    private final GetOrUpdateCacheReplyDriver getOrUpdateCacheReply;

    private final AddCacheDriver addCache;
    private final AddCacheBlockingDriver addCacheBlocking;
    private final AddCacheNonBlockingDriver addCacheNonBlocking;
    private final AddCacheMessageDriver addCacheMessage;
    private final AddCacheReplyDriver addCacheReply;

    private final PatchOrAddAndGetCacheDriver patchOrAddAndGetCache;
    private final PatchOrAddAndGetCacheBlockingDriver patchOrAddAndGetCacheBlocking;
    private final PatchOrAddAndGetCacheNonBlockingDriver patchOrAddAndGetCacheNonBlocking;
    private final PatchOrAddAndGetCacheMessageDriver patchOrAddAndGetCacheMessage;
    private final PatchOrAddAndGetCacheReplyDriver patchOrAddAndGetCacheReply;

    private final PatchAndGetOrUpdateCacheDriver patchAndGetOrUpdateCache;
    private final PatchAndGetOrUpdateCacheBlockingDriver patchAndGetOrUpdateCacheBlocking;
    private final PatchAndGetOrUpdateCacheNonBlockingDriver patchAndGetOrUpdateCacheNonBlocking;
    private final PatchAndGetOrUpdateCacheMessageDriver patchAndGetOrUpdateCacheMessage;
    private final PatchAndGetOrUpdateCacheReplyDriver patchAndGetOrUpdateCacheReply;

    private final CheckStockDriver checkStock;
    private final CheckStockBlockingDriver checkStockBlocking;
    private final CheckStockMessageDriver checkStockMessage;
    private final CheckStockReplyDriver checkStockReply;

    private final CheckDemandDriver checkDemand;
    private final CheckDemandBlockingDriver checkDemandBlocking;
    private final CheckDemandMessageDriver checkDemandMessage;
    private final CheckDemandReplyDriver checkDemandReply;

    private final RecoveryDriver recovery;
    private final RecoveryNonBlockingDriver recoveryNonBlocking;
    private final RecoverySelectDriver recoverySelect;
    private final RecoveryMessageDriver recoveryMessage;
    private final RecoveryReplyDriver recoveryReply;

    private final BackupDriver backup;
    private final BackupBlockingDriver backupBlocking;
    private final BackupSelectDriver backupSelect;
    private final BackupNonBlockingDriver backupNonBlocking;
    private final BackupMessageDriver backupMessage;
    private final BackupReplyDriver backupReply;

    private final SimpleRecoveryDriver simpleRecovery;
    private final SimpleRecoveryNonBlockingDriver simpleRecoveryNonBlocking;
    private final SimpleRecoveryMessageDriver simpleRecoveryMessage;
    private final SimpleRecoveryReplyDriver simpleRecoveryReply;

    private final BackupOneDriver backupOne;
    private final BackupOneNonBlockingDriver backupOneNonBlocking;

    private final CheckOneDemandDriver checkOneDemand;
    private final CheckOneDemandBlockingDriver checkOneDemandNonBlocking;
    private final CheckOneDemandMessageDriver checkOneDemandMessage;
    private final CheckOneDemandReplyDriver checkOneDemandReply;

    private final FirstAccessDriver firstAccess;
    private final FirstAccessSelectDriver firstAccessSelect;

    private final ConnectReportDriver connectReport;
    private final CommunicationErrorDriver communicationError;
    private final ContactErrorDriver contactError;
    private final AcceptanceErrorDriver acceptanceError;
    private final UnsentMailDriver unsentMail;
    private final ClosePortWarningDriver closePortWarning;

    DriverSet(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
            final BlockingQueue<Operation> operationSink, final ExecutorService executor, final int checkChunkLimit) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (operationSink == null) {
            throw new IllegalArgumentException("Null operation sink.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (checkChunkLimit < 0) {
            throw new IllegalArgumentException("Negative check chunk limit ( " + checkChunkLimit + " ).");
        }

        final TypeRegistry<Chunk> chunkRegistry = storage.getChunkRegistry();
        final TypeRegistry<Chunk.Id<?>> idRegistry = storage.getIdRegistry();
        final TypeRegistry<Mountain.Dust<?>> diffRegistry = storage.getDiffRegistry();

        this.connectReport = new ConnectReportDriver(network);

        final OperationAggregator<PeerAccessOperation, PeerAccessResult> peerAccessAggregator = new OperationAggregator<>();
        this.peerAccess = new PeerAccessDriver(sessionManager, network);
        this.peerAccessNonBlocking = new PeerAccessNonBlockingDriver(peerAccessAggregator, this.peerAccess, executor);
        this.peerAccessSelect = new PeerAccessSelectDriver(peerAccessAggregator, this.peerAccess);
        this.peerAccessMessage = new PeerAccessMessageDriver(network);
        this.peerAccessReply = new PeerAccessReplyDriver(network);

        final OperationAggregator<AddressAccessOperation, AddressAccessResult> addressAccessAggregator = new OperationAggregator<>();
        this.addressAccess = new AddressAccessDriver(sessionManager, network);
        this.addressAccessBlocking = new AddressAccessBlockingDriver(addressAccessAggregator, this.addressAccess);
        this.addressAccessNonBlocking = new AddressAccessNonBlockingDriver(addressAccessAggregator, this.addressAccess, executor);
        this.addressAccessMessage = new AddressAccessMessageDriver(network, this.addressAccessBlocking, executor);
        this.addressAccessReply = new AddressAccessReplyDriver(network);

        final OperationAggregator<GetChunkOperation, GetChunkResult> getChunkAggregator = new OperationAggregator<>();
        this.getChunk = new GetChunkDriver(network, storage, sessionManager, idRegistry);
        this.getChunkBlocking = new GetChunkBlockingDriver(getChunkAggregator, this.getChunk);
        this.getChunkNonBlocking = new GetChunkNonBlockingDriver(getChunkAggregator, this.getChunk, executor);
        this.getChunkMessage = new GetChunkMessageDriver(network, this.getChunkBlocking, chunkRegistry, executor);
        this.getChunkReply = new GetChunkReplyDriver();

        final OperationAggregator<UpdateChunkOperation, UpdateChunkResult> updateChunkAggregator = new OperationAggregator<>();
        this.updateChunk = new UpdateChunkDriver(network, storage, sessionManager, idRegistry);
        this.updateChunkBlocking = new UpdateChunkBlockingDriver(updateChunkAggregator, this.updateChunk);
        this.updateChunkNonBlocking = new UpdateChunkNonBlockingDriver(updateChunkAggregator, this.updateChunk, executor);
        this.updateChunkMessage = new UpdateChunkMessageDriver(network, this.updateChunkBlocking, diffRegistry, executor);
        this.updateChunkReply = new UpdateChunkReplyDriver();

        final OperationAggregator<AddChunkOperation, AddChunkResult> addChunkAggregator = new OperationAggregator<>();
        this.addChunk = new AddChunkDriver(network, storage, operationSink, sessionManager, chunkRegistry);
        this.addChunkBlocking = new AddChunkBlockingDriver(addChunkAggregator, this.addChunk);
        this.addChunkNonBlocking = new AddChunkNonBlockingDriver(addChunkAggregator, this.addChunk, executor);
        this.addChunkMessage = new AddChunkMessageDriver(network, this.addChunkBlocking, executor);
        this.addChunkReply = new AddChunkReplyDriver();

        final OperationAggregator<PatchChunkOperation<?>, PatchChunkResult> patchChunkAggregator = new OperationAggregator<>();
        this.patchChunk = new PatchChunkDriver(network, storage, operationSink, sessionManager, idRegistry);
        this.patchChunkBlocking = new PatchChunkBlockingDriver(patchChunkAggregator, this.patchChunk);
        this.patchChunkNonBlocking = new PatchChunkNonBlockingDriver(patchChunkAggregator, this.patchChunk, executor);
        this.patchChunkMessage = new PatchChunkMessageDriver(network, this.patchChunkBlocking, executor);
        this.patchChunkReply = new PatchChunkReplyDriver();

        final OperationAggregator<GetCacheOperation, GetCacheResult> getCacheAggregator = new OperationAggregator<>();
        this.getCache = new GetCacheDriver(network, storage, sessionManager, idRegistry);
        this.getCacheBlocking = new GetCacheBlockingDriver(getCacheAggregator, this.getCache);
        this.getCacheNonBlocking = new GetCacheNonBlockingDriver(getCacheAggregator, this.getCache, executor);
        this.getCacheMessage = new GetCacheMessageDriver(network, this.getCacheBlocking, chunkRegistry, idRegistry, executor);
        this.getCacheReply = new GetCacheReplyDriver(storage);

        final OperationAggregator<PatchOrAddAndGetCacheOperation, PatchOrAddAndGetCacheResult> patchOrAddAndGetCacheAggregator = new OperationAggregator<>();
        this.patchOrAddAndGetCache = new PatchOrAddAndGetCacheDriver(network, storage, operationSink, sessionManager, chunkRegistry);
        this.patchOrAddAndGetCacheBlocking = new PatchOrAddAndGetCacheBlockingDriver(patchOrAddAndGetCacheAggregator, this.patchOrAddAndGetCache);
        this.patchOrAddAndGetCacheNonBlocking = new PatchOrAddAndGetCacheNonBlockingDriver(patchOrAddAndGetCacheAggregator, this.patchOrAddAndGetCache,
                executor);
        this.patchOrAddAndGetCacheMessage = new PatchOrAddAndGetCacheMessageDriver(network, this.patchOrAddAndGetCacheBlocking, chunkRegistry, executor);
        this.patchOrAddAndGetCacheReply = new PatchOrAddAndGetCacheReplyDriver(storage);

        final OperationAggregator<GetOrUpdateCacheOperation, GetOrUpdateCacheResult> getOrUpdateCacheAggregator = new OperationAggregator<>();
        this.getOrUpdateCache = new GetOrUpdateCacheDriver(network, storage, sessionManager, idRegistry, this.getCacheBlocking,
                this.patchOrAddAndGetCacheBlocking);
        this.getOrUpdateCacheBlocking = new GetOrUpdateCacheBlockingDriver(getOrUpdateCacheAggregator, this.getOrUpdateCache);
        this.getOrUpdateCacheNonBlocking = new GetOrUpdateCacheNonBlockingDriver(getOrUpdateCacheAggregator, this.getOrUpdateCache, executor);
        this.getOrUpdateCacheMessage = new GetOrUpdateCacheMessageDriver(network, this.getOrUpdateCacheBlocking, chunkRegistry, idRegistry, executor);
        this.getOrUpdateCacheReply = new GetOrUpdateCacheReplyDriver(storage);

        final OperationAggregator<AddCacheOperation, AddCacheResult> addCacheAggregator = new OperationAggregator<>();
        this.addCache = new AddCacheDriver(network, storage, operationSink, sessionManager, chunkRegistry);
        this.addCacheBlocking = new AddCacheBlockingDriver(addCacheAggregator, this.addCache);
        this.addCacheNonBlocking = new AddCacheNonBlockingDriver(addCacheAggregator, this.addCache, executor);
        this.addCacheMessage = new AddCacheMessageDriver(network, this.addCacheBlocking, executor);
        this.addCacheReply = new AddCacheReplyDriver();

        final OperationAggregator<PatchAndGetOrUpdateCacheOperation<?>, PatchAndGetOrUpdateCacheResult> patchAndGetOrUpdateCacheAggregator = new OperationAggregator<>();
        this.patchAndGetOrUpdateCache = new PatchAndGetOrUpdateCacheDriver(network, storage, operationSink, sessionManager, idRegistry, this.getCacheBlocking,
                this.patchOrAddAndGetCacheBlocking);
        this.patchAndGetOrUpdateCacheBlocking = new PatchAndGetOrUpdateCacheBlockingDriver(patchAndGetOrUpdateCacheAggregator, this.patchAndGetOrUpdateCache);
        this.patchAndGetOrUpdateCacheNonBlocking = new PatchAndGetOrUpdateCacheNonBlockingDriver(patchAndGetOrUpdateCacheAggregator,
                this.patchAndGetOrUpdateCache, executor);
        this.patchAndGetOrUpdateCacheMessage = new PatchAndGetOrUpdateCacheMessageDriver(network, this.patchAndGetOrUpdateCacheBlocking, chunkRegistry,
                idRegistry, executor);
        this.patchAndGetOrUpdateCacheReply = new PatchAndGetOrUpdateCacheReplyDriver(storage);

        final OperationAggregator<CheckStockOperation, CheckStockResult> checkStockAggregator = new OperationAggregator<>();
        this.checkStock = new CheckStockDriver(network, storage, sessionManager, idRegistry, checkChunkLimit);
        this.checkStockBlocking = new CheckStockBlockingDriver(checkStockAggregator, this.checkStock);
        this.checkStockMessage = new CheckStockMessageDriver(network, storage, idRegistry, checkChunkLimit);
        this.checkStockReply = new CheckStockReplyDriver();

        final OperationAggregator<CheckDemandOperation, CheckDemandResult> checkDemandAggregator = new OperationAggregator<>();
        this.checkDemand = new CheckDemandDriver(network, storage, sessionManager, idRegistry, checkChunkLimit);
        this.checkDemandBlocking = new CheckDemandBlockingDriver(checkDemandAggregator, this.checkDemand);
        this.checkDemandMessage = new CheckDemandMessageDriver(network, storage, idRegistry, checkChunkLimit);
        this.checkDemandReply = new CheckDemandReplyDriver();

        final OperationAggregator<RecoveryOperation, RecoveryResult> recoveryAggregator = new OperationAggregator<>();
        this.recovery = new RecoveryDriver(network, storage, sessionManager, idRegistry);
        this.recoverySelect = new RecoverySelectDriver(recoveryAggregator, this.recovery);
        this.recoveryNonBlocking = new RecoveryNonBlockingDriver(recoveryAggregator, this.recovery, executor);
        this.recoveryMessage = new RecoveryMessageDriver(network, storage, chunkRegistry);
        this.recoveryReply = new RecoveryReplyDriver(network, storage);

        final OperationAggregator<BackupOperation, BackupResult> backupAggregator = new OperationAggregator<>();
        this.backup = new BackupDriver(network, storage, sessionManager, chunkRegistry);
        this.backupBlocking = new BackupBlockingDriver(backupAggregator, this.backup);
        this.backupSelect = new BackupSelectDriver(backupAggregator, this.backup);
        this.backupNonBlocking = new BackupNonBlockingDriver(backupAggregator, this.backup, executor);
        this.backupMessage = new BackupMessageDriver(network, storage, chunkRegistry);
        this.backupReply = new BackupReplyDriver(network, storage);

        final OperationAggregator<SimpleRecoveryOperation, SimpleRecoveryResult> simpleRecoveryAggregator = new OperationAggregator<>();
        this.simpleRecovery = new SimpleRecoveryDriver(network, storage, sessionManager, idRegistry);
        this.simpleRecoveryNonBlocking = new SimpleRecoveryNonBlockingDriver(simpleRecoveryAggregator, this.simpleRecovery, executor);
        this.simpleRecoveryMessage = new SimpleRecoveryMessageDriver(network, storage, chunkRegistry);
        this.simpleRecoveryReply = new SimpleRecoveryReplyDriver(network, storage);

        final OperationAggregator<CheckOneDemandOperation, CheckOneDemandResult> checkOneDemandAggregator = new OperationAggregator<>();
        this.checkOneDemand = new CheckOneDemandDriver(network, storage, sessionManager, idRegistry);
        this.checkOneDemandNonBlocking = new CheckOneDemandBlockingDriver(checkOneDemandAggregator, this.checkOneDemand);
        this.checkOneDemandMessage = new CheckOneDemandMessageDriver(network, storage, idRegistry);
        this.checkOneDemandReply = new CheckOneDemandReplyDriver();

        final OperationAggregator<BackupOneOperation, BackupOneResult> backupOneAggregator = new OperationAggregator<>();
        this.backupOne = new BackupOneDriver(network, this.checkOneDemandNonBlocking, this.backupBlocking);
        this.backupOneNonBlocking = new BackupOneNonBlockingDriver(backupOneAggregator, this.backupOne, executor);

        final OperationAggregator<FirstAccessOperation, FirstAccessResult> firstAccessAggregator = new OperationAggregator<>();
        this.firstAccess = new FirstAccessDriver(sessionManager, network);
        this.firstAccessSelect = new FirstAccessSelectDriver(firstAccessAggregator, this.firstAccess);

        this.communicationError = new CommunicationErrorDriver(network);
        this.contactError = new ContactErrorDriver(network);
        this.acceptanceError = new AcceptanceErrorDriver(network);
        this.unsentMail = new UnsentMailDriver(sessionManager);
        this.closePortWarning = new ClosePortWarningDriver(network);
    }

    PeerAccessDriver getPeerAccess() {
        return this.peerAccess;
    }

    PeerAccessSelectDriver getPeerAccessSelect() {
        return this.peerAccessSelect;
    }

    @Override
    public PeerAccessNonBlockingDriver getPeerAccessNonBlocking() {
        return this.peerAccessNonBlocking;
    }

    @Override
    public PeerAccessMessageDriver getPeerAccessMessage() {
        return this.peerAccessMessage;
    }

    @Override
    public PeerAccessReplyDriver getPeerAccessReply() {
        return this.peerAccessReply;
    }

    AddressAccessDriver getAddressAccess() {
        return this.addressAccess;
    }

    AddressAccessBlockingDriver getAddressAccessBlocking() {
        return this.addressAccessBlocking;
    }

    @Override
    public AddressAccessNonBlockingDriver getAddressAccessNonBlocking() {
        return this.addressAccessNonBlocking;
    }

    @Override
    public AddressAccessMessageDriver getAddressAccessMessage() {
        return this.addressAccessMessage;
    }

    @Override
    public AddressAccessReplyDriver getAddressAccessReply() {
        return this.addressAccessReply;
    }

    GetChunkDriver getGetChunk() {
        return this.getChunk;
    }

    GetChunkBlockingDriver getGetChunkBlocking() {
        return this.getChunkBlocking;
    }

    @Override
    public GetChunkNonBlockingDriver getGetChunkNonBlocking() {
        return this.getChunkNonBlocking;
    }

    @Override
    public GetChunkMessageDriver getGetChunkMessage() {
        return this.getChunkMessage;
    }

    @Override
    public GetChunkReplyDriver getGetChunkReply() {
        return this.getChunkReply;
    }

    UpdateChunkDriver getUpdateChunk() {
        return this.updateChunk;
    }

    UpdateChunkBlockingDriver getUpdateChunkBlocking() {
        return this.updateChunkBlocking;
    }

    @Override
    public UpdateChunkNonBlockingDriver getUpdateChunkNonBlocking() {
        return this.updateChunkNonBlocking;
    }

    @Override
    public UpdateChunkMessageDriver getUpdateChunkMessage() {
        return this.updateChunkMessage;
    }

    @Override
    public UpdateChunkReplyDriver getUpdateChunkReply() {
        return this.updateChunkReply;
    }

    AddChunkDriver getAddChunk() {
        return this.addChunk;
    }

    AddChunkBlockingDriver getAddChunkBlocking() {
        return this.addChunkBlocking;
    }

    @Override
    public AddChunkNonBlockingDriver getAddChunkNonBlocking() {
        return this.addChunkNonBlocking;
    }

    @Override
    public AddChunkMessageDriver getAddChunkMessage() {
        return this.addChunkMessage;
    }

    @Override
    public AddChunkReplyDriver getAddChunkReply() {
        return this.addChunkReply;
    }

    PatchChunkDriver getPatchChunk() {
        return this.patchChunk;
    }

    PatchChunkBlockingDriver getPatchChunkBlocking() {
        return this.patchChunkBlocking;
    }

    @Override
    public PatchChunkNonBlockingDriver getPatchChunkNonBlocking() {
        return this.patchChunkNonBlocking;
    }

    @Override
    public PatchChunkMessageDriver getPatchChunkMessage() {
        return this.patchChunkMessage;
    }

    @Override
    public PatchChunkReplyDriver getPatchChunkReply() {
        return this.patchChunkReply;
    }

    PatchAndGetOrUpdateCacheDriver getPatchAndGetOrUpdateCache() {
        return this.patchAndGetOrUpdateCache;
    }

    PatchAndGetOrUpdateCacheBlockingDriver getPatchAndGetOrUpdateCacheBlocking() {
        return this.patchAndGetOrUpdateCacheBlocking;
    }

    @Override
    public PatchAndGetOrUpdateCacheNonBlockingDriver getPatchAndGetOrUpdateCacheNonBlocking() {
        return this.patchAndGetOrUpdateCacheNonBlocking;
    }

    @Override
    public PatchAndGetOrUpdateCacheMessageDriver getPatchAndGetOrUpdateCacheMessage() {
        return this.patchAndGetOrUpdateCacheMessage;
    }

    @Override
    public PatchAndGetOrUpdateCacheReplyDriver getPatchAndGetOrUpdateCacheReply() {
        return this.patchAndGetOrUpdateCacheReply;
    }

    GetCacheDriver getGetCache() {
        return this.getCache;
    }

    GetCacheBlockingDriver getGetCacheBlocking() {
        return this.getCacheBlocking;
    }

    @Override
    public GetCacheNonBlockingDriver getGetCacheNonBlocking() {
        return this.getCacheNonBlocking;
    }

    @Override
    public GetCacheMessageDriver getGetCacheMessage() {
        return this.getCacheMessage;
    }

    @Override
    public GetCacheReplyDriver getGetCacheReply() {
        return this.getCacheReply;
    }

    GetOrUpdateCacheDriver getGetOrUpdateCache() {
        return this.getOrUpdateCache;
    }

    GetOrUpdateCacheBlockingDriver getGetOrUpdateCacheBlocking() {
        return this.getOrUpdateCacheBlocking;
    }

    @Override
    public GetOrUpdateCacheNonBlockingDriver getGetOrUpdateCacheNonBlocking() {
        return this.getOrUpdateCacheNonBlocking;
    }

    @Override
    public GetOrUpdateCacheMessageDriver getGetOrUpdateCacheMessage() {
        return this.getOrUpdateCacheMessage;
    }

    @Override
    public GetOrUpdateCacheReplyDriver getGetOrUpdateCacheReply() {
        return this.getOrUpdateCacheReply;
    }

    AddCacheDriver getAddCache() {
        return this.addCache;
    }

    AddCacheBlockingDriver getAddCacheBlocking() {
        return this.addCacheBlocking;
    }

    @Override
    public AddCacheNonBlockingDriver getAddCacheNonBlocking() {
        return this.addCacheNonBlocking;
    }

    @Override
    public AddCacheMessageDriver getAddCacheMessage() {
        return this.addCacheMessage;
    }

    @Override
    public AddCacheReplyDriver getAddCacheReply() {
        return this.addCacheReply;
    }

    PatchOrAddAndGetCacheDriver getPatchOrAddAndGetCache() {
        return this.patchOrAddAndGetCache;
    }

    PatchOrAddAndGetCacheBlockingDriver getPatchOrAddAndGetCacheBlocking() {
        return this.patchOrAddAndGetCacheBlocking;
    }

    @Override
    public PatchOrAddAndGetCacheNonBlockingDriver getPatchOrAddAndGetCacheNonBlocking() {
        return this.patchOrAddAndGetCacheNonBlocking;
    }

    @Override
    public PatchOrAddAndGetCacheMessageDriver getPatchOrAddAndGetCacheMessage() {
        return this.patchOrAddAndGetCacheMessage;
    }

    @Override
    public PatchOrAddAndGetCacheReplyDriver getPatchOrAddAndGetCacheReply() {
        return this.patchOrAddAndGetCacheReply;
    }

    CheckStockDriver getCheckStock() {
        return this.checkStock;
    }

    @Override
    public CheckStockBlockingDriver getCheckStockBlocking() {
        return this.checkStockBlocking;
    }

    @Override
    public CheckStockMessageDriver getCheckStockMessage() {
        return this.checkStockMessage;
    }

    @Override
    public CheckStockReplyDriver getCheckStockReply() {
        return this.checkStockReply;
    }

    CheckDemandDriver getCheckDemand() {
        return this.checkDemand;
    }

    @Override
    public CheckDemandBlockingDriver getCheckDemandBlocking() {
        return this.checkDemandBlocking;
    }

    @Override
    public CheckDemandMessageDriver getCheckDemandMessage() {
        return this.checkDemandMessage;
    }

    @Override
    public CheckDemandReplyDriver getCheckDemandReply() {
        return this.checkDemandReply;
    }

    RecoveryDriver getRecovery() {
        return this.recovery;
    }

    @Override
    public RecoverySelectDriver getRecoverySelect() {
        return this.recoverySelect;
    }

    RecoveryNonBlockingDriver getRecoveryNonBlocking() {
        return this.recoveryNonBlocking;
    }

    @Override
    public RecoveryMessageDriver getRecoveryMessage() {
        return this.recoveryMessage;
    }

    @Override
    public RecoveryReplyDriver getRecoveryReply() {
        return this.recoveryReply;
    }

    BackupDriver getBackup() {
        return this.backup;
    }

    @Override
    public BackupOneNonBlockingDriver getBackupOneNonBlocking() {
        return this.backupOneNonBlocking;
    }

    @Override
    public BackupSelectDriver getBackupSelect() {
        return this.backupSelect;
    }

    BackupNonBlockingDriver getBackupNonBlocking() {
        return this.backupNonBlocking;
    }

    @Override
    public BackupMessageDriver getBackupMessage() {
        return this.backupMessage;
    }

    @Override
    public BackupReplyDriver getBackupReply() {
        return this.backupReply;
    }

    SimpleRecoveryDriver getSimpleRecovery() {
        return this.simpleRecovery;
    }

    @Override
    public SimpleRecoveryNonBlockingDriver getSimpleRecoveryNonBlocking() {
        return this.simpleRecoveryNonBlocking;
    }

    @Override
    public SimpleRecoveryMessageDriver getSimpleRecoveryMessage() {
        return this.simpleRecoveryMessage;
    }

    @Override
    public SimpleRecoveryReplyDriver getSimpleRecoveryReply() {
        return this.simpleRecoveryReply;
    }

    @Override
    public CheckOneDemandReplyDriver getCheckOneDemandReply() {
        return this.checkOneDemandReply;
    }

    @Override
    public CheckOneDemandMessageDriver getCheckOneDemandMessage() {
        return this.checkOneDemandMessage;
    }

    FirstAccessSelectDriver getFirstAccessSelect() {
        return this.firstAccessSelect;
    }

    @Override
    public ConnectReportDriver getConnectReport() {
        return this.connectReport;
    }

    @Override
    public CommunicationErrorDriver getCommunicationError() {
        return this.communicationError;
    }

    @Override
    public ContactErrorDriver getContactError() {
        return this.contactError;
    }

    @Override
    public AcceptanceErrorDriver getAcceptanceError() {
        return this.acceptanceError;
    }

    @Override
    public UnsentMailDriver getUnsentMail() {
        return this.unsentMail;
    }

    @Override
    public ClosePortWarningDriver getClosePortWarning() {
        return this.closePortWarning;
    }

}
