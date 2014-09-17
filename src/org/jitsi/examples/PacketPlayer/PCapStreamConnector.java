package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.net.*;

import org.jitsi.service.neomedia.*;

/**
 * Support UDP only. RTP only - no RTCP
 */
public class PCapStreamConnector
    implements StreamConnector
{

    private String filename;
    private StreamIdentifier stream;

    public PCapStreamConnector(String filename, StreamIdentifier stream)
    {
        this.filename = filename;
        this.stream = stream;
    }

    DatagramSocket sock = null;

    @Override
    public DatagramSocket getDataSocket()
    {
        if (sock == null)
        {
            try
            {
                sock = new TimedPCapDatagramSocket(filename, stream);
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            catch (FileNotFoundException e)
            {
                e.getMessage();
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return sock;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.UDP;
    }

    @Override
    public void close()
    {
    }

    @Override
    public void started()
    {
    }

    @Override
    public void stopped()
    {
    }

    @Override
    public DatagramSocket getControlSocket()
    {
        return null;
    }

    @Override
    public Socket getDataTCPSocket()
    {
        return null;
    }

    @Override
    public Socket getControlTCPSocket()
    {
        return null;
    }
}
