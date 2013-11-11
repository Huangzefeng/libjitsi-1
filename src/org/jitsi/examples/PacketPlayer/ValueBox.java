package org.jitsi.examples.PacketPlayer;

public class ValueBox<T> {
	T value;
	ValueBox(T val) {set(val);}
	T get() {return value;}
	void set(T val) { value = val;}
}
