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

import java.io.InputStream;
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

/**
 * NhkDownloaderのUnitTest
 */
class NhkDownloaderTest {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(NhkDownloaderTest.class.getName());

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
//		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
//	         public boolean accept(Path file) throws IOException {
//	        	 String fileName = file.getFileName().toString();
//	             return fileName.matches("^.+\\.ts$") || fileName.matches("^.+\\.mp3$");
//	         }
//	    };
//		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("."), filter)) {
//			stream.forEach(t -> {
//				try {
//					Files.delete(t);
//				} catch (IOException e) {
//					new IllegalStateException(e);
//				}
//			});
//		}
	}

	/**
	 * {@link cyou.obliquerays.media.downloader.NhkDownloader#getMedia()} のためのテスト・メソッド。
	 * @throws Exception
	 */
	@Test
	void testRun01() throws Exception {
		var executor = Executors.newScheduledThreadPool(10);
		var nhkDownloader = new NhkDownloader(executor);
		var future = executor.submit(nhkDownloader);
		while (!future.isDone()) {
			TimeUnit.SECONDS.sleep(5L);
		}

		executor.shutdown();
		if (executor.awaitTermination(10L, TimeUnit.SECONDS))
			executor.shutdownNow();
		TimeUnit.SECONDS.sleep(5L);

		nhkDownloader.getTsMedias().stream()
	    	.peek(media -> LOGGER.log(Level.INFO, "media=" + media))
	    	.peek(media -> Assertions.assertNotNull(media.getTsUri()))
	    	.forEach(media -> Assertions.assertNotNull(media.getTsPath()));
	}
}
