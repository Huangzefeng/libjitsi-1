package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.net.*;
import java.nio.*;

public class TimedPCapDatagramSocket extends PCapDatagramSocket
{
    // An offset between the time the packet was written and the current nano time reading
    // It's the amount of time to add to the media time to make it system time.
    long mediaTimeOffset = Long.MIN_VALUE;
    int ssrc;

    public TimedPCapDatagramSocket(String xiFilename, int ssrc) throws IOException
    {
        super(xiFilename);
        this.ssrc = ssrc;
    }

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException
    {
        long timeStampNanoSeconds;
        do
        {
            timeStampNanoSeconds = super.receiveWithTimeStamp(p);
        }
        while (!isInterestingSSRC(p));

        try
        {
            if (! hasMediaOffsetEverBeenSet())
            {
                // Set the media time offset to be the difference between the
                // current time and the time the packet was captured.
                mediaTimeOffset = System.nanoTime() - timeStampNanoSeconds;
            }
            else
            {
                // Don't return until we actually would have read the packet.
                // Sleeping is more accurate than using a timer.
                long currentTime = System.nanoTime();
                long timeToPlayPacket = convertMediaToSystemTime(timeStampNanoSeconds);

                long timeToSleepFor = timeToPlayPacket - currentTime;
                long timeToSleepForJustMillis = timeToSleepFor / 1000000;
                int timeToSleepForJustNanos = (int) (timeToSleepFor % 1000000);

                if (timeToSleepFor > 0)
                {
                    if (timeToSleepForJustMillis > 30)
                    {
                        System.out.println("Warning: Been asked to sleep for a long time " + timeToSleepForJustMillis + "ms.");
                    }

                    Thread.sleep(timeToSleepForJustMillis, timeToSleepForJustNanos);
                }
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private boolean isInterestingSSRC(DatagramPacket p)
    {
        if (ssrc == -1)
        {
            return true;
        }
        int readSSRC = StreamIdentifier.readSSRC(ByteBuffer.wrap(p.getData()));
        return readSSRC == ssrc;
    }

    private long convertMediaToSystemTime(long timeStampNanoSeconds)
    {
        return timeStampNanoSeconds + mediaTimeOffset;
    }

    private boolean hasMediaOffsetEverBeenSet()
    {
        return mediaTimeOffset != Long.MIN_VALUE;
    }
}
