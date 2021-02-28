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

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	 * @param _mp3path エンコード後のMP3ファイル
	 * @param _tsMedias ダウンロード済みのHLSセグメントファイル一覧
	 */
	public NhkEncoder(Path _mp3path, List<TsMedia> _tsMedias) {
		this.mp3path = Objects.requireNonNull(_mp3path).toAbsolutePath().normalize();
		Objects.requireNonNull(_tsMedias);
		if (_tsMedias.isEmpty()) throw new IllegalArgumentException("'tsMedias' must not be empty");

		this.media = _tsMedias.stream()
				.sorted((m1, m2) -> m1.getTsPath().compareTo(m2.getTsPath()))
				.map(media -> new MultimediaObject(media.getTsPath().toFile()))
				.collect(Collectors.toList());
	}

	/**
	 * コンストラクタ
	 * @param _mp3path エンコード後のMP3ファイル
	 * @param _nhkStream ダウンロード済みのHLSストリームURI
	 * @throws MalformedURLException URL変換エラー
	 */
	public NhkEncoder(Path _mp3path, URI _nhkStream) throws MalformedURLException {
		this.mp3path = Objects.requireNonNull(_mp3path).toAbsolutePath().normalize();
		Objects.requireNonNull(_nhkStream);

		this.media = List.of(new MultimediaObject(_nhkStream.toURL()));
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
//	 			.setOffset(30F)// 変換開始までの待機秒数
//	 			.setDuration(60F)// 変換開始から終了までの秒数
	 			.setDecodingThreads(2)// デコーダー並列数
	 			.setEncodingThreads(2)// エンコーダー並列数
	 			.setAudioAttributes(audio)// 音声パラメータ
	 			.setExtraContext(Map.of("concat", Integer.toString(this.media.size())))
	 			.setOutputFormat("mp3");// 出力フォーマット

	 	LOGGER.log(Level.INFO, attrs.toString());
	 	return attrs;
	}

	/**
	 * NHKラジオをMP3へエンコード
	 */
	public Path record() {
		try {
//		 	EncodingArgument concat = new ValueArgument(ArgType.OUTFILE, "-filter_complex",
//	                  ea -> Optional.ofNullable("concat=n=103:v=0:a=1"));

		 	Encoder.addOptionAtIndex(new AudioConcatArgument(), Integer.valueOf(0));
		 	Encoder encoder = new Encoder();
		 	encoder.encode(this.media, this.mp3path.toFile(), this.getEncodingAttributes(),new LogProgressListener());
		} catch (Exception ex) {
		 	ex.printStackTrace();
		}

		return this.mp3path;
	}
}