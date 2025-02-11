/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.core.values.NotAvailable;
import net.azib.ipscan.core.values.NotScanned;
import net.azib.ipscan.fetchers.Fetcher;
import net.azib.ipscan.fetchers.FetcherRegistry;

/**
 * Scanner functionality is encapsulated in this class.
 * It uses a list of fetchers to perform the actual scanning.
 *
 * @author Anton Keks
 */
@Log4j2
@RequiredArgsConstructor
public class Scanner {
	private final FetcherRegistry fetcherRegistry;
	private final Map<Long, Fetcher> activeFetchers = new ConcurrentHashMap<>();

	/**
	 * Executes all registered fetchers for the current IP address.
	 * @param subject containing the IP address to scan
	 * @param result where the results are injected
	 */
	public void scan(ScanningSubject subject, ScanningResult result) {
		// populate results
		int fetcherIndex = 0;
		boolean isScanningInterrupted = false;
		for (Fetcher fetcher : fetcherRegistry.getSelectedFetchers()) {
			Object value = NotScanned.VALUE;
			try {
				activeFetchers.put(Thread.currentThread().getId(), fetcher);
				if (!subject.isAddressAborted() && !isScanningInterrupted) {
					// run the fetcher
					value = fetcher.scan(subject);
					// check if scanning was interrupted
					isScanningInterrupted = Thread.currentThread().isInterrupted();
					if (value == null)
						value = isScanningInterrupted ? NotScanned.VALUE : NotAvailable.VALUE;
				}
			}
			catch (Throwable e) {
				log.error(e);
			}
			// store the value
			result.setValue(fetcherIndex, value);
			fetcherIndex++;
		}
		activeFetchers.remove(Thread.currentThread().getId());

		result.setType(subject.getResultType());
	}

	public void interrupt(Thread thread) {
		Fetcher fetcher = activeFetchers.get(thread.getId());
		if (fetcher != null) fetcher.cleanup();
	}

	/**
	 * Init everything needed for scanning, including Fetchers
	 */
	public void init() {
		for (Fetcher fetcher : fetcherRegistry.getSelectedFetchers()) {
			fetcher.init();
		}
	}

	/**
	 * Cleanup after a scan
	 */
	public void cleanup() {
		activeFetchers.clear();
		for (Fetcher fetcher : fetcherRegistry.getSelectedFetchers()) {
			fetcher.cleanup();
		}
	}
}
