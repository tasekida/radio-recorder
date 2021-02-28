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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cyou.obliquerays.media.downloader.model.TsMedia;

/**
 * NHK第2放送のHLSよりセグメントファイル（.ts）をダウンロードする処理
 */
public class NhkDownloader extends AbstractMediaDownloader<TsMedia> implements Runnable {
	/** ロガー */
    private static final Logger LOGGER = Logger.getLogger(NhkDownloader.class.getName());

    /** 内部で使用する{@linkplain java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} */
    private final ScheduledExecutorService executor;

    /** HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI */
    private final URI m3u8Uri;

    /** ダウンロード済みのHLSセグメントファイル一覧 */
    private final List<TsMedia> tsMedias = new ArrayList<>();

	/**
	 * コンストラクタ
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.ScheduledExecutorService ScheduledExecutorService}
	 * @param _m3u8Uri HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI
	 */
	public NhkDownloader(ScheduledExecutorService _executor, URI _m3u8Uri) {
		super(new ConcurrentLinkedQueue<TsMedia>());
	    this.executor = _executor;
		this.m3u8Uri = _m3u8Uri;
	}

	@Override
	public void run() {

		var hlsDownloader = new HlsDownloader(this.media(), this.executor, this.m3u8Uri);
		var tsDownloader = new TsDownloader(this.media(), this.executor);
		var hlsHandle = this.executor.scheduleAtFixedRate(hlsDownloader, 0L, 20L, TimeUnit.SECONDS);
		var tsHandle = this.executor.scheduleAtFixedRate(tsDownloader, 0L, 1L, TimeUnit.SECONDS);
		Runnable hlsCanceller = () -> hlsHandle.cancel(false);
		Runnable tsCanceller = () -> tsHandle.cancel(false);
		this.executor.schedule(hlsCanceller, 15L, TimeUnit.MINUTES);

		try {
			TimeUnit.MINUTES.sleep(15L);
			while (!hlsHandle.isDone() && !this.media().isEmpty()) {
				LOGGER.log(Level.CONFIG, "NHK第2放送ダウンロード中");
				TimeUnit.SECONDS.sleep(1L);
			}
			this.executor.execute(tsCanceller);
		    while (!tsHandle.isDone()) {
    			LOGGER.log(Level.CONFIG, "NHK第2放送ダウンロード中");
    			TimeUnit.SECONDS.sleep(1L);
		    }
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "NHK第2放送ダウンロード中に割り込みを検知", e);
			throw new IllegalStateException(e);
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
