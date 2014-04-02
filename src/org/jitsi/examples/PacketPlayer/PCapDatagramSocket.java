
package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import org.jitsi.util.Logger;

/**
 * A datagram socket that gets its data from a pcap file.
 *
 * @author ted
 */
public class PCapDatagramSocket extends DatagramSocket
{
    private static Logger logger = Logger.getLogger(PCapDatagramSocket.class);
    private boolean connected = true;
    private final FileInputStream fis;

    public PCapDatagramSocket(String filename) throws IOException
    {
        super();
        fis = new FileInputStream(filename);

        //Skip the global header
        fis.skip(24);
    }

    private static int byteArrayToInt(byte[] b)
    {
        return b[0] & 0xFF | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 |
               (b[3] & 0xFF) << 24;
    }

    private static int byteArrayToShort(byte[] b)
    {
        return (0 | (b[0] & 0xFF | (b[1] & 0xFF) << 8));
    }

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException
    {
        receiveWithTimeStamp(p);
    }

    public synchronized void receive(RtpData d) throws IOException
    {
        byte[] intByteArray = new byte[4];
        byte[] shortByteArray = new byte[2];
        byte[] byteByteArray = new byte[1];

        //  First 4 bytes are ts in seconds
        //  Next 4 bytes are ts in nano seconds
        int readResult = fis.read(intByteArray);
        int timeStampSeconds = byteArrayToInt(intByteArray);

        fis.read(intByteArray);
        int timeStampMicroSecondsOnly = byteArrayToInt(intByteArray);

        if (readResult == -1)
        {
            //End of file has been reached - assumes that EOF is aligned with packets
            close();
            d.payloadLength = 0;
            return;
        }

        d.timestamp = combineTimestamps(timeStampSeconds, timeStampMicroSecondsOnly);

        //  next 4 bytes are total packet length
        fis.read(intByteArray, 0, 4);
        d.payloadLength = byteArrayToInt(intByteArray);

        //  next 4 bytes are just the last four repeated
        fis.skip(4);

        //  skip 14 bytes for ethernet header
        fis.skip(14);
        d.payloadLength -= 14;

        // 20 bytes for IP header
        // 4 for version, lengths, dscp, ecn, identification, flags
        // 4 for identification, flags, fragment offset
        // 1 for TTL
        // 1 for Protocol
        // 2 for a checksum
        // 4 for Source IP
        // 4 for Destination IP
        fis.skip(9);
        fis.read(byteByteArray, 0, 1);
        d.protocol = byteByteArray[0] & 0xFF;

        fis.skip(2);
        fis.read(intByteArray, 0, 4);
        d.srcIp = InetAddress.getByAddress(intByteArray);
        fis.read(intByteArray, 0, 4);
        d.dstIp = InetAddress.getByAddress(intByteArray);
        d.payloadLength -= 20;

        // If this isn't UDP continue
        if (d.protocol != 17)
        {
            fis.skip(d.payloadLength);
            return;
        }

        // UDP header - 8 bytes
        // 2 for Source Port
        // 2 for Destination Port
        // 4 for Length and Checksum
        fis.read(shortByteArray, 0, 2);
        d.srcPort = byteArrayToShort(shortByteArray);
        fis.read(shortByteArray, 0, 2);
        d.dstPort = byteArrayToShort(shortByteArray);
        fis.skip(4);
        d.payloadLength -= 8;

        if (d.payloadLength != 0)
        {
            //  Next read the payload
            fis.read(d.data, d.offset, d.payloadLength);

            ByteBuffer b = ByteBuffer.wrap(d.data);
            d.ssrc = b.getInt(8);
            d.pt = b.get(1);
        }
    }

    public synchronized long receiveWithTimeStamp(DatagramPacket p) throws IOException
    {
        RtpData rtp = new RtpData(p);

        // Set a random port - I don't know if this is required.
        p.setPort(rtp.dstPort);
        p.setLength(rtp.payloadLength);

        return rtp.timestamp;
    }

	@Override
    public void close()
    {
        super.close();
        connected = false;

        try
        {
            fis.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean isConnected()
    {
        return connected;
    }
    /**
     * @return a timestamp in nanoseconds
     */
    private long combineTimestamps(int timeStampSeconds, int timeStampMicroSecondsOnly)
    {
        return (timeStampSeconds * 1000000000L) + (timeStampMicroSecondsOnly * 1000L);
    }

    // Test method. Just read a single packet...
    public static void main(String[] args) throws Exception
    {
        PCapDatagramSocket sock = new PCapDatagramSocket("media.pcap");
        DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
        for (int ii = 0; ii<10;ii++)
        {
        sock.receive(p);
//        System.out.println("Got packet");
//        System.out.println(bytesToHex(p.getData(), p.getLength()));
//        System.out.println(String.format("Length=%s\nOffset=%s\nPort=%s\n",
//                                         p.getLength(),
//                                         p.getOffset(),
//                                         p.getPort()));
        }
        sock.close();
    }

    private static String bytesToHex(byte[] bytes, int length)
    {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < length; j++)
        {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
