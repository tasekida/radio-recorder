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
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import cyou.obliquerays.media.config.RadioProperties;

/**
 * HLS（HTTP Live Streaming）セグメントファイル（.ts）を結合する処理<br>
 * 音声ファイル（.mp3）を保存
 */
public class NhkRecorder implements Callable<Path> {
    /** ロガー */
    private static final Logger LOG = System.getLogger(NhkRecorder.class.getName());

    /** エンコード後のMP3ファイル */
    private final Path mp3path;

	/**
	 * コンストラクタ
	 */
	public NhkRecorder() {
		this.mp3path = RadioProperties.getProperties().getMp3FilePath();
	}

	/**
	 * FFMPEGのパラメータ取得
	 * @return FFMPEGのパラメータ
	 */
	private List<String> getEncodingAttributes() {
		LocalTime start = RadioProperties.getProperties().getStart();
		LocalTime end = RadioProperties.getProperties().getEnd();
		Duration duration = Duration.between(start, end);
		
		List<String> attrs = new ArrayList<>(0);
		attrs.add("ffmpeg");
		attrs.add("-i");
		attrs.add(RadioProperties.getProperties().getRadio().toString());
		attrs.add("-vn");
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
		attrs.add("-t");
		attrs.add(String.valueOf(duration.getSeconds()));
		attrs.add("-y");
		attrs.add(this.mp3path.toString());
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
			ffmpegBuilder.directory(Path.of(RadioProperties.getProperties().getBaseDir()).toAbsolutePath().normalize().toFile());
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

		return this.mp3path;
	}

	@Override
	public Path call() throws Exception {
		Path mp3path = this.record();
		return mp3path;
	}
}
