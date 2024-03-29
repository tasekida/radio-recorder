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
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import cyou.obliquerays.media.command.NhkRecorder;
import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.status.LockFileStatus;

/**
 * NHKラジオ録音処理
 */
public class RadioRecProcess {
    /** ロガー */
    private static final Logger LOG = System.getLogger(RadioRecProcess.class.getName());

    /** スレッド管理 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

	/**
	 * デフォルトコンストラクタ
	 * @throws IOException ファイル操作失敗
	 */
	private RadioRecProcess() throws IOException {

		var lockFile = Path.of(this.getClass().getSimpleName() + ".lock");
    	try {
			var lockFileStatus =
					new LockFileStatus(Thread.currentThread(), lockFile);
			this.executor.scheduleAtFixedRate(lockFileStatus, 5L, 1L, TimeUnit.SECONDS);
		} catch (IOException e) {
			LOG.log(Level.ERROR, "プロセス実行時存在ファイルの管理に失敗#" + lockFile, e);
			throw e;
		}
	}

	/**
	 * 録音実行
	 * @throws InterruptedException 録音中の割り込み
	 */
	private void execute() throws InterruptedException {

		try {
			do {
				if (!RadioProperties.getProperties().getDayOfWeeks().contains(DayOfWeek.from(LocalDate.now()))) {
					LOG.log(Level.DEBUG, "待機");
					TimeUnit.HOURS.sleep(1L);
					continue;
				}

				LocalTime start = RadioProperties.getProperties().getStart();
				LocalTime end = RadioProperties.getProperties().getEnd();
				if (start.minusMinutes(2L).isAfter(LocalTime.now()) || end.minusMinutes(1L).isBefore(LocalTime.now())) {
					LOG.log(Level.DEBUG, "待機");
					TimeUnit.MINUTES.sleep(1L);
					continue;
				}

				Future<Path> future = this.executor.submit(new NhkRecorder());
				LOG.log(Level.INFO, "録音ファイル = "+ future.get());

			} while (RadioProperties.getProperties().isProcess());

		} catch (Exception e) {

			LOG.log(Level.ERROR, "エラー終了", e);

		} finally {

			this.executor.shutdown();
			if (!this.executor.awaitTermination(10L, TimeUnit.SECONDS) && !this.executor.isTerminated()) {
				this.executor.shutdownNow();
			}
		}
	}

	/**
	 * エントリーポイント
	 * @param args プログラム引数
	 * @throws Exception エラー終了原因の通知例外
	 */
	public static void main(String[] args) throws Exception {
		int returnCode = 0;// プログラムのリターンコード

        try (InputStream propLogging = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(propLogging);
            LOG.log(Level.DEBUG, "logging.properties#handlers=" + LogManager.getLogManager().getProperty("handlers"));
            LOG.log(Level.DEBUG, "logging.properties#.level=" + LogManager.getLogManager().getProperty(".level"));
            LOG.log(Level.DEBUG, "logging.properties#java.util.logging.ConsoleHandler.level=" + LogManager.getLogManager().getProperty("java.util.logging.ConsoleHandler.level"));
            LOG.log(Level.DEBUG, "logging.properties#java.util.logging.ConsoleHandler.formatter=" + LogManager.getLogManager().getProperty("java.util.logging.ConsoleHandler.formatter"));
        } catch (Exception e) {
        	LOG.log(Level.ERROR, "エラー終了", e);
        	returnCode = 1;
        	System.exit(returnCode);
        }

        try {
    		RadioRecProcess process = new RadioRecProcess();
    		process.execute();
        } catch (InterruptedException e) {
        	LOG.log(Level.INFO, "割り込み終了", e);
        	returnCode = 0;
        } catch (Exception e) {
        	LOG.log(Level.ERROR, "エラー終了", e);
        	returnCode = 1;
        	throw e;
        } finally {
            System.exit(returnCode);
        }
	}
}
