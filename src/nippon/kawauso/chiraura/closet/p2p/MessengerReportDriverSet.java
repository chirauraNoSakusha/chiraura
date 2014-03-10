/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * @author chirauraNoSakusha
 */
interface MessengerReportDriverSet {

    ConnectReportDriver getConnectReport();

    CommunicationErrorDriver getCommunicationError();

    ContactErrorDriver getContactError();

    AcceptanceErrorDriver getAcceptanceError();

    UnsentMailDriver getUnsentMail();

    ClosePortWarningDriver getClosePortWarning();

}
