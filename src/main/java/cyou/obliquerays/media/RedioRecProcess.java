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
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import cyou.obliquerays.media.downloader.NhkDownloader;
import cyou.obliquerays.media.downloader.model.TsMedia;
import cyou.obliquerays.media.jave2.NhkEncoder;
import cyou.obliquerays.status.LockFileStatus;

/**
 * NHKラジオ録画処理
 */
public class RedioRecProcess {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(RedioRecProcess.class.getName());

    /** NHK第2放送のHLS */
    private static final URI NHKURI = URI.create("https://nhkradioakr2-i.akamaihd.net/hls/live/511929/1-r2/1-r2-01.m3u8");

	/** 録音開始時間の文字列表現 */
	private static LocalTime START_TIME;

	/** 録音終了時間の文字列表現 */
	private static LocalTime END_TIME;

	/** 録音する曜日の一覧 */
	private static Set<DayOfWeek> SET_DAY_OF_WEEK;

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

	/** デフォルトコンストラクタ
	 * @throws IOException
	 */
	private RedioRecProcess() throws IOException {
		Path lockFile = Path.of(this.getClass().getSimpleName() + ".lock");
    	try {
			LockFileStatus lockFileStatus =
					new LockFileStatus(Thread.currentThread(), lockFile);
			this.executor.execute(lockFileStatus);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "プロセス実行時存在ファイルの管理に失敗#" + lockFile, e);
			throw e;
		}
	}

	private List<TsMedia> download() throws InterruptedException {
		NhkDownloader nhkDownloader = new NhkDownloader(this.executor, NHKURI);
		var future = this.executor.submit(nhkDownloader);
		while (!future.isDone()) {
			TimeUnit.SECONDS.sleep(5L);
		}
		return nhkDownloader.getTsMedias();
	}

	private Path encode(List<TsMedia> _media) {
		String target = "./NHK.mp3";
		var mp3path = Path.of(target);
		NhkEncoder recorder = new NhkEncoder(mp3path, _media);
		Path result = recorder.record();

		return result;
	}

	/**
	 * 録画実行
	 * @throws InterruptedException
	 */
	private void execute() throws InterruptedException {

		List<TsMedia> media = this.download();
		media.stream().forEach(tsMedia -> LOGGER.log(Level.INFO, "downloaded=" + tsMedia));

		Path mp3 = this.encode(media);

		this.executor.shutdown();
		if (this.executor.awaitTermination(10L, TimeUnit.SECONDS))
			this.executor.shutdownNow();
		TimeUnit.SECONDS.sleep(5L);

		LOGGER.log(Level.INFO, "mp3="+ mp3);
	}




	/**
	 * エントリーポイント
	 * @param args プログラム引数
	 * @throws Exception エラー終了原因の通知例外
	 */
	public static void main(String[] args) throws Exception {
		int returnCode = 0;// プログラムのリターンコード

//		START_TIME = LocalTime.parse(args[0]);// 開始時間の文字列表現
//		END_TIME = LocalTime.parse(args[1]);// 終了時間の文字列表現
//		String[] arrDayOfWeek = Arrays.copyOfRange(args, 2, args.length-1);// 録音する曜日の一覧文字列表現
//		SET_DAY_OF_WEEK =
//				Stream.of(arrDayOfWeek)
//				.map(DayOfWeek::valueOf)
//				.collect(Collectors.toSet());// 録音する曜日の一覧

        try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
    		RedioRecProcess process = new RedioRecProcess();
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
