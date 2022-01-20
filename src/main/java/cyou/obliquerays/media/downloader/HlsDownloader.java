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
package cyou.obliquerays.media.downloader;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
import java.util.stream.Collectors;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.subscriber.HlsParserSubscriber;
import cyou.obliquerays.media.model.TsMedia;

/**
 * HLS（HTTP Live Streaming）のインデックスファイルを読み取る処理<br>
 * インデックスファイル（.m3u8）からセグメントファイル（.ts）URI一覧を取得<br>
 * M3U8はUTF-8で書かれたM3Uファイル
 */
public class HlsDownloader extends AbstractMediaDownloader<TsMedia> implements Runnable {
	/** ロガー */
    private static final Logger LOG = System.getLogger(HlsDownloader.class.getName());

    /** HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI */
    private final URI m3u8Uri;

    /** HTTP送信クライアント */
    private final HttpClient client;

    /** HTTP送信リクエスト */
    private final HttpRequest request;

	/**
	 * HLS（HTTP Live Streaming）インデックスファイルを読み取る処理を初期化
	 * @param _queue ダウンロード対象のHLSセグメントファイル情報一覧
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.Executor Executor}
	 * @param _m3u8Uri HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURI
	 */
	public HlsDownloader(Queue<TsMedia> _queue, Executor _executor, URI _m3u8Uri) {
		super(_queue);
        this.m3u8Uri = _m3u8Uri;
		HttpClient.Builder builder = HttpClient.newBuilder()
        		.version(Version.HTTP_2)
        		.followRedirects(Redirect.NORMAL)
        		.executor(_executor);
		if (RadioProperties.getProperties().isProxy()) {
			builder = builder.proxy(RadioProperties.getProperties().getProxySelector());
			if (RadioProperties.getProperties().isProxyAuth()) {
				builder = builder.authenticator(RadioProperties.getProperties().getProxyAuthenticator());
			}
		}
	    this.client = builder.build();
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
				LOG.log(Level.ERROR, "HTTP #RESPONSE=ERROR", e);
				return Collections.emptySet();
			} else {
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                	LOG.log(Level.INFO, "HTTP #RESPONSE=" + response.statusCode());
                } else {
                    String msg = new StringBuilder("HTTP #RESPONSE=").append(response.statusCode()).append("#BODY=")
                    		.append(response.body().stream()
                    				.map(TsMedia::getTsUri)
                    				.map(URI::toString)
                    				.collect(Collectors.joining(System.lineSeparator()))).toString();
                    LOG.log(Level.ERROR, msg);
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
		LOG.log(Level.DEBUG, "URI=" + this.m3u8Uri);
		try {
			CompletableFuture<HttpResponse<Set<TsMedia>>> completableFuture = this.client
					.sendAsync(
							this.request
							, BodyHandlers.fromLineSubscriber(new HlsParserSubscriber(), HlsParserSubscriber::getMatchingLines, null));
			Set<TsMedia> responce = completableFuture.handle(this.getHandle()).get();
			responce.stream()
				.filter(media -> !this.media().contains(media))
				.forEach(media -> this.media().offer(media));
		} catch (InterruptedException e) {
			LOG.log(Level.ERROR, "HTTP送信クライアント実行中に割り込みを検知", e);
		} catch (ExecutionException e) {
			LOG.log(Level.ERROR, "HTTP送信クライアント実行中にエラーが発生", e);
		}
	}
}
