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
package cyou.obliquerays.media.jave2;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cyou.obliquerays.media.downloader.NhkDownloader;
import cyou.obliquerays.media.downloader.model.TsMedia;

/** NhkRecorderのUnitTest */
class NhkEncoderTest {
	/** ロガー */
	private static final Logger LOGGER = Logger.getLogger(NhkEncoderTest.class.getName());

	/** @throws java.lang.Exception */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
    	try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        } catch (Throwable t) {
        	LOGGER.log(Level.SEVERE, "エラー終了", t);
        }
	}

	/** @throws java.lang.Exception */
	@AfterAll
	static void tearDownAfterClass() throws Exception {}

	/** @throws java.lang.Exception */
	@BeforeEach
	void setUp() throws Exception {}

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
	 * TSファイルをダウンロードしてエンコードするテスト
	 * {@link cyou.obliquerays.media.jave2.NhkEncoder#record()} のためのテスト・メソッド
	 * @throws MalformedURLException
	 * @throws InterruptedException
	 */
	@Test
	void testRecord01() throws MalformedURLException, InterruptedException {
		String source = "https://nhkradioakr2-i.akamaihd.net/hls/live/511929/1-r2/1-r2-01.m3u8";
		var executor = Executors.newScheduledThreadPool(10);
		var uri = URI.create(source);
		var nhkDownloader = new NhkDownloader(executor, uri);
		var future = executor.submit(nhkDownloader);
		while (!future.isDone()) {
			TimeUnit.SECONDS.sleep(5L);
		}
		List<TsMedia> media = nhkDownloader.getTsMedias();
		media.stream().forEach(tsMedia -> LOGGER.log(Level.INFO, "download=" + tsMedia));

		String target = "./NHK.mp3";
		var mp3path = Path.of(target);
		NhkEncoder recorder = new NhkEncoder(mp3path, media);
		Path result = recorder.record();

		executor.shutdown();
		if (executor.awaitTermination(10L, TimeUnit.SECONDS))
			executor.shutdownNow();
		TimeUnit.SECONDS.sleep(5L);

		media.stream()
	    	.peek(ts -> LOGGER.log(Level.INFO, "media=" + ts))
	    	.peek(ts -> Assertions.assertNotNull(ts.getTsUri()))
	    	.forEach(ts -> Assertions.assertNotNull(ts.getTsPath()));
		Assertions.assertTrue(Files.exists(result));
	}

	/**
	 * ダウンロード済みのTSファイルを使用するテスト
	 * {@link cyou.obliquerays.media.jave2.NhkEncoder#record()} のためのテスト・メソッド。
	 * @throws IOException
	 */
	@Test
	@Disabled
	void testRecord02() throws IOException {

		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
	        public boolean accept(Path file) throws IOException {
	            return file.toString().matches("^.+\\.ts$");
	        }
	    };
	    List<TsMedia> media = new ArrayList<>();
	    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of("."), filter)) {
	    	dirStream.forEach(path -> {
	    		TsMedia ts = new TsMedia(URI.create("https://nhkradioakr2-i.akamaihd.net/hls/live/511929/1-r2/1-r2-01.m3u8"));
	    		ts.setTsPath(path);
	    		media.add(ts);
	    	});
	    }

		media.stream().forEach(tsMedia -> LOGGER.log(Level.INFO, "download=" + tsMedia));

		String target = "./NHK.mp3";
		var mp3path = Path.of(target);
		NhkEncoder recorder = new NhkEncoder(mp3path, media);
		Path result = recorder.record();

		LOGGER.log(Level.INFO, "result="+ result);
	}
}
