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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cyou.obliquerays.media.downloader.model.TsMedia;

/**
 * HTTP Live Streamingのインデックス本文サブスクライバの実装<br>
 * インデックスファイル（.m3u8）からセグメントファイル（.ts）パス一覧を取得
 */
public class HlsParserSubscriber implements Subscriber<String> {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(HlsParserSubscriber.class.getName());

    /** セグメントファイル（.ts）パスの正規表現 */
	private static final Pattern PATERN = Pattern.compile("^[^#].+\\.ts$");

	/** セグメントファイル（.ts）パス一覧 */
	private final Set<TsMedia> tsLines = new HashSet<>();

	/** HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI */
    private final URI m3u8Uri;

	/**
	 * HTTP Live Streamingのインデックス本文サブスクライバのコンストラクタ
	 * @param  _m3u8Uri HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI
	 */
	public HlsParserSubscriber(URI _m3u8Uri) {
		this.m3u8Uri = _m3u8Uri;
	}

	/**
	 * セグメントファイル（.ts）URIの作成
	 * @param m3u8Uri インデックスファイル（.m3u8）のURI
	 * @param tsPath セグメントファイル（.ts）のpath
	 * @return セグメントファイル（.ts）のURI
	 */
	private URI convertURI(URI m3u8Uri, String tsPath) {
		try {
			String path = Stream.of(m3u8Uri.getPath().split("/")).map(s -> s.replaceFirst("^.+\\.m3u8$", tsPath)).collect(Collectors.joining("/"));
			URI tsUri = new URI(m3u8Uri.getScheme(), m3u8Uri.getUserInfo(), m3u8Uri.getHost()
					, m3u8Uri.getPort(), path, m3u8Uri.getQuery(), m3u8Uri.getFragment());
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
		LOGGER.log(Level.CONFIG, new StringBuilder().append("subscription=").append(_subscription).toString());
		_subscription.request(Long.MAX_VALUE);
	}

	/** @see java.util.concurrent.Flow.Subscriber#onNext(Object) */
	@Override
	public void onNext(String _item) {
		LOGGER.log(Level.CONFIG, new StringBuilder().append("item=").append(_item).toString());
		if (PATERN.matcher(_item).matches())
			this.tsLines.add(new TsMedia(this.convertURI(this.m3u8Uri, _item)));
	}

	/** @see java.util.concurrent.Flow.Subscriber#onError(Throwable) */
	@Override
	public void onError(Throwable _throwable) {
		LOGGER.log(Level.CONFIG, new StringBuilder().append("throwable=").append(_throwable).toString());
		this.tsLines.clear();
	}

	/** @see java.util.concurrent.Flow.Subscriber#onComplete() */
	@Override
	public void onComplete() {
		LOGGER.log(Level.CONFIG, "");
	}
}
