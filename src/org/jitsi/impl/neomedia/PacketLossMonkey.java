package org.jitsi.impl.neomedia;

import java.util.Random;

import org.jitsi.util.Logger;

public class PacketLossMonkey implements PacketMonkey
{
    private static Logger sLogger = Logger.getLogger(PacketLossMonkey.class);

	private Random mRandom = new Random();

	private float mMeanInterval;
	private float mIntervalVariance;
	private float mMeanCutout;
	private float mCutoutVariance;

	int mPacketsToDrop = 0;
	int mPacketsToKeep = 0;

	public PacketLossMonkey(float meanInterval, float intervalVariance,
			                float meanCutout, float cutoutVariance)
	{
		mMeanInterval = meanInterval;
		mIntervalVariance = intervalVariance;
		mMeanCutout = meanCutout;
		mCutoutVariance = cutoutVariance;
	}

	@Override
	public boolean shouldDropPacket()
	{
		if (mPacketsToDrop > 0)
		{
			mPacketsToDrop -= 1;
			return true;
		}
		else if (mPacketsToKeep > 0)
		{
			mPacketsToKeep -= 1;
			return false;
		}
		else
		{
			// Calculate the duration of the next cut-out (which will happen
			// immediately), and the duration of the recovery period which will
			// follow it.
			float dropPeriod = nextGaussian(mMeanCutout, mCutoutVariance);
			float recoveryPeriod = nextGaussian(mMeanInterval, mIntervalVariance);
			mPacketsToDrop = (int)Math.round(dropPeriod / 0.02);
			mPacketsToKeep = (int)Math.round(recoveryPeriod / 0.02);
			mPacketsToDrop--;
			sLogger.debug("Will drop the next " + mPacketsToDrop +
			              " packets and save the following " + mPacketsToKeep);
			return true;
		}
	}

	private float nextGaussian(float mean, float variance)
	{
	    float result;
	    while (true)
	    {
	        result = (float)mRandom.nextGaussian() * variance + mean;

	        if (result > 0)
	        {
	            return result;
	        }
	    }
	}
}
