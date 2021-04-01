/**
 * Copyright (C) 2021 tasekida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cyou.obliquerays.media.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cyou.obliquerays.media.downloader.model.TsMedia;

/** HlsDownloaderのUnitTest */
class HlsDownloaderTest {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(HlsDownloaderTest.class.getName());

	/** @throws java.lang.Exception */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
        try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        }
	}

	/** @throws java.lang.Exception */
	@AfterAll
	static void tearDownAfterClass() throws Exception {}

	/** @throws java.lang.Exception */
	@BeforeEach
	void setUp() throws Exception {	}

	/** @throws java.lang.Exception */
	@AfterEach
	void tearDown() throws Exception {
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
	         public boolean accept(Path file) throws IOException {
	        	 String fileName = file.getFileName().toString();
	             return fileName.matches("^.+\\.ts$") || fileName.matches("^.+\\.mp3$");
	         }
	    };
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("."), filter)) {
			stream.forEach(t -> {
				try {
					Files.delete(t);
				} catch (IOException e) {
					new IllegalStateException(e);
				}
			});
		}
	}

	/**
	 * {@link cyou.obliquerays.media.downloader.HlsDownloader#getMedia()} のためのテスト・メソッド。
	 * @throws InterruptedException
	 */
	@Test
	void testRun01() throws InterruptedException {
		String source = "https://nhkradioakr2-i.akamaihd.net/hls/live/511929/1-r2/1-r2-01.m3u8";
		var queue = new ConcurrentLinkedQueue<TsMedia>();
		var executor = Executors.newScheduledThreadPool(10);
		var uri = URI.create(source);
		var hlsDownloader = new HlsDownloader(queue, executor, uri);
		var hlsHandle = executor.scheduleAtFixedRate(hlsDownloader, 0L, 10L, TimeUnit.SECONDS);
		Runnable canceller = () -> hlsHandle.cancel(false);
		executor.schedule(canceller, 60L, TimeUnit.SECONDS);

	    while (!hlsHandle.isDone()) {
	    	TimeUnit.SECONDS.sleep(1L);
		}

	    executor.shutdown();
	    if (executor.awaitTermination(10L, TimeUnit.SECONDS))
	    	executor.shutdownNow();
	    TimeUnit.SECONDS.sleep(5L);

	    hlsDownloader.media().stream()
	    	.peek(media -> LOGGER.log(Level.INFO, "media=" + media))
	    	.forEach(media -> Assertions.assertNotNull(media.getTsUri()));
	}
}
