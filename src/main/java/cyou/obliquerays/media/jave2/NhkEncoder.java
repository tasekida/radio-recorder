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
package cyou.obliquerays.media.jave2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cyou.obliquerays.media.config.RadioProperties;
import cyou.obliquerays.media.downloader.model.TsMedia;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

/**
 * Jave2を使用してNHKラジオを保存
 */
public class NhkEncoder {
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
	public NhkEncoder(List<TsMedia> _tsMedias) {
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
	 * コンストラクタ
	 * @param _nhkStream ダウンロード済みのHLSストリームURI
	 * @throws MalformedURLException URL変換エラー
	 */
	public NhkEncoder(URI _nhkStream) throws MalformedURLException {
		Objects.requireNonNull(_nhkStream);
		this.media = List.of(new MultimediaObject(_nhkStream.toURL()));
		this.mp3path = Path.of(
				RadioProperties.getProperties().getSaveDir()
				, RadioProperties.getProperties().getFilename())
				.toAbsolutePath().normalize();
	}

	/**
	 * FFMPEGのパラメータ取得
	 * @return FFMPEGのパラメータ
	 */
	private EncodingAttributes getEncodingAttributes() {

	 	AudioAttributes audio =
	 			new AudioAttributes()// 音声パラメータ
	 			.setCodec("libmp3lame")// LAME
	 			.setSamplingRate(48000)// サンプルレート 44.1KHz
	 			.setBitRate(320000)// CBR 320Kbps
	 			.setChannels(2);// ステレオ

	 	EncodingAttributes attrs =
	 			new EncodingAttributes()// エンコードパラメータ
	 			.setDecodingThreads(2)// デコーダー並列数
	 			.setEncodingThreads(2)// エンコーダー並列数
	 			.setAudioAttributes(audio)// 音声パラメータ
	 			.setExtraContext(Map.of("concat", Integer.toString(this.media.size())))
	 			.setOutputFormat("mp3");// 出力フォーマット

	 	LOGGER.log(Level.INFO, attrs.toString());
	 	return attrs;
	}

	private void deleteMultimediaObjects() {
		this.media.stream()
				.filter(media -> null != media.getFile())
				.map(media -> media.getFile().toPath().toAbsolutePath().normalize())
				.forEach((path) -> {
					try {
						Files.delete(path);
						LOGGER.log(Level.INFO, "delete = " + path.toString());
					} catch (IOException e) {
						LOGGER.log(Level.SEVERE, "ファイル削除失敗 = " + path.toString());
						throw new UncheckedIOException(e);
					}
				});
	}

	/**
	 * NHKラジオをMP3へエンコード
	 * @return エンコード結果のMP3ファイル
	 */
	public Path record() {
		try {
		 	Encoder.addOptionAtIndex(new AudioConcatArgument(), Integer.valueOf(0));
		 	Encoder encoder = new Encoder();
		 	encoder.encode(this.media, this.mp3path.toFile(), this.getEncodingAttributes(),new LogProgressListener());
		 	this.deleteMultimediaObjects();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "MP3エンコード中にエラーが発生", e);
		}

		return this.mp3path;
	}
}