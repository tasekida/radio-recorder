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
package cyou.obliquerays.media;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;

import cyou.obliquerays.media.config.RadioProperties;

/**
 * RedioRecProcessのUnitTest
 */
class RadioRecProcessTest {

	private static final Logger LOG = System.getLogger(RadioRecProcessTest.class.getName());

	/** @throws java.lang.Exception */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        } catch (Throwable t) {
        	LOG.log(Level.ERROR, "エラー終了", t);
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
	             return fileName.matches("^.+\\.mp3$");
	         }
	    };
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(RadioProperties.getProperties().getBaseDir()), filter)) {
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

		Assertions.assertTrue(Files.exists(RadioProperties.getProperties().getMp3FilePath()));
	}
}
