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
package cyou.obliquerays.media.downloader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.model.TsMedia;

/**
 * NHK第2放送のHLSよりセグメントファイル（.ts）をダウンロードする処理
 */
public class NhkDownloader extends AbstractMediaDownloader<TsMedia> implements Runnable {
	/** ロガー */
    private static final Logger LOGGER = Logger.getLogger(NhkDownloader.class.getName());

    /** 内部で使用する{@linkplain java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} */
    private final ScheduledExecutorService executor;

    /** ダウンロード済みのHLSセグメントファイル一覧 */
    private final List<TsMedia> tsMedias = new ArrayList<>();

	/**
	 * コンストラクタ
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.ScheduledExecutorService ScheduledExecutorService}
	 */
	public NhkDownloader(ScheduledExecutorService _executor) {
		super(new ConcurrentLinkedQueue<TsMedia>());
	    this.executor = Objects.requireNonNull(_executor);
	}

	@Override
	public void run() {
		var hlsDownloader = new HlsDownloader(this.media(), this.executor, RadioProperties.getProperties().getRadio());
		var tsDownloader = new TsDownloader(this.media(), this.executor);

		try {
			LocalTime start = RadioProperties.getProperties().getStart();
			LocalTime end = RadioProperties.getProperties().getEnd();
			Duration duration = Duration.between(start, end.plusMinutes(1L));

			while (LocalTime.now().isBefore(start.minusMinutes(1L))) {
				LOGGER.log(Level.CONFIG, "待機");
				TimeUnit.MINUTES.sleep(1L);
			}

			Path saveDir = Path.of(RadioProperties.getProperties().getSaveDir());
			if (!Files.isDirectory(saveDir) &&  Files.notExists(saveDir)) {
				Files.createDirectories(saveDir);
			}

			var hlsHandle = this.executor.scheduleAtFixedRate(hlsDownloader, 0L, 20L, TimeUnit.SECONDS);
			var tsHandle1 = this.executor.scheduleAtFixedRate(tsDownloader, 0L, 1L, TimeUnit.SECONDS);
			var tsHandle2 = this.executor.scheduleAtFixedRate(tsDownloader, 0L, 1L, TimeUnit.SECONDS);
			this.executor.schedule(() -> hlsHandle.cancel(false), duration.toMinutes(), TimeUnit.MINUTES);
			TimeUnit.MINUTES.sleep(duration.toMinutes());

			while (!hlsHandle.isDone() && !this.media().isEmpty()) {
				LOGGER.log(Level.CONFIG, "NHK第2放送ダウンロード中");
				TimeUnit.SECONDS.sleep(1L);
			}

			this.executor.execute(() -> tsHandle1.cancel(false));
			this.executor.execute(() -> tsHandle2.cancel(false));
		    while (!tsHandle1.isDone() || !tsHandle2.isDone()) {
    			LOGGER.log(Level.CONFIG, "NHK第2放送ダウンロード中");
    			TimeUnit.SECONDS.sleep(1L);
		    }
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "NHK第2放送ダウンロード中に割り込みを検知", e);
			throw new IllegalStateException(e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "NHK第2放送ダウンロード中にIOエラーを検知", e);
			throw new UncheckedIOException(e);
		}

	    this.tsMedias.clear();
	    this.tsMedias.addAll(tsDownloader.getTsMedias());
	    tsDownloader.getTsMedias().clear();
	}

	/**
	 * ダウンロード済みのHLSセグメントファイル一覧を取得
	 * @return tsMedias ダウンロード済みのHLSセグメントファイル一覧
	 */
	public List<TsMedia> getTsMedias() {
		return this.tsMedias;
	}
}
