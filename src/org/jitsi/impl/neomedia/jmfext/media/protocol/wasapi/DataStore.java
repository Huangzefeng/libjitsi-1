package org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.jitsi.service.resources.ResourceManagementService;

/**
 * A very hacky class which contains some media chunks to be inserted into the 
 * media stream instead of the media received from the input device
 */
public class DataStore
{
    private final ArrayList<byte[]> bytes;
    private int i = 0; 

    public DataStore()
    {
        bytes = new ArrayList<byte[]>();
        BufferedReader br = null;
        
        try
        {
            FileReader fr = new FileReader("resources/sounds/recorded.wav");
            br = new BufferedReader(fr);
            
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
        }
        catch (IOException e)
        {
            e.printStackTrace();
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
