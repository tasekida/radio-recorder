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
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.model.TsMedia;
import cyou.obliquerays.media.model.TsMediaTool;

/**
 * HLS（HTTP Live Streaming）セグメントファイル（.ts）をダウンロードする処理<br>
 * セグメントファイル（.ts）を保存
 */
public class TsDownloader extends AbstractMediaDownloader<TsMedia> implements Runnable {
	/** ロガー */
    private static final Logger LOG = System.getLogger(TsDownloader.class.getName());

    /** HTTP送信クライアント */
    private final HttpClient client;

    /** ダウンロード済みのHLSセグメントファイル一覧 */
    private final Set<TsMedia> tsMedias = new HashSet<>(0);

    private final String tsWorkDir;

	/**
	 * HLS（HTTP Live Streaming）セグメントファイル（.ts）をダウンロードする処理を初期化
	 * @param _queue ダウンロード対象のHLSセグメントファイル情報一覧
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.Executor Executor}
	 */
	public TsDownloader(Queue<TsMedia> _queue, Executor _executor) {
		super(_queue);
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
	    this.tsWorkDir = TsMediaTool.getTsWorkDir();
	}

	/**
	 * HTTP送信クライアント実行結果のハンドリング
	 * @return HTTP送信クライアント実行結果をハンドリングする関数
	 */
	private BiFunction<HttpResponse<Path>, Throwable, Path> getHandle() {
		return (response, e) -> {
			if (Optional.ofNullable(e).isPresent()) {
				LOG.log(Level.ERROR, "HTTP #RESPONSE=ERROR", e);
				return null;
			} else {
                StringBuilder msg = new StringBuilder("HTTP #RESPONSE=")
                		.append(response.statusCode()).append("  #BODY=").append(response.body());
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                	LOG.log(Level.INFO, msg.toString());
                    return response.body();
                } else {
                	LOG.log(Level.ERROR, msg.toString());
                    return null;
                }
			}
		};
	}

	/**
	 * HLS（HTTP Live Streaming）セグメントファイルをダウンロードして保存
	 */
	@Override
	public void run() {
		try {
			if (!this.media().isEmpty()) {
				TsMedia tsMedia = this.media().poll();
				if (null != tsMedia) {
					Path tsPath = TsMediaTool.tsUriToTsPath(this.tsWorkDir, tsMedia.getTsUri());
					if (Files.notExists(tsPath)) {
						LOG.log(Level.DEBUG, "URI=" + tsMedia.getTsUri());
						HttpRequest request = HttpRequest.newBuilder()
				        		.uri(tsMedia.getTsUri())
				        		.timeout(Duration.ofSeconds(60L))
				        		.header("Content-Type", "video/MP2T")
				        		.GET()
				        		.build();
				        tsPath = this.client
								.sendAsync(request, BodyHandlers.ofFile(tsPath))
								.handle(this.getHandle())
								.get();
				        tsMedia.setTsPath(tsPath);
				        tsMedias.add(tsMedia);
					}
				}
			}
		} catch (InterruptedException e) {
			LOG.log(Level.ERROR, "HTTP送信クライアント実行中に割り込みを検知", e);
		} catch (ExecutionException e) {
			LOG.log(Level.ERROR, "HTTP送信クライアント実行中にエラーが発生", e);
		}
	}

	/**
	 * ダウンロード済みのHLSセグメントファイル一覧を取得
	 * @return ダウンロード済みのHLSセグメントファイル一覧
	 */
	public Set<TsMedia> getTsMedias() {
		return this.tsMedias;
	}
}
