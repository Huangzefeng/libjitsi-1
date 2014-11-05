package org.jitsi.impl.neomedia;


public class DoNothingPacketMonkey implements PacketMonkey 
{
	@Override
	public boolean shouldDropPacket() 
	{
		return false;
	}

}
