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
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.NhkDownloader;
import cyou.obliquerays.media.downloader.model.TsMedia;
import cyou.obliquerays.media.jave2.NhkEncoder;
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
	 */
	private Path encode(List<TsMedia> _media) {
		NhkEncoder recorder = new NhkEncoder(_media);
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