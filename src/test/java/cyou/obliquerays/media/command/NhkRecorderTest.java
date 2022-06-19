/**
 * 
 */
package cyou.obliquerays.media.command;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NhkEncoderのUnitTest */
class NhkRecorderTest {
	/** ロガー */
	private static final Logger LOG = System.getLogger(NhkRecorderTest.class.getName());
	
	/** @throws java.lang.Exception */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
    	try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        } catch (Throwable t) {
        	LOG.log(Level.ERROR, "エラー終了", t);
        }
		Arrays.stream(System.getenv("Path").split(";")).forEach(str -> LOG.log(Level.DEBUG, str));
	}

	/** @throws java.lang.Exception */
	@AfterAll
	static void tearDownAfterClass() throws Exception {}

	/** @throws java.lang.Exception */
	@BeforeEach
	void setUp() throws Exception {	}

	/** @throws java.lang.Exception */
	@AfterEach
	void tearDown() throws Exception {}

	/**
	 * {@link cyou.obliquerays.media.command.NhkRecorder#record()} のためのテスト・メソッド。
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test
	void testRecord() throws IOException, InterruptedException, ExecutionException {
		NhkRecorder recorder = new NhkRecorder();
		Path result = recorder.record();

		LOG.log(Level.INFO, "result="+ result);
	}

}
