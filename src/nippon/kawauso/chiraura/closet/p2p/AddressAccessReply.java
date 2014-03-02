/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 論理位置の支配者確認への返答。
 * @author chirauraNoSakusha
 */
final class AddressAccessReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;

    private final AddressedPeer manager;
    private final List<AddressedPeer> peers;

    private AddressAccessReply(final boolean rejected, final boolean givenUp, final AddressedPeer manager, final List<AddressedPeer> peers) {
        this.rejected = rejected;
        this.givenUp = givenUp;
        this.manager = manager;
        this.peers = peers;
    }

    static AddressAccessReply newRejected() {
        return new AddressAccessReply(true, false, null, null);
    }

    static AddressAccessReply newGiveUp() {
        return new AddressAccessReply(false, true, null, null);
    }

    AddressAccessReply(final AddressedPeer manager, final List<AddressedPeer> peers) {
        this(false, false, manager, peers);
        if (peers == null) {
            throw new IllegalArgumentException("Null peers.");
        }
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    AddressedPeer getManager() {
        return this.manager;
    }

    List<AddressedPeer> getPeers() {
        return this.peers;
    }

    /*
     * 先頭バイトは、成功かつ支配者無しなら 1、成功かつ支配者ありなら 2、諦めたら 3、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.givenUp) {
            return 1;
        } else if (this.manager == null) {
            return 1 + BytesConversion.byteSize("ao", this.peers);
        } else {
            return 1 + BytesConversion.byteSize("oao", this.manager, this.peers);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.givenUp) {
            output.write(3);
            return 1;
        } else if (this.manager == null) {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "ao", this.peers);
        } else {
            output.write(2);
            return 1 + BytesConversion.toStream(output, "oao", this.manager, this.peers);
        }
    }

    static BytesConvertible.Parser<AddressAccessReply> getParser() {
        return new BytesConvertible.Parser<AddressAccessReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super AddressAccessReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final List<AddressedPeer> peers = new ArrayList<>();
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ao", peers, AddressedPeer.getParser());
                    output.add(new AddressAccessReply(null, peers));
                } else if (flag[0] == 2) {
                    final List<AddressedPeer> manager = new ArrayList<>(1);
                    final List<AddressedPeer> peers = new ArrayList<>();
                    size += BytesConversion.fromStream(input, maxByteSize - size, "oao", manager, AddressedPeer.getParser(), peers, AddressedPeer.getParser());
                    output.add(new AddressAccessReply(manager.get(0), peers));
                } else if (flag[0] == 3) {
                    output.add(newGiveUp());
                } else {
                    output.add(newRejected());
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = new StringBuilder(this.getClass().getSimpleName()).append('[');
        if (this.rejected) {
            buff.append("reject");
        } else if (this.givenUp) {
            buff.append("giveUp");
        } else {
            if (this.manager != null) {
                buff.append(this.manager).append(", ");
            }
            buff.append("numOfPeers=").append(this.peers.size());
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.givenUp ? 1231 : 1237);
        result = prime * result + ((this.manager == null) ? 0 : this.manager.hashCode());
        result = prime * result + ((this.peers == null) ? 0 : this.peers.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddressAccessReply)) {
            return false;
        }
        final AddressAccessReply other = (AddressAccessReply) obj;
        if (this.rejected != other.rejected || this.givenUp != other.givenUp) {
            return false;
        }
        if (this.manager == null) {
            if (other.manager != null) {
                return false;
            }
        } else if (!this.manager.equals(other.manager)) {
            return false;
        }
        if (this.peers == null) {
            if (other.peers != null) {
                return false;
            }
        } else if (!this.peers.equals(other.peers)) {
            return false;
        }
        return true;
    }

}
