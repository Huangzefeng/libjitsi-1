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
    private int ssrc;
    private int pt;

    public PCapStreamConnector(String filename, int ssrc)
    {
        this.filename = filename;
        this.ssrc = ssrc;
        this.pt = -1;
    }

    public PCapStreamConnector(String filename, int ssrc, int pt)
    {
        this(filename, ssrc);
        System.out.println(filename + " : " + ssrc + " : " + pt);
        this.pt = pt;
    }

    DatagramSocket sock = null;

    @Override
    public DatagramSocket getDataSocket()
    {
        if (sock == null)
        {
            try
            {
              if (pt != -1)
              {
                sock = new TimedPCapDatagramSocket(filename, ssrc, pt);
              }
              else
              {
                sock = new TimedPCapDatagramSocket(filename, ssrc);
              }
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
