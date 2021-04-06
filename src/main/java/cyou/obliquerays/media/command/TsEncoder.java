/**
 *  Copyright 2021 tasekida
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.model.TsMedia;
import cyou.obliquerays.media.jave2.NhkEncoder;
import ws.schild.jave.MultimediaObject;

/**
 *
 */
public class TsEncoder {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(NhkEncoder.class.getName());

    /** エンコード後のMP3ファイル */
    private final Path mp3path;

    /** エンコード対象のメディア */
    private final List<MultimediaObject> media;

	/**
	 * コンストラクタ
	 * @param _tsMedias ダウンロード済みのHLSセグメントファイル一覧
	 */
	public TsEncoder(List<TsMedia> _tsMedias) {
		Objects.requireNonNull(_tsMedias);
		if (_tsMedias.isEmpty()) throw new IllegalArgumentException("'tsMedias' must not be empty");
		this.media = _tsMedias.stream()
				.sorted((m1, m2) -> m1.getTsPath().compareTo(m2.getTsPath()))
				.map(media -> new MultimediaObject(media.getTsPath().toFile()))
				.collect(Collectors.toList());
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
//		attrs.add("cmd");
//		attrs.add("/C");
		attrs.add("ffmpeg");
		attrs.add("-threads");
		attrs.add("2");
		this.media.stream()
			.map(MultimediaObject::getFile)
			.sorted((s1, s2) -> s1.toPath().compareTo(s2.toPath()))
			.forEach(f -> {
				attrs.add("-i");
				attrs.add(f.toString());
			});
		attrs.add("-vn");
		attrs.add("-filter_complex");
		attrs.add("concat=n=" + this.media.size() + ":v=0:a=1");
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
	 */
	public Path record() {

		ProcessBuilder ffmpegBuilder = new ProcessBuilder(this.getEncodingAttributes());
		ffmpegBuilder.directory(new File(RadioProperties.getProperties().getSaveDir()));
		ffmpegBuilder.redirectErrorStream(true);
		Process ffmpeg = null;
		try {
			ffmpeg = ffmpegBuilder.start();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "FFMPEGの起動に失敗", e);
		}

		String line = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream(), StandardCharsets.UTF_8))) {
		    while ((line = reader.readLine()) != null) {
				LOGGER.log(Level.INFO, line);
		    }
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "MP3エンコード中にエラーが発生" + line, e);
		}

	    try {
	        ffmpeg.waitFor();
	    } catch (InterruptedException e) {
	    	LOGGER.log(Level.SEVERE, "MP3エンコードを中断", e);
	    }

		if (ffmpeg.exitValue() == 0) {
			LOGGER.log(Level.INFO, "MP3エンコード終了 exitCode = " + ffmpeg.exitValue());
		} else {
			LOGGER.log(Level.SEVERE, "MP3エンコード終了 exitCode = " + ffmpeg.exitValue());
		}

		return this.mp3path;
	}
}
