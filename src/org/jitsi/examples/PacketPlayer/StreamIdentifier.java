package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.Map.Entry;

public class StreamIdentifier
{
    public StreamIdentifier(String fileLocation)
    {
        readFile(fileLocation);
    }

    public static void main(String[] args)
    {
        long startTime = System.nanoTime();
        StreamIdentifier streamIdentifier = new StreamIdentifier("media0.pcap");
        System.out.println(String.format("Took %.3f seconds.",
            ((double) (System.nanoTime() - startTime)) / 1000000000));

        for (Entry<Integer, Integer> entry : streamIdentifier.ssrcPacketCounts
            .entrySet())
        {
            System.out.println(String.format("0x%S = %s packets\n PTs: %s",
                Integer.toHexString(entry.getKey()), entry.getValue(),
                streamIdentifier.ssrcPayloadTypes.get(entry.getKey())));
        }

    }

    public Map<Integer, Integer> ssrcPacketCounts =
        new HashMap<Integer, Integer>();

    public Map<Integer, List<Byte>> ssrcPayloadTypes =
        new HashMap<Integer, List<Byte>>();

    private void readFile(String fileLocation)
    {
        PCapDatagramSocket socket = null;
        try
        {
            socket = new PCapDatagramSocket(fileLocation);

            while (!socket.isClosed())
            {
                DatagramPacket p = new DatagramPacket(new byte[2048], 0);
                socket.receive(p);

                if (p.getLength() != 0)
                {
                    ByteBuffer wrapped = ByteBuffer.wrap(p.getData());

                    int SSRC = readSSRC(wrapped);
                    byte payloadType = readPayloadType(wrapped);

                    if (isRTCPPayload(payloadType))
                    {
                        continue;
                    }

                    ensureSSRCCountsInit(SSRC);
                    recordSSRCCount(SSRC);

                    ensureSSRCPayloadTypesInit(SSRC);
                    recordSSRCPayloadType(SSRC, payloadType);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            socket.close();
        }
    }

    public static int readSSRC(ByteBuffer wrapped)
    {
        int SSRC = wrapped.getInt(8);
        return SSRC;
    }

    private byte readPayloadType(ByteBuffer wrapped)
    {
        // This include the marker bit so mask the first bit
        byte payloadType = wrapped.get(1);
        payloadType = (byte) (payloadType & ((byte) 0x7f));
        return payloadType;
    }

    private void recordSSRCPayloadType(int SSRC, byte payloadType)
    {
        if (!ssrcPayloadTypes.get(SSRC).contains(payloadType))
        {
            List<Byte> payloadList = ssrcPayloadTypes.get(SSRC);
            payloadList.add(payloadType);
            ssrcPayloadTypes.put(SSRC, payloadList);
        }
    }

    private void recordSSRCCount(int SSRC)
    {
        ssrcPacketCounts.put(SSRC, ssrcPacketCounts.get(SSRC) + 1);
    }

    private void ensureSSRCPayloadTypesInit(int SSRC)
    {
        if (!ssrcPayloadTypes.containsKey(SSRC))
        {
            ssrcPayloadTypes.put(SSRC, new LinkedList<Byte>());
        }
    }

    private void ensureSSRCCountsInit(int SSRC)
    {
        if (!ssrcPacketCounts.containsKey(SSRC))
        {
            ssrcPacketCounts.put(SSRC, 0);
        }
    }

    private boolean isRTCPPayload(byte payloadType)
    {
        return payloadType >= 72 && payloadType <= 76;
    }
}
