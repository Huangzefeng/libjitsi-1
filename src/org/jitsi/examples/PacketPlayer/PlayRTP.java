package org.jitsi.examples.PacketPlayer;

import java.io.*;
import java.util.*;

import org.jitsi.examples.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;

/**
 * Play RTP from a file.
 */
public class PlayRTP
{
    private static boolean started;

    private MediaStream mediaStream;

    private StreamConnector connector;
    /**
     * Initializes the receipt of audio.
     *
     * @return <tt>true</tt> if this instance has been successfully initialized
     * @throws Exception if anything goes wrong while initializing this instance
     */
    private boolean playMedia(String filename, String encoding,
        double clockRate, byte dynamicRTPPayloadType, int ssrc) throws Exception
    {
        /*
         * Prepare for the start of the transmission i.e. initialize the
         * MediaStream instances.
         */
        MediaService mediaService = LibJitsi.getMediaService();
        MediaDevice device =
            mediaService.getDefaultDevice(MediaType.AUDIO, MediaUseCase.CALL);
        mediaStream = mediaService.createMediaStream(device);
        mediaStream.setDirection(MediaDirection.RECVONLY);

        MediaFormat format =
            mediaService.getFormatFactory().createMediaFormat(encoding,
                clockRate);

        /*
         * The MediaFormat instances which do not have a static RTP payload type
         * number association must be explicitly assigned a dynamic RTP payload
         * type number.
         */
        if (dynamicRTPPayloadType != -1)
        {
            mediaStream.addDynamicRTPPayloadType(dynamicRTPPayloadType, format);
        }

        mediaStream.setFormat(format);


        // connector
        connector = new PCapStreamConnector(filename, ssrc);
        mediaStream.setConnector(connector);
        mediaStream.start();

        return true;
    }

    /**
     * Close the <tt>MediaStream</tt>s.
     */
    private void close()
    {
        if (mediaStream != null)
        {
            try
            {
                mediaStream.stop();
            }
            finally
            {
                mediaStream.close();
                mediaStream = null;
            }
        }
    }

    private static void initIfRequired()
    {
        if (!started)
            LibJitsi.start();
    }

    private static void shutdown()
    {
        LibJitsi.stop();
    }

    /*
     * Blocking
     */
    public void playFile(String filename, String encoding, double clockRate,
        byte dynamicRTPPayloadType, int ssrc)
    {
        close();
        initIfRequired();

        try
        {
            playMedia(filename, encoding, clockRate, dynamicRTPPayloadType, ssrc);
            while (connector.getDataSocket().isConnected())
            {
                Thread.sleep(100);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            close();
        }
    }

    public static void main(String[] args) throws Exception
    {
        // We need one parameter . For example,
        // ant run-example -Drun.example.name=PlayRTP
        // -Drun.example.arg.line="--filename=test.pcap"
        if (args.length < 1)
        {
            prUsage();
        }
        else
        {
            Map<String, String> argMap = AVTransmit2.parseCommandLineArgs(args);

            try
            {
                PlayRTP playRTP = new PlayRTP();
                playRTP.playFile(argMap.get(FILENAME), "SILK", 8000,
                    (byte) 96, -1);
            }
            finally
            {
                shutdown();
            }
            System.exit(0);
        }
    }

    /**
     * The filename to play
     */
    private static final String     FILENAME = "--filename=";

    /**
     * The list of command-line arguments accepted as valid.
     */
    private static final String[][] ARGS     = {{FILENAME,
            "The filename to play."          },};


    /**
     * Outputs human-readable description about the usage.
     */
    private static void prUsage()
    {
        PrintStream err = System.err;

        err.println("Usage: " + PlayRTP.class.getName() + " <args>");
        err.println("Valid args:");
        for (String[] arg : ARGS)
            err.println("  " + arg[0] + " " + arg[1]);
    }
}
