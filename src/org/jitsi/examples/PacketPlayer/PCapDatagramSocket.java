
package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.net.*;

/**
 * A datagram socket that gets its data from a pcap file.
 *
 * @author ted
 */
public class PCapDatagramSocket extends DatagramSocket
{
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

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException
    {
        receiveWithTimeStamp(p);
    }

    public synchronized long receiveWithTimeStamp(DatagramPacket p) throws IOException
    {
        byte[] intByteArray = new byte[4];

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
            return 0;
        }

        long timeStampNanoSeconds = combineTimestamps(timeStampSeconds, timeStampMicroSecondsOnly);
//        System.out.println(String.format("Timestamp from packet %s secs, %s nanos (%s))", timeStampNanoSeconds/1000000000, timeStampNanoSeconds % 1000000000, new Date(timeStampNanoSeconds / 1000000)));

        //  next 4 bytes are total packet length
        fis.read(intByteArray);
        int payloadLength = byteArrayToInt(intByteArray);
        // subtract 42 for the ethernet, IP and UDP headers.
        payloadLength -= 42;
        p.setLength(payloadLength);

        //  next 4 bytes are just the last four repeated
        fis.skip(4);

        // Skip the ethernet header. The easiest way of doing this is to sniff
        // the 'ethernet type' field, which will end with 0x0800.
        fis.skip(2); // Ethernet header is not a whole number of words.
        boolean isInEthHeader = true;
        while (isInEthHeader)
        {
            fis.read(intByteArray);
            if (intByteArray[2] == 8 && intByteArray[3] == 0)
            {
                isInEthHeader = false;
            }
        }

        //  skip 20 bytes for IP header
        fis.skip(20);

        //  skip UDP header - 8 bytes
        fis.skip(8);

        //  Next read the payload - which is total packet length - 42 bytes long.
        fis.read(p.getData(), 0, payloadLength);

        //Set a random port - I don't know if this is required.
        p.setPort(5000);

        return timeStampNanoSeconds;
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
