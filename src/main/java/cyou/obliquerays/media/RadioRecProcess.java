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
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import cyou.obliquerays.media.command.TsEncoder;
import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.NhkDownloader;
import cyou.obliquerays.media.downloader.model.TsMedia;
import cyou.obliquerays.status.LockFileStatus;

/**
 * NHKラジオ録画処理
 */
public class RadioRecProcess {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(RadioRecProcess.class.getName());

    /** スレッド管理 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

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
			LOGGER.log(Level.SEVERE, "プロセス実行時存在ファイルの管理に失敗#" + lockFile, e);
			throw e;
		}
	}

	/**
	 * HLSインデックスファイルに記載されているセグメントファイルをダウンロード
	 * @return ダウンロード済みのHLSセグメントファイル
	 * @throws InterruptedException ダウンロード中の割り込み
	 */
	private List<TsMedia> download() throws InterruptedException {
		var nhkDownloader = new NhkDownloader(this.executor);
		var future = this.executor.submit(nhkDownloader);
		while (!future.isDone()) {
			TimeUnit.SECONDS.sleep(1L);
		}
		return nhkDownloader.getTsMedias();
	}

	/**
	 * セグメントファイルをMP3ファイルへエンコード
	 * @param _media ダウンロード済みのHLSセグメントファイル
	 * @return エンコード後のMP3ファイル
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private Path encode(List<TsMedia> _media) throws IOException, InterruptedException, ExecutionException {
		TsEncoder recorder = new TsEncoder(_media);
		Path result = recorder.record();
		return result;
	}

	/**
	 * 録音実行
	 * @throws InterruptedException 録音中の割り込み
	 */
	private void execute() throws InterruptedException {

		try {
			do {
				if (!RadioProperties.getProperties().getDayOfWeeks().contains(DayOfWeek.from(LocalDate.now()))) {
					LOGGER.log(Level.CONFIG, "待機");
					TimeUnit.HOURS.sleep(1L);
					continue;
				}

				LocalTime start = RadioProperties.getProperties().getStart();
				LocalTime end = RadioProperties.getProperties().getEnd();
				if (start.minusMinutes(10L).isAfter(LocalTime.now()) || end.minusMinutes(1L).isBefore(LocalTime.now())) {
					LOGGER.log(Level.CONFIG, "待機");
					TimeUnit.MINUTES.sleep(1L);
					continue;
				}

				List<TsMedia> media = this.download();
				media.stream().forEach(tsMedia -> LOGGER.log(Level.CONFIG, "downloaded=" + tsMedia));
				Path mp3 = this.encode(media);
				LOGGER.log(Level.INFO, "録音ファイル = "+ mp3);

			} while (RadioProperties.getProperties().isProcess());

		} catch (Exception e) {

			LOGGER.log(Level.SEVERE, "エラー終了", e);

		} finally {

			this.executor.shutdown();
			if (!this.executor.awaitTermination(10L, TimeUnit.SECONDS) && !this.executor.isTerminated())
				this.executor.shutdownNow();

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
        } catch (Exception e) {
        	LOGGER.log(Level.SEVERE, "エラー終了", e);
        	returnCode = 1;
        	System.exit(returnCode);
        }

        try {
    		RadioRecProcess process = new RadioRecProcess();
    		process.execute();
        } catch (Exception e) {
        	LOGGER.log(Level.SEVERE, "エラー終了", e);
        	returnCode = 1;
        	throw e;
        } finally {
            System.exit(returnCode);
        }
	}
}
