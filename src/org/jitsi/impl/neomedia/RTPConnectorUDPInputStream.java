/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia;

import java.io.*;
import java.net.*;

import org.ice4j.socket.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.packetlogging.*;

/**
 * RTPConnectorInputStream implementation for UDP protocol.
 *
 * @author Sebastien Vincent
 */
public class RTPConnectorUDPInputStream
    extends RTPConnectorInputStream
{
    /**
     * UDP socket used to receive data.
     */
    private final DatagramSocket socket;

    /**
     * Receive size configured flag.
     */
    private boolean receivedSizeFlag = false;

    /**
     * Initializes a new <tt>RTPConnectorInputStream</tt> which is to receive
     * packet data from a specific UDP socket.
     *
     * @param socket the UDP socket the new instance is to receive data from
     */
    public RTPConnectorUDPInputStream(DatagramSocket socket)
    {
        this.socket = socket;

        if(socket != null)
        {
            closed = false;
            receiverThread = new Thread(this);
            receiverThread.start();
        }
    }

    /**
     * Close this stream, stops the worker thread.
     */
    @Override
    public synchronized void close()
    {
        closed = true;
        if(socket != null)
        {
            socket.close();
            LibJitsi.getPacketLoggingService().dumpMediaBuffer();
        }
    }

    /**
     * Log the packet.
     *
     * @param p packet to log
     */
    protected void doLogPacket(DatagramPacket p)
    {
        if(socket.getLocalAddress() == null)
            return;

        // Do not log the packet if this one has been processed (and already
        // logged) by the ice4j stack.
        if(socket instanceof MultiplexingDatagramSocket)
            return;

        PacketLoggingService packetLogging = LibJitsi.getPacketLoggingService();

        //Create a RawPacket to make it easier to extract just the header
        RawPacket convertedPacket = new RawPacket(p.getData(),
                                                  p.getOffset(),
                                                  p.getLength());

        if (packetLogging != null)
        {
            packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.RTP,
                    p.getAddress().getAddress(),
                    p.getPort(),
                    socket.getLocalAddress().getAddress(),
                    socket.getLocalPort(),
                    PacketLoggingService.TransportName.UDP,
                    false,
                    convertedPacket.readRegion(convertedPacket.getOffset(),
                                             convertedPacket.getHeaderLength()),
                    convertedPacket.getOffset(),
                    convertedPacket.getHeaderLength());
            
            // And log to the media buffer
             byte[] data = new byte[p.getLength()];
             System.arraycopy(p.getData(), p.getOffset(), data, 0, p.getLength());
             packetLogging.mediaBuffer(data, System.currentTimeMillis());
        }
    }

    /**
     * Receive packet.
     *
     * @param p packet for receiving
     * @throws IOException if something goes wrong during receiving
     */
    protected void receivePacket(DatagramPacket p)
        throws IOException
    {
        if(!receivedSizeFlag)
        {
            receivedSizeFlag = true;

            try
            {
                socket.setReceiveBufferSize(65535);
            }
            catch(Throwable t)
            {
            }
        }
        socket.receive(p);
    }
}
