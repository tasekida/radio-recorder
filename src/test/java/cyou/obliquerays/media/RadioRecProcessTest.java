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
package cyou.obliquerays.media;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;

import cyou.obliquerays.media.downloader.model.TsMedia;

/**
 * RedioRecProcessのUnitTest
 */
class RadioRecProcessTest {

	private static final Logger LOGGER = Logger.getLogger(RadioRecProcessTest.class.getName());

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
	 * System.exit(0)のケース
	 * {@link cyou.obliquerays.media.RadioRecProcess#main(String[])} のためのテスト・メソッド。
	 * @throws Exception
	 */
	@Test
	@ExpectSystemExitWithStatus(0)
	void testMainSuccess01() throws Exception {
		RadioRecProcess.main(null);

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

		media.stream()
	    	.peek(ts -> LOGGER.log(Level.INFO, "media=" + ts))
	    	.forEach(ts -> Assertions.assertNotNull(ts.getTsPath()));
		Assertions.assertTrue(Files.exists(Path.of("./NHK.mp3")));
	}
}
