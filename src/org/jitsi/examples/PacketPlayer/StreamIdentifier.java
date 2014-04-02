package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.Map.Entry;

public class StreamIdentifier
{
    private int ssrc;
    private int packetCount;
    private List<Byte> packetTypes;
    private InetAddress srcIp;
    private int srcPort;
    private InetAddress dstIp;
    private int dstPort;

    public StreamIdentifier(int ssrc)
    {
        this.ssrc = ssrc;
        srcPort = -1;
        srcIp = null;
        dstPort = -1;
        dstIp = null;
    }
    public StreamIdentifier(RtpData d)
    {
        ssrc = d.ssrc;
        srcPort = d.srcPort;
        srcIp = d.srcIp;
        dstPort = d.dstPort;
        dstIp = d.dstIp;
        packetCount = 1;
        packetTypes = new ArrayList<Byte>();
        packetTypes.add(d.pt);
    }

    public boolean matches(RtpData d)
    {
        if ((ssrc == d.ssrc || ssrc == -1) &&
            (srcIp == null || srcIp.equals(d.srcIp)) &&
            (dstIp == null || dstIp.equals(d.dstIp)) &&
            (srcPort == -1 || srcPort == d.srcPort) &&
            (dstPort == -1 || dstPort == d.dstPort)
           )
        {
            return true;
        }

        return false;
    }

    public boolean add(RtpData d)
    {
        if (matches(d))
        {
            packetCount ++;

            if (! packetTypes.contains(d.pt))
            {
                packetTypes.add(d.pt);
            }

            return true;
        }

        return false;
    }

    public String getSource()
    {
        return srcIp.toString() + ":" + srcPort;
    }

    public String getDestination()
    {
        return dstIp.toString() + ":" + dstPort;
    }

    public int getSSRC()
    {
        return ssrc;
    }

    public int getPacketCount()
    {
        return packetCount;
    }

    public List<Byte> getPacketTypes()
    {
        return new ArrayList<Byte>(packetTypes);
    }

    public static List<StreamIdentifier> fromFile(String fileLocation)
    {
        List<StreamIdentifier> streams =
                new ArrayList<StreamIdentifier>();

        Map<Integer, List<StreamIdentifier>> ssrcs =
                new HashMap<Integer, List<StreamIdentifier>>();

        PCapDatagramSocket socket = null;
        try
        {
            socket = new PCapDatagramSocket(fileLocation);

            while (!socket.isClosed())
            {
                RtpData d = new RtpData();

                socket.receive(d);

                if (d.payloadLength != 0 && d.protocol == 17)
                {
                    if (d.isRtcp())
                    {
                        continue;
                    }

                    List<StreamIdentifier> ssrcStreams = ssrcs.get(d.ssrc);

                    if (ssrcStreams != null)
                    {
                        boolean found = false;

                        for (StreamIdentifier s: ssrcStreams)
                        {
                            if (!found && s.add(d))
                            {
                                found = true;
                            }
                        }

                        if (! found)
                        {
                            StreamIdentifier s = new StreamIdentifier(d);
                            ssrcStreams.add(s);
                            streams.add(s);
                        }
                    }
                    else
                    {
                        List<StreamIdentifier> newSsrcStreams =
                                new ArrayList<StreamIdentifier>();

                        StreamIdentifier s = new StreamIdentifier(d);

                        newSsrcStreams.add(s);
                        ssrcs.put(d.ssrc, newSsrcStreams);

                        streams.add(s);
                    }
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

        return streams;
    }

    public static void main(String[] args)
    {
        long startTime = System.nanoTime();
        List<StreamIdentifier> streams = StreamIdentifier.fromFile("media0.pcap");
        System.out.println(String.format("Took %.3f seconds.",
            ((double) (System.nanoTime() - startTime)) / 1000000000));

        for (StreamIdentifier stream : streams)
        {
            System.out.println(String.format("0x%S = %s packets\n PTs: %s",
                Integer.toHexString(stream.getSSRC()),
                stream.getPacketCount(),
                stream.getPacketTypes()
            ));
        }

    }
}
