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
package cyou.obliquerays.media.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.model.TsMedia;

/**
 * HLS（HTTP Live Streaming）セグメントファイル（.ts）を結合する処理<br>
 * 音声ファイル（.mp3）を保存
 */
public class TsEncoder {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(TsEncoder.class.getName());

    /** エンコード後のMP3ファイル */
    private final Path mp3path;

    /** エンコード対象のメディア */
    private final List<TsMedia> tsMedias;

	/**
	 * コンストラクタ
	 * @param _tsMedias ダウンロード済みのHLSセグメントファイル一覧
	 */
	public TsEncoder(List<TsMedia> _tsMedias) {
		this.tsMedias = Objects.requireNonNull(_tsMedias);
		if (this.tsMedias.isEmpty()) throw new IllegalArgumentException("'tsMedias' must not be empty");
		this.mp3path = Path.of(
				RadioProperties.getProperties().getSaveDir()
				, RadioProperties.getProperties().getFilename())
				.toAbsolutePath().normalize();
	}

	/**
	 * FFMPEGのパラメータ取得
	 * @return FFMPEGのパラメータ
	 */
	private List<String> getEncodingAttributes() {
		List<String> attrs = new ArrayList<>(0);
		attrs.add("ffmpeg");
		attrs.add("-threads");
		attrs.add("2");
		this.tsMedias.stream()
			.map(TsMedia::getTsPath)
			.sorted((p1, p2) -> {
				String[] arr1 = p1.getFileName().toString().split("-");
				String[] arr2 = p2.getFileName().toString().split("-");
				int index = arr1.length < arr2.length ? arr1.length : arr2.length;
				for (int ret = 0, i = 0; i < index; i++) {
					if (arr1[i].length() == arr2[i].length()) {
						ret = arr1[i].compareTo(arr2[i]);
					} else {
						int padding = arr1[i].length() < arr2[i].length() ? arr2[i].length() : arr1[i].length();
						String str1 = arr1[i].length() < padding ?  String.format("%" + padding + "s", arr1[i]).replace(" ", "0") : arr1[i];
						String str2 = arr2[i].length() < padding ?  String.format("%" + padding + "s", arr2[i]).replace(" ", "0") : arr2[i];
						ret = str1.compareTo(str2);
					}
					if (ret != 0) {
						return ret;
					}
				}
				return 0;
			})
			.forEach(f -> {
				attrs.add("-i");
				attrs.add(f.toAbsolutePath().normalize().toString());
			});
		attrs.add("-vn");
		attrs.add("-filter_complex");
		attrs.add("concat=n=" + this.tsMedias.size() + ":v=0:a=1");
		attrs.add("-write_xing");
		attrs.add("0");
		attrs.add("-ab");
		attrs.add("320000");
		attrs.add("-ar");
		attrs.add("48000");
		attrs.add("-ac");
		attrs.add("2");
		attrs.add("-acodec");
		attrs.add("libmp3lame");
		attrs.add("-f");
		attrs.add("mp3");
		attrs.add("-threads");
		attrs.add("2");
		attrs.add("-y");
		attrs.add(this.mp3path.toString());
	 	LOGGER.log(Level.CONFIG, attrs.toString());
	 	return attrs;
	}

	/**
	 * NHKラジオをMP3へエンコード
	 * @return エンコード結果のMP3ファイル
	 * @throws IOException FFMPEG起動失敗
	 * @throws InterruptedException FFMPEG実行中にスレッド割り込み
	 * @throws ExecutionException FFMPEG実行失敗
	 */
	public Path record() throws IOException, InterruptedException, ExecutionException {

		Process ffmpeg = null;
		try {
			ProcessBuilder ffmpegBuilder = new ProcessBuilder(this.getEncodingAttributes());
			ffmpegBuilder.directory(new File(RadioProperties.getProperties().getSaveDir()));
			ffmpegBuilder.redirectErrorStream(true);
			ffmpeg = ffmpegBuilder.start();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "FFMPEGの起動に失敗", e);
			throw e;
		}

		try (Stream<String> lines = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream(), StandardCharsets.UTF_8)).lines()) {
			lines.forEach(s -> LOGGER.log(Level.INFO, s));
			Future<Boolean> result = ffmpeg.onExit().thenApply(p -> p.exitValue() == 0);
			if (result.get()) {
				LOGGER.log(Level.INFO, "MP3エンコード終了 exitCode = " + ffmpeg.exitValue());
			} else {
				LOGGER.log(Level.SEVERE, "MP3エンコード終了 exitCode = " + ffmpeg.exitValue());
			}
		} catch (InterruptedException | ExecutionException e) {
	    	LOGGER.log(Level.SEVERE, "MP3エンコードを中断", e);
	    	throw e;
	    } finally {
	    	ffmpeg.destroyForcibly();
	    }

		return this.mp3path;
	}
}
