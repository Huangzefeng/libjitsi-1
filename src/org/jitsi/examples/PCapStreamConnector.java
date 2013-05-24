package org.jitsi.examples;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

import org.jitsi.service.neomedia.StreamConnector;

/**
 * Support UDP only.
 * RTP only - no RTCP
 */
public class PCapStreamConnector implements StreamConnector {

    private String filename;
	public PCapStreamConnector(String filename)
    {
	    this.filename = filename;
    }

    @Override
	public DatagramSocket getDataSocket()
	{
		DatagramSocket sock = null;
		try {
			sock = new TimedPCapDatagramSocket(filename);
		} catch (SocketException e) {
			e.printStackTrace();
		}
        catch (IOException e)
        {
            e.printStackTrace();
        }
		return sock;
	}

	@Override
	public Protocol getProtocol() {
		return Protocol.UDP;
	}

	@Override
	public void close() {}

	@Override
	public void started() {}

	@Override
	public void stopped() {}

	@Override
	public DatagramSocket getControlSocket() {return null;}

	@Override
	public Socket getDataTCPSocket() {return null;}

	@Override
	public Socket getControlTCPSocket() {return null;}
}
