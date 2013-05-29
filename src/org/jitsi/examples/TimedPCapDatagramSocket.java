package org.jitsi.examples;

import java.io.IOException;
import java.net.DatagramPacket;

public class TimedPCapDatagramSocket extends PCapDatagramSocket
{
    // An offset between the time the packet was written and the current nano time reading
    // It's the amount of time to add to the media time to make it system time.
    long mediaTimeOffset = Long.MIN_VALUE;

    public TimedPCapDatagramSocket(String xiFilename) throws IOException
    {
        super(xiFilename);
    }

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException
    {
        long timeStampNanoSeconds = super.receiveWithTimeStamp(p);
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
                    Thread.sleep(timeToSleepForJustMillis, timeToSleepForJustNanos);
                }
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private long convertMediaToSystemTime(long timeStampNanoSeconds) {
        return timeStampNanoSeconds + mediaTimeOffset;
    }

    private boolean hasMediaOffsetEverBeenSet() {
        return mediaTimeOffset != Long.MIN_VALUE;
    }
}
