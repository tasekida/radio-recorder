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

/**
 * TsDownloaderのUnitTest
 */
class TsDownloaderTest {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(TsDownloaderTest.class.getName());

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
	 * {@link cyou.obliquerays.media.downloader.TsDownloader#getMedia()} のためのテスト・メソッド。
	 * @throws InterruptedException
	 */
	@Test
	void testRun01() throws InterruptedException {
		String source = "https://nhkradioakr2-i.akamaihd.net/hls/live/511929/1-r2/1-r2-01.m3u8";
		var queue = new ConcurrentLinkedQueue<TsMedia>();
		var executor = Executors.newScheduledThreadPool(10);
		var uri = URI.create(source);
		var hlsDownloader = new HlsDownloader(queue, executor, uri);
		var tsDownloader = new TsDownloader(queue, executor);
		executor.execute(hlsDownloader);
		var tsHandle = executor.scheduleAtFixedRate(tsDownloader, 0L, 20L, TimeUnit.SECONDS);
		Runnable tsCanceller = () -> tsHandle.cancel(false);
		executor.schedule(tsCanceller, 600L, TimeUnit.SECONDS);
		TimeUnit.SECONDS.sleep(5L);

	    while (!tsHandle.isDone() && !queue.isEmpty()) {
	    	TimeUnit.SECONDS.sleep(1L);
		}

	    executor.shutdown();
	    if (executor.awaitTermination(10L, TimeUnit.SECONDS))
	    	executor.shutdownNow();
	    TimeUnit.SECONDS.sleep(5L);

	    tsDownloader.getTsMedias().stream()
	    	.peek(media -> LOGGER.log(Level.INFO, "media=" + media))
	    	.peek(media -> Assertions.assertNotNull(media.getTsUri()))
	    	.forEach(media -> Assertions.assertNotNull(media.getTsPath()));
	}

	/**
	 * {@link cyou.obliquerays.media.downloader.TsDownloader#getMedia()} のためのテスト・メソッド。
	 * @throws InterruptedException
	 */
	@Test
	void testRun02() throws InterruptedException {
		var queue = new ConcurrentLinkedQueue<TsMedia>();
		for (int i = 238; i < 241; i++) {
			String source = "https://nhkradioakr2-i.akamaihd.net/hls/live/511929/1-r2/1-r2-20210218T163715-01-189/" + i + ".ts";
			TsMedia tsMedia = new TsMedia(URI.create(source));
			queue.add(tsMedia);
		}

		var executor = Executors.newScheduledThreadPool(10);
		var tsDownloader = new TsDownloader(queue, executor);
		var tsHandle = executor.scheduleAtFixedRate(tsDownloader, 0L, 5L, TimeUnit.SECONDS);

	    while (!tsHandle.isDone() && !queue.isEmpty()) {
	    	TimeUnit.SECONDS.sleep(1L);
		}

		Runnable tsCanceller = () -> tsHandle.cancel(false);
		executor.execute(tsCanceller);

	    executor.shutdown();
	    if (executor.awaitTermination(10L, TimeUnit.SECONDS))
	    	executor.shutdownNow();
	    TimeUnit.SECONDS.sleep(5L);

	    tsDownloader.getTsMedias().stream()
	    	.peek(media -> LOGGER.log(Level.INFO, "media=" + media))
	    	.peek(media -> Assertions.assertNotNull(media.getTsUri()))
	    	.forEach(media -> Assertions.assertNotNull(media.getTsPath()));
	}
}
