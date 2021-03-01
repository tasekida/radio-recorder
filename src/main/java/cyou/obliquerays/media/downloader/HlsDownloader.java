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

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cyou.obliquerays.media.downloader.model.TsMedia;
import cyou.obliquerays.media.downloader.subscriber.HlsParserSubscriber;

/**
 * HLS（HTTP Live Streaming）のインデックスファイルを読み取る処理<br>
 * インデックスファイル（.m3u8）からセグメントファイル（.ts）URI一覧を取得<br>
 * M3U8はUTF-8で書かれたM3Uファイル
 */
public class HlsDownloader extends AbstractMediaDownloader<TsMedia> implements Runnable {
	/** ロガー */
    private static final Logger LOGGER = Logger.getLogger(HlsDownloader.class.getName());

    /** HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI */
    private final URI m3u8Uri;

    /** HTTP送信クライアント */
    private final HttpClient client;

    /** HTTP送信リクエスト */
    private final HttpRequest request;

	/**
	 * HLS（HTTP Live Streaming）インデックスファイルを読み取る処理を初期化
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.Executor Executor}
	 */
	/**
	 * HLS（HTTP Live Streaming）インデックスファイルを読み取る処理を初期化
	 * @param _queue ダウンロード対象のHLSセグメントファイル情報一覧
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.Executor Executor}
	 * @param _m3u8Uri HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI
	 */
	public HlsDownloader(Queue<TsMedia> _queue, Executor _executor, URI _m3u8Uri) {
		super(_queue);
        this.m3u8Uri = _m3u8Uri;
		this.client = HttpClient.newBuilder()
        		.version(Version.HTTP_2)
        		.followRedirects(Redirect.NORMAL)
        		.proxy(HttpClient.Builder.NO_PROXY)
        		.executor(_executor)
        		.build();
        this.request = HttpRequest.newBuilder()
        		.uri(_m3u8Uri)
        		.timeout(Duration.ofSeconds(60L))
        		.header("Content-Type", "application/x-mpegURL")
        		.GET()
        		.build();
	}

	/**
	 * HTTP送信クライアント実行結果のハンドリング
	 * @return HTTP送信クライアント実行結果をハンドリングする関数
	 */
	private BiFunction<HttpResponse<Set<TsMedia>>, Throwable, Set<TsMedia>> getHandle() {
		return (response, e) -> {
			if (Optional.ofNullable(e).isPresent()) {
				LOGGER.log(Level.SEVERE, "HTTP #RESPONSE=ERROR", e);
				return Collections.emptySet();
			} else {
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    LOGGER.log(Level.INFO, "HTTP #RESPONSE=" + response.statusCode());
                } else {
                    StringBuilder msg = new StringBuilder()
                    		.append("HTTP #RESPONSE=").append(response.statusCode())
                    		.append("#BODY=").append(response.body().stream()
                    				.map(TsMedia::getTsUri)
                    				.map(URI::toString)
                    				.collect(Collectors.joining(System.lineSeparator())));
                    LOGGER.log(Level.SEVERE, msg.toString());
                }
				return response.body();
			}
		};
	}

	/**
	 * HLS（HTTP Live Streaming）インデックスファイルからセグメントファイル（.ts）URI一覧を取得
	 */
	@Override
	public void run() {
        LOGGER.log(Level.CONFIG, "URI=" + this.m3u8Uri);
		try {
			CompletableFuture<HttpResponse<Set<TsMedia>>> completableFuture = this.client
					.sendAsync(this.request, BodyHandlers.fromLineSubscriber(new HlsParserSubscriber(this.m3u8Uri), HlsParserSubscriber::getMatchingLines, null));
			Set<TsMedia> responce = completableFuture.handle(this.getHandle()).get();
			responce.stream().filter(media -> !this.media().contains(media)).forEach(media -> this.media().offer(media));
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "HTTP送信クライアント実行中に割り込みを検知", e);
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "HTTP送信クライアント実行中にエラーが発生", e);
		}
	}
}
