/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core.net;

import java.net.InetAddress;
import lombok.Getter;

/**
 * The result of pinging
 *
 * @author Anton Keks
 */
public class PingResult {
	InetAddress address;

	private int ttl;
	private long totalTime;
	private long longestTime;
	@Getter private int packetCount;
	@Getter private int replyCount;
	@Getter private boolean timeoutAdaptationAllowed;

	public PingResult(InetAddress address, int packetCount) {
		this.address = address;
		this.packetCount = packetCount;
	}

	public void addReply(long time) {
		replyCount++;
		if (time > longestTime)
			longestTime = time;
		totalTime += time;
		// this is for ports fetcher, etc
		timeoutAdaptationAllowed = replyCount > 2;
	}

	public int getTTL() {
		return ttl;
	}

	public void setTTL(int ttl) {
		this.ttl = ttl;
	}

	public int getAverageTime() {
		return (int)(totalTime / replyCount);
	}

	public int getLongestTime() {
		return (int)longestTime;
	}

	public int getPacketLoss() {
		return packetCount - replyCount;
	}

	public int getPacketLossPercent() {
		if (replyCount > 0)
			return (this.getPacketLoss() * 100) / packetCount;
		else
			return 100;
	}

	/**
	 * @return true in case at least one reply was received
	 */
	public boolean isAlive() {
		return replyCount > 0;
	}

	public void enableTimeoutAdaptation() {
		if (isAlive())
			timeoutAdaptationAllowed = true;
	}

	PingResult merge(PingResult result) {
		this.packetCount += result.packetCount;
		this.replyCount += result.replyCount;
		return this;
	}
}
