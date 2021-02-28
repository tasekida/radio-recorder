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
import java.util.logging.Level;
import java.util.logging.Logger;

import cyou.obliquerays.media.downloader.model.TsMedia;

/**
 * HLS（HTTP Live Streaming）セグメントファイル（.ts）をダウンロードする処理<br>
 * セグメントファイル（.ts）を保存
 */
public class TsDownloader extends AbstractMediaDownloader<TsMedia> implements Runnable {
	/** ロガー */
    private static final Logger LOGGER = Logger.getLogger(TsDownloader.class.getName());

    /** HTTP送信クライアント */
    private final HttpClient client;

    /** ダウンロード済みのHLSセグメントファイル一覧 */
    private final Set<TsMedia> tsMedias = new HashSet<>(0);

	/**
	 * HLS（HTTP Live Streaming）セグメントファイル（.ts）をダウンロードする処理を初期化
	 * @param _executor 内部で使用するHTTPクライアントを実行する{@linkplain java.util.concurrent.Executor Executor}
	 */
	public TsDownloader(Queue<TsMedia> _queue, Executor _executor) {
		super(_queue);
        this.client = HttpClient.newBuilder()
        		.version(Version.HTTP_2)
        		.followRedirects(Redirect.NORMAL)
        		.proxy(HttpClient.Builder.NO_PROXY)
        		.executor(_executor)
        		.build();
	}

	/**
	 * HTTP送信クライアント実行結果のハンドリング
	 * @return HTTP送信クライアント実行結果をハンドリングする関数
	 */
	private BiFunction<HttpResponse<Path>, Throwable, Path> getHandle() {
		return (response, e) -> {
			if (Optional.ofNullable(e).isPresent()) {
				LOGGER.log(Level.SEVERE, "HTTP #RESPONSE=ERROR", e);
				return null;
			} else {
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    LOGGER.log(Level.INFO, "HTTP #RESPONSE=" + response.statusCode());
                    return response.body();
                } else {
                    StringBuilder msg = new StringBuilder()
                    		.append("HTTP #RESPONSE=").append(response.statusCode())
                    		.append("#BODY=").append(response.body());
                    LOGGER.log(Level.SEVERE, msg.toString());
                    return null;
                }
			}
		};
	}

	/**
	 * HLS（HTTP Live Streaming）セグメントファイルをダウンロードして保存
	 * @return 保存したセグメントファイル（.ts）のPath
	 */
	@Override
	public void run() {
		try {
			if (!this.media().isEmpty()) {
				TsMedia tsMedia = this.media().poll();
				if (null != tsMedia) {
					Path tsPath = this.tsUriToTsPath(tsMedia.getTsUri());
					if (Files.notExists(tsPath)) {
						LOGGER.log(Level.CONFIG, "URI=" + tsMedia.getTsUri());
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
			LOGGER.log(Level.SEVERE, "HTTP送信クライアント実行中に割り込みを検知", e);
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "HTTP送信クライアント実行中にエラーが発生", e);
		}
	}

	/**
	 * セグメントファイル（.ts）取得先のURIからセグメントファイル（.ts）を保存するファイルパス生成する関数
	 * @param _target セグメントファイル（.ts）取得先のURI
	 * @return セグメントファイル（.ts）を保存するファイルパス生成する関数
	 */
	private Path tsUriToTsPath(URI _tsUri) {
		LOGGER.log(Level.CONFIG, "tsUri=" + _tsUri);
		String[] arrUri = _tsUri.getPath().split("/");
		String path = new StringBuilder()
				.append("./").append(arrUri[arrUri.length-2])
				.append("-").append(arrUri[arrUri.length-1]).toString();
		Path tspath = Path.of(path).toAbsolutePath().normalize();
		LOGGER.log(Level.CONFIG, "tspath=" + tspath);
		return tspath;
	}

	/**
	 * ダウンロード済みのHLSセグメントファイル一覧を取得
	 * @return tsMedias ダウンロード済みのHLSセグメントファイル一覧
	 */
	public Set<TsMedia> getTsMedias() {
		return this.tsMedias;
	}
}
