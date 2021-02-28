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
	 * HTTP送信クライアント実行結果のハンドリング
	 * @return HTTP送信クライアント実行結果をハンドリングする関数
	 */
	/**
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
