package org.jitsi.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Logging class which logs messages on a back-off.  I.e., it logs the messages
 * that it has been asked to log with decreasing frequency.
 */
public class IntervalLogger
{
    /**
     * The maximum number of log attempts time to allow between actually logging 
     */
    private static final int MAX_INTERVAL = 1024;
    
    /**
     * Map keeping track of how many times the logger has been asked to log a
     * particular message.  The map is from the message (which can't be null)
     * to the number of times that we have been asked to log that message.
     */
    private final Map<Object, Integer> timesTriedToLogMap = 
                                                 new HashMap<Object, Integer>();
    
    /**
     * The logger that we actually use to log with
     */
    private final Logger delegateLogger;
    
    /**
     * Constructor
     * 
     * @param logger the logger that we use to log with.
     */
    public IntervalLogger(Logger logger)
    {
        delegateLogger = logger;
    }

    /**
     * Log a DEBUG message.
     * <p>
     * If the logger is currently enabled for the DEBUG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param msg The message to log.  Should not be null
     */
    public void debug(Object msg)
    {
        if (shouldLog(msg))
            delegateLogger.debug(msg);
    }
    
    /**
     * Tests to see if we should log the message that we have been passed
     * 
     * @param msg The message to (possibly) log
     * @return true if the message should be logged
     */
    private synchronized boolean shouldLog(Object msg)
    {
        boolean shouldLog;
        int timesTriedToLog;
        
        if (timesTriedToLogMap.containsKey(msg))
        {
            // We've logged this message before - find out how many times: 
            timesTriedToLog = timesTriedToLogMap.get(msg);
        }
        else
        {
            // We've not logged this before
            timesTriedToLog = 0;
        }
        
        if (timesTriedToLog > MAX_INTERVAL)
        {
            // We've tried to log this more than MAX_INTERVAL times, therefore 
            // we should only be logging every MAX_INTERVAL-th time.
            shouldLog = (timesTriedToLog % MAX_INTERVAL == 0);
        }
        else
        {
            // We've not logged very often, log only if the number of times we
            // have tried to log is a power of 2 (including 0).  I.e. the 0th, 
            // 1st, 2nd, 4th, 8th...
            // Use a bit hack to determine if this is the case.  
            shouldLog = ((timesTriedToLog & (timesTriedToLog - 1)) == 0);
        }
        
        timesTriedToLog++;
        timesTriedToLogMap.put(msg, Integer.valueOf(timesTriedToLog));
                
        return shouldLog;        
    }
}
