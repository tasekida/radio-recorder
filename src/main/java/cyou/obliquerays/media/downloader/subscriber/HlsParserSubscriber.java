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
package cyou.obliquerays.media.downloader.subscriber;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import cyou.obliquerays.media.model.TsMedia;
import cyou.obliquerays.media.model.TsMediaTool;

/**
 * HTTP Live Streamingのインデックス本文サブスクライバの実装<br>
 * インデックスファイル（.m3u8）からセグメントファイル（.ts）パス一覧を取得
 */
public class HlsParserSubscriber implements Subscriber<String> {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(HlsParserSubscriber.class.getName());

    /**
     * HLS（HTTP Live Streaming）インデックスファイル（.m3u8）に
     * 記載されているセグメントファイル（.ts）パスの正規表現
     */
	private static final Pattern TS_PATH_PATERN = Pattern.compile("^[^#].+\\.ts$");

	/** セグメントファイル（.ts）パス一覧 */
	private final Set<TsMedia> tsLines = new HashSet<>();

    /** HLS（HTTP Live Streaming）URIの基底PATH */
    private final URI hlsBaseURI;

	/** HTTP Live Streamingのインデックス本文サブスクライバのコンストラクタ */
	public HlsParserSubscriber() {
		this.hlsBaseURI = TsMediaTool.getBaseURI();
	}

	/**
	 * セグメントファイル（.ts）URIの作成
	 * @param _tsPath セグメントファイル（.ts）のpath
	 * @return セグメントファイル（.ts）のURI
	 */
	private URI convertURI(String _tsPath) {
		try {
			String path = this.hlsBaseURI.getPath() + _tsPath;
			URI tsUri = new URI(
					this.hlsBaseURI.getScheme()
					, this.hlsBaseURI.getUserInfo()
					, this.hlsBaseURI.getHost()
					, this.hlsBaseURI.getPort()
					, path
					, this.hlsBaseURI.getQuery()
					, this.hlsBaseURI.getFragment());
			LOGGER.log(Level.CONFIG, "tsUri=" + tsUri);
			return tsUri;
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * インデックスファイル（.m3u8）から抽出したセグメントファイル（.ts）のURI一覧を取得
	 * @return セグメントファイル（.ts）のURI一覧
	 */
	public Set<TsMedia> getMatchingLines() {
		LOGGER.log(Level.CONFIG, "");
		return this.tsLines;
	}

	/** @see java.util.concurrent.Flow.Subscriber#onSubscribe(Subscription) */
	@Override
	public void onSubscribe(Subscription _subscription) {
		LOGGER.log(Level.CONFIG, new StringBuilder("subscription=").append(_subscription).toString());
		_subscription.request(Long.MAX_VALUE);
	}

	/** @see java.util.concurrent.Flow.Subscriber#onNext(Object) */
	@Override
	public void onNext(String _item) {
		LOGGER.log(Level.CONFIG, new StringBuilder("item=").append(_item).toString());
		if (TS_PATH_PATERN.matcher(_item).matches())
			this.tsLines.add(new TsMedia(this.convertURI(_item)));
	}

	/** @see java.util.concurrent.Flow.Subscriber#onError(Throwable) */
	@Override
	public void onError(Throwable _throwable) {
		LOGGER.log(Level.CONFIG, new StringBuilder("throwable=").append(_throwable).toString());
		this.tsLines.clear();
	}

	/** @see java.util.concurrent.Flow.Subscriber#onComplete() */
	@Override
	public void onComplete() {
		LOGGER.log(Level.CONFIG, "");
	}
}
