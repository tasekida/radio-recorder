/**
 * Copyright (C) 2021 tasekida
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package cyou.obliquerays.media.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cyou.obliquerays.media.model.TsMedia;
import cyou.obliquerays.media.model.TsMediaTool;

/** HlsDownloaderのUnitTest */
class HlsDownloaderTest {
    /** ロガー */
    private static final Logger LOG = System.getLogger(HlsDownloaderTest.class.getName());

	/** @throws java.lang.Exception */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
        try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        }
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
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
	    	.sorted(TsMediaTool.getURIComparator())
	    	.peek(media -> LOG.log(Level.INFO, "media=" + media))
	    	.forEach(media -> Assertions.assertNotNull(media.getTsUri()));
	}
}
