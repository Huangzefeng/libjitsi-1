package org.jitsi.service.audionotifier;

/**
 * Used to track the state of audio clips that are playing.
 * Handler functions are executed in their own thread.
 */
public interface AudioListener {
	public void onClipStarted();
	public void onClipEnded();
}
