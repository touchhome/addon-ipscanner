/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core.net;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.core.ScanningSubject;

/**
 * UDP Pinger. Uses a UDP port to ping, doesn't require root privileges.
 *
 * @author Anton Keks
 */
@Log4j2
@RequiredArgsConstructor
public class UDPPinger implements Pinger {

	private static final int PROBE_UDP_PORT = 37381;

	private final int timeout;

	public PingResult ping(ScanningSubject subject, int count) throws IOException {
		PingResult result = new PingResult(subject.getAddress(), count);

		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(timeout);
			socket.connect(subject.getAddress(), PROBE_UDP_PORT);

			for (int i = 0; i < count && !Thread.currentThread().isInterrupted(); i++) {
				byte[] payload = new byte[8];
				long startTime = System.currentTimeMillis();
				ByteBuffer.wrap(payload).putLong(startTime);
				DatagramPacket packet = new DatagramPacket(payload, payload.length);
				try {
					socket.send(packet);
					socket.receive(packet);
				}
				catch (PortUnreachableException e) {
					result.addReply(System.currentTimeMillis() - startTime);
				}
				catch (SocketTimeoutException ignore) {
				}
				catch (NoRouteToHostException e) {
					// this means that the host is down
					break;
				}
				catch (SocketException e) {
					if (e.getMessage().contains(/*No*/"route to host")) {
						// sometimes 'No route to host' also gets here...
						break;
					}
				}
				catch (IOException e) {
					if (e.getMessage().startsWith("Network is unreachable"))
						break;
					log.debug(subject.toString(), e);
				}
			}
			return result;
		}
		finally {
			closeQuietly(socket);
		}
	}

	public void close() {
	}
}
