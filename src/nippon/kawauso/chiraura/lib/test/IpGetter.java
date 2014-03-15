package nippon.kawauso.chiraura.lib.test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author chirauraNoSakusha
 */
public final class IpGetter {

    // インスタンス化防止。
    private IpGetter() {}

    /**
     * 利用可能でループバックでない IPv4 アドレスを返す。
     * @return 利用可能でループバックでない IPv4 アドレス。
     *         無い場合は null
     */
    public static InetAddress getNotLoopbackV4() {
        try {
            final Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) {
                return null;
            }
            while (ifaces.hasMoreElements()) {
                final NetworkInterface iface = ifaces.nextElement();
                if (!iface.isLoopback()) {
                    final Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        final InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }
            return null;
        } catch (final SocketException e) {
            return null;
        }
    }

    /**
     * IPv4 環境で使用できす自身のアドレスを表示する。
     * @param args 使用しない
     */
    public static void main(final String[] args) {
        final InetAddress address = getNotLoopbackV4();
        if (address != null) {
            System.out.println(getNotLoopbackV4().getHostAddress());
        } else {
            System.out.println("localhost");
        }
    }

}
