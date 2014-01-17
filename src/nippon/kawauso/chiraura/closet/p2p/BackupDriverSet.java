/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * @author chirauraNoSakusha
 */
interface BackupDriverSet {

    CheckStockBlockingDriver getCheckStockBlocking();

    CheckDemandBlockingDriver getCheckDemandBlocking();

    RecoverySelectDriver getRecoverySelect();

    BackupSelectDriver getBackupSelect();

}
