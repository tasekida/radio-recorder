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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import cyou.obliquerays.media.model.TsMedia;
import cyou.obliquerays.media.model.TsMediaTool;

/**
 * HLS（HTTP Live Streaming）セグメントファイル（.ts）を結合する処理<br>
 * 音声ファイル（.mp3）を保存
 */
public class TsEncoder {
    /** ロガー */
    private static final Logger LOG = System.getLogger(TsEncoder.class.getName());

    /** エンコード後のMP3ファイル */
    private final Path mp3path;

    /** エンコード中のMP3ファイル */
    private final Path mp3temp;

    /** エンコード対象のメディア */
    private final List<TsMedia> tsMedias;

	/**
	 * コンストラクタ
	 * @param _tsMedias ダウンロード済みのHLSセグメントファイル一覧
	 */
	public TsEncoder(List<TsMedia> _tsMedias) {
		this.tsMedias = Objects.requireNonNull(_tsMedias);
		if (this.tsMedias.isEmpty()) throw new IllegalArgumentException("'tsMedias' must not be empty");
		this.mp3path = TsMediaTool.getMp3FilePath();
		this.mp3temp = TsMediaTool.getMp3TempPath();
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
			.sorted(TsMediaTool.getPathComparator())
			.map(TsMedia::getTsPath)
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
		attrs.add(this.mp3temp.toString());
		LOG.log(Level.DEBUG, attrs.toString());
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
			ffmpegBuilder.directory(new File(TsMediaTool.getTsWorkDir()));
			ffmpegBuilder.redirectErrorStream(true);
			ffmpeg = ffmpegBuilder.start();
		} catch (IOException e) {
			LOG.log(Level.ERROR, "FFMPEGの起動に失敗", e);
			throw e;
		}

		try (Stream<String> lines = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream(), StandardCharsets.UTF_8)).lines()) {
			lines.forEach(s -> LOG.log(Level.INFO, s));
			Future<Boolean> result = ffmpeg.onExit().thenApply(p -> p.exitValue() == 0);
			if (result.get()) {
				LOG.log(Level.INFO, "MP3エンコード終了 exitCode = " + ffmpeg.exitValue());
			} else {
				LOG.log(Level.ERROR, "MP3エンコード終了 exitCode = " + ffmpeg.exitValue());
			}
		} catch (InterruptedException | ExecutionException e) {
			LOG.log(Level.ERROR, "MP3エンコードを中断", e);
	    	throw e;
	    } finally {
	    	ffmpeg.destroyForcibly();
	    }

		Files.move(this.mp3temp, this.mp3path, StandardCopyOption.ATOMIC_MOVE);

		return this.mp3path;
	}
}
