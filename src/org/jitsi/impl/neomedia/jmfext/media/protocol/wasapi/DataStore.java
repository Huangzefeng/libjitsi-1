package org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.Logger;

/**
 * A very hacky class which contains some media chunks to be inserted into the 
 * media stream instead of the media received from the input device
 */
public class DataStore
{
    private static final Logger sLog = Logger.getLogger(DataStore.class);
    
    private final ArrayList<byte[]> bytes;
    private int i = 0; 

    public DataStore()
    {
        bytes = new ArrayList<byte[]>();
        BufferedReader br = null;
        
        try
        {
            ResourceManagementService res = 
                                        LibJitsi.getResourceManagementService();
            String path = res.getSoundPath("RECORDED_WAV");
            sLog.error("Getting record from " + path);
            URL url = res.getSoundURLForPath(path);
            sLog.error("Sounds URL is " + url);
            InputStream is = url.openStream();
            InputStreamReader in = new InputStreamReader(is);
            br = new BufferedReader(in);
            
            String line = null;
            while ((line = br.readLine()) != null)
            {
                String[] fragments = line.split(",");
                byte[] linebytes = new byte[fragments.length];
                
                for (int i = 0; i < fragments.length; i++)
                {
                    linebytes[i] = Byte.valueOf(fragments[i]); 
                }
                
                bytes.add(linebytes);
            }
            
            sLog.info("Got some packets of data " + bytes.size());
        }
        catch (IOException e)
        {
            sLog.error("IOException opening! ", e);
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    sLog.error("IOException closing! ", e);
                }
            }
        }
    }
    
    public byte[] next()
    {
        i = (i + 1) % bytes.size();
        
        return bytes.get(i);        
    }
}
