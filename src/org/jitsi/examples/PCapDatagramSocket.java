
package org.jitsi.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * A datagram socket that gets its data from a pcap file.
 *
 * @author ted
 */
public class PCapDatagramSocket extends DatagramSocket
{
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
        byte[] intByteArray = new byte[4];

        //  First 4 bytes are ts
        //  Next 4 bytes are ms ts
        //  skip timestamp for now...
        fis.skip(8);

        //  next 4 bytes are total packet length
        fis.read(intByteArray);
        int payloadLength = byteArrayToInt(intByteArray);
        // subtract 42 for the ethernet, IP and UDP headers.
        payloadLength -= 42;
        p.setLength(payloadLength);

        //  next 4 bytes are just the last four repeated
        fis.skip(4);

        //  skip 14 bytes for ethernet header
        fis.skip(14);

        //  skip 20 bytes for IP header
        fis.skip(20);

        //  skip UDP header - 8 bytes
        fis.skip(8);

        //  Next read the payload - which is total packet length - 42 bytes long.
        fis.read(p.getData(), 0, payloadLength);

        //Set a random port - I don't know if this is required.
        p.setPort(5000);

        try
        {
            // Since we're not reading the timestamps, we need to pace
            // ourselves somehow. Assume packetization interval of 20ms
            // and just sleep. This means we will certainly play a
            // little slow...
            Thread.sleep(20);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        super.close();

        try
        {
            fis.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Test method. Just read a single packet...
    public static void main(String[] args) throws Exception
    {
        PCapDatagramSocket sock = new PCapDatagramSocket("media0.pcap");
        DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
        sock.receive(p);
        System.out.println("Got packet");
        System.out.println(bytesToHex(p.getData(), p.getLength()));
        System.out.println(String.format("Length=%s\nOffset=%s\nPort=%s\n",
                                         p.getLength(),
                                         p.getOffset(),
                                         p.getPort()));
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
