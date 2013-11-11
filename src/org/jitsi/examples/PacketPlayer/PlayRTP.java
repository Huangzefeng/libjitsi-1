package org.jitsi.examples.PacketPlayer;

import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.examples.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.event.SimpleAudioLevelListener;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.util.event.*;
import org.jitsi.util.swing.*;

class VideoFrame extends JFrame
{
    private static final long serialVersionUID = 1L;
    private VideoContainer vc;

    VideoFrame(Component video)
    {
        super("Video");
        vc = new VideoContainer(video, false);
        this.add(vc);
        this.setSize(1000, 1000);
    }
}

/**
 * Play RTP from a file.
 */
public class PlayRTP
{
    private static boolean started;

    private MediaStream mediaStream;
    
    //private int maxLocalAudioLevel = SimpleAudioLevelListener.MIN_LEVEL; // Default to lowest possible
    final ValueBox<Integer> maxStreamAudioLevel = new ValueBox<Integer>(SimpleAudioLevelListener.MIN_LEVEL); // Default to lowest possible

    boolean foundVideo;

    public PlayRTP()
    {
        initIfRequired();
    }

    private synchronized void checkForVideo()
    {
        if (!foundVideo)
        {
            System.out.println("Still finding video");

            List<Component> videos = ((VideoMediaStream) mediaStream).getVisualComponents();
            if (! videos.isEmpty())
            {
                System.out.println("Found Video!");

                foundVideo = true;
                final Component video = videos.get(0);
                SwingUtilities.invokeLater(new Runnable(){

                    @Override
                    public void run() {
                        System.out.println("Displaying Video!");

                        VideoFrame videoFrame = new VideoFrame(video);
                        videoFrame.setVisible(true);
                    }
                });
            }
        }
    }

    //private StreamConnector connector;
    /**
     * Initializes the receipt of audio.
     *
     * @return The stream connector that can be used to check if the wotsit is still doobering.
     * @throws Exception if anything goes wrong while initializing this instance
     */
    private StreamConnector playMedia(String filename, MediaFormat initialFormat,
        List<Byte> dynamicRTPPayloadTypes,
        MediaFormat dynamicFormat,int ssrc) throws Exception
    {
        /*
         * Prepare for the start of the transmission i.e. initialize the
         * MediaStream instances.
         */
        MediaService mediaService = LibJitsi.getMediaService();
        MediaDevice device = mediaService.getDefaultDevice(
            initialFormat.getMediaType(), MediaUseCase.CALL);
        mediaStream = mediaService.createMediaStream(device);
        mediaStream.setDirection(MediaDirection.RECVONLY);

        if (initialFormat.getMediaType().equals(MediaType.VIDEO))
        {
            foundVideo = false;

            ((VideoMediaStream) mediaStream).addVideoListener(new VideoListener(){

                @Override
                public void videoAdded(VideoEvent event)
                {
                    checkForVideo();
                }

                @Override
                public void videoRemoved(VideoEvent event)
                {
                    checkForVideo();
                }

                @Override
                public void videoUpdate(VideoEvent event)
                {
                    checkForVideo();
                }
            });

            PropertyChangeListener pchange = new PropertyChangeListener(){
                @Override
                public synchronized void propertyChange(PropertyChangeEvent evt)
                {
                    System.out.println("Change event: " + evt);
                    checkForVideo();
                }
            };

            mediaStream.addPropertyChangeListener(pchange);
            pchange.propertyChange(null);
        }


        /*
         * The MediaFormat instances which do not have a static RTP payload type
         * number association must be explicitly assigned a dynamic RTP payload
         * type number.
         */
        for (byte dynamicPT : dynamicRTPPayloadTypes)
        {
            mediaStream.addDynamicRTPPayloadType(dynamicPT, dynamicFormat);
        }

        mediaStream.setFormat(initialFormat);

        // connector
        final StreamConnector connector = new PCapStreamConnector(filename, ssrc);
        mediaStream.setConnector(connector);
        final MediaStream fMediaStream = mediaStream;// hahahahahha
        
        // New:
        if (mediaStream instanceof AudioMediaStream) {
        	System.out.println("This is an AudioMediaStream");
        	AudioMediaStream audioMediaStream = (AudioMediaStream)mediaStream;
        	/*audioMediaStream.setLocalUserAudioLevelListener(new SimpleAudioLevelListener(){
				@Override
				public void audioLevelChanged(int level) {
					// TODO Auto-generated method stub
					System.out.println("@@@ Local Audio level changed to: " + level);
					
					if (level > maxLocalAudioLevel) {
						maxLocalAudioLevel = level;
					}
					
					if (level > SimpleAudioLevelListener.MIN_LEVEL) {
						// We've got some audio, so skip and move onto the next one
						System.out.println("Got some local audio - move on to next iteration");
						//mediaStream.stop();
					}
				}});*/
        	audioMediaStream.setStreamAudioLevelListener(new SimpleAudioLevelListener(){
				@Override
				public void audioLevelChanged(int level) {
					// TODO Auto-generated method stub
					if (level > maxStreamAudioLevel.get()) {
						maxStreamAudioLevel.set(level);
						System.out.println("-- Max Stream Audio level increased to: " + level);
					}
					if (level > SimpleAudioLevelListener.MIN_LEVEL){
						System.out.println("Got some audio - this one works, so move on to the next.");
						connector.getDataSocket().close();
						try {
							Thread.sleep(110); // Give the socket time to close so we don't get a bunch of new callbacks
						} catch (InterruptedException e) { e.printStackTrace(); }
						System.out.println("...end of sleep");
					}
        	}});
        }
        
        mediaStream.start();

        return connector;
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

	private static void makeLog(String str)
	{
		System.out.println(str);
	}
	
    /*
     * Blocking
     */
    public void playFile(String filename, MediaFormat initialFormat,
        List<Byte> dynamicRTPPayloadTypes, MediaFormat dynamicFormat, int ssrc, int attempts)
    {
    	int ix;
        for (ix = 0; ix < attempts; ix++)
        {
            // Now play the stream
        	makeLog("Play file, attempt: " + (ix+1)); 
        	maxStreamAudioLevel.set(SimpleAudioLevelListener.MIN_LEVEL);
        	
            close();
            initIfRequired();
            
            try
            {
                StreamConnector connector = playMedia(filename, initialFormat, dynamicRTPPayloadTypes,
                    dynamicFormat, ssrc);
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
            	int audioLevel = maxStreamAudioLevel.get(); 
            	System.out.println("Max local audio level recorded: " + audioLevel); // And do something clever if it was too low??
            	if (audioLevel <= SimpleAudioLevelListener.MIN_LEVEL) {
            		System.out.print("\n!!  No audio on this loop !!\n\n");
            		break;
            	}
                close();
                shutdown();
            }
        }
        makeLog("Finished - stopped after attempt " + (ix+1) + " of " + attempts);

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
                List<Byte> pts = Arrays.asList((byte) 96);
                MediaFormat format = LibJitsi.getMediaService()
                    .getFormatFactory().createMediaFormat("SILK", (double)8000);
                playRTP.playFile(argMap.get(FILENAME), format, pts, format, -1, 1);
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
