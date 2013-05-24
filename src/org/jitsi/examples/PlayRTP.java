package org.jitsi.examples;

import java.io.*;
import java.util.*;

import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;

/**
 * Play RTP from a file.
 */
public class PlayRTP
{
    private MediaStream mediaStream;
    private String      filename;
    StreamConnector connector;

    private PlayRTP(String filename) throws Exception
    {
        this.filename = filename;
    }

    /**
     * Initializes the receipt of audio.
     *
     * @return <tt>true</tt> if this instance has been successfully initialized
     * @throws Exception
     *             if anything goes wrong while initializing this instance
     */
    private boolean initialize() throws Exception
    {
        /*
         * Prepare for the start of the transmission i.e. initialize the
         * MediaStream instances.
         */
        MediaService mediaService = LibJitsi.getMediaService();

        MediaDevice device = mediaService.getDefaultDevice(MediaType.AUDIO,
                                                           MediaUseCase.CALL);
        mediaStream = mediaService.createMediaStream(device);
        mediaStream.setDirection(MediaDirection.RECVONLY);

        // format
//        String encoding = "PCMU";
//        double clockRate = 8000;
//        byte dynamicRTPPayloadType = -1;

        String encoding = "SILK";
        double clockRate = 8000;
        byte dynamicRTPPayloadType = 96;
        MediaFormat format = mediaService.getFormatFactory().createMediaFormat(encoding, clockRate);

        /*
         * The MediaFormat instances which do not have a static RTP payload
         * type number association must be explicitly assigned a dynamic RTP
         * payload type number.
         */
        if (dynamicRTPPayloadType != -1)
        {
            mediaStream.addDynamicRTPPayloadType(dynamicRTPPayloadType, format);
        }

        mediaStream.setFormat(format);

        // connector
        connector = new PCapStreamConnector(filename);
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

    /**
     * The filename to play
     */
    private static final String     FILENAME = "--filename=";

    /**
     * The list of command-line arguments accepted as valid.
     */
    private static final String[][] ARGS     = {{FILENAME,
            "The filename to play."          },};

    public static void main(String[] args) throws Exception
    {
        // We need one parameter . For example,
        // ant run-example -Drun.example.name=PlayRTP -Drun.example.arg.line="--filename=test.pcap"
        if (args.length < 1)
        {
            prUsage();
        }
        else
        {
            Map<String, String> argMap = AVTransmit2
                    .parseCommandLineArgs(args);

            LibJitsi.start();
            try
            {
                PlayRTP playRTP = new PlayRTP(argMap.get(FILENAME));

                if (playRTP.initialize())
                {
                    while (playRTP.connector.getDataSocket().isConnected())
                    {
                        Thread.sleep(1000);
                    }
                    playRTP.close();
                    System.err.println("Exiting");
                }
                else
                {
                    System.err.println("Failed to initialize the sessions.");
                }
            }
            finally
            {
                LibJitsi.stop();
            }
        }
    }

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
