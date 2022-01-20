/**
 *  Copyright 2021 takahiro
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
package cyou.obliquerays.media.model;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import cyou.obliquerays.media.config.RadioProperties;

/**
 * HLSセグメントファイルを操作するクラス
 */
public class TsMediaTool {
    /** ロガー */
    private static final Logger LOG = System.getLogger(TsMediaTool.class.getName());

    /** HLS（HTTP Live Streaming）インデックスファイル（.m3u8）の正規表現 */
	private static final Pattern M3U8_FILE_PATERN = Pattern.compile("^.+\\.m3u8$");

    /** URI文字列分割用の正規表現 */
	private static final Pattern URI_SPLIT_PATERN = Pattern.compile("/");

	/**
	 * セグメントファイル（.ts）のURIからセグメントファイル（.ts）の保存ファイルパスを生成
	 * @param _tsWorkDir セグメントファイル（.ts）の保存ディレクトリ
	 * @param _tsUri セグメントファイル（.ts）取得先のURI
	 * @return セグメントファイル（.ts）の保存ファイルパス
	 */
	public static synchronized Path tsUriToTsPath(String _tsWorkDir, URI _tsUri) {
		LOG.log(Level.DEBUG, "tsUri=" + _tsUri);
		String[] arrUri = URI_SPLIT_PATERN.split(_tsUri.getPath());
		String path = new StringBuilder()
				.append(_tsWorkDir)
				.append("/").append(arrUri[arrUri.length-2])
				.append("-").append(arrUri[arrUri.length-1]).toString();
		Path tspath = Path.of(path).toAbsolutePath().normalize();
		LOG.log(Level.DEBUG, "tspath=" + tspath);
		return tspath;
	}

	/**
	 * HLS（HTTP Live Streaming）インデックスファイル（.m3u8）のURIから<br>
	 * HLS（HTTP Live Streaming）の基底となるURIを取得
	 * @return HLS（HTTP Live Streaming）の基底となるURI
	 */
	public static synchronized URI getBaseURI() {
		URI m3u8Uri = RadioProperties.getProperties().getRadio();

		StringTokenizer st = new StringTokenizer(m3u8Uri.getPath(), "/");
		StringBuilder uriBasePath = new StringBuilder();
		while (st.hasMoreElements()) {
			String str = st.nextToken();
			if (M3U8_FILE_PATERN.matcher(str).matches()) {
				uriBasePath.append("/");
			} else {
				uriBasePath.append("/").append(str);
			}
		}
		try {
			URI tsUri = new URI(m3u8Uri.getScheme(), m3u8Uri.getUserInfo(), m3u8Uri.getHost()
					, m3u8Uri.getPort(), uriBasePath.toString(), m3u8Uri.getQuery(), m3u8Uri.getFragment());
			LOG.log(Level.DEBUG, "tsUri=" + tsUri);
			return tsUri;
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * ダウンロードしたTSファイルを保存する作業ディレクトリ絶対パスの文字列を取得
	 * @return ダウンロードしたTSファイルを保存する作業ディレクトリ絶対パスの文字列
	 */
	public static synchronized String getTsWorkDir() {
		String baseDir = RadioProperties.getProperties().getBaseDir();
		String dailyDir = "";
    	try {
    		dailyDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    	} catch (IllegalArgumentException | DateTimeException e) {
    		// ignore
    	}
		return Path.of(baseDir, dailyDir).toAbsolutePath().normalize().toString();
	}

	/**
	 * 録音ファイル名[prefix]-[name].[suffix]の絶対パスを取得
	 * @return 録音ファイル名[prefix]-[name].[suffix]の絶対パス
	 */
	public static synchronized Path getMp3FilePath () {
		String baseDir = RadioProperties.getProperties().getBaseDir();
		String mp3FilePrefix = Objects.requireNonNull(RadioProperties.getProperties().getMp3FilePrefix());
    	try {
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mp3FilePrefix);
    		mp3FilePrefix = LocalDate.now().format(formatter);
    	} catch (IllegalArgumentException | DateTimeException e) {
    		// ignore
    	}
    	String mp3FileName = Objects.requireNonNull(RadioProperties.getProperties().getMp3FileName());
    	try {
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mp3FileName);
    		mp3FileName = LocalDate.now().format(formatter);
    	} catch (IllegalArgumentException | DateTimeException e) {
    		// ignore
    	}
    	String mp3FileSuffix = Objects.requireNonNull(RadioProperties.getProperties().getMp3FileSuffix());
    	StringBuilder mp3File =
    			new StringBuilder(mp3FilePrefix).append("-")
    			.append(mp3FileName).append(".").append(mp3FileSuffix);
		return Path.of(baseDir, mp3File.toString()).toAbsolutePath().normalize();
	}

	/**
	 * 録音一次ファイル名[prefix]-[name].[suffix]の絶対パスを取得
	 * @return 録音一次ファイル名[prefix]-[name].[suffix]の絶対パス
	 */
	public static synchronized Path getMp3TempPath () {
		String baseDir = RadioProperties.getProperties().getBaseDir();
		String mp3FilePrefix = Objects.requireNonNull(RadioProperties.getProperties().getMp3FilePrefix());
    	try {
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mp3FilePrefix);
    		mp3FilePrefix = LocalDate.now().format(formatter);
    	} catch (IllegalArgumentException | DateTimeException e) {
    		// ignore
    	}
    	String mp3FileName = Objects.requireNonNull(RadioProperties.getProperties().getMp3FileName());
    	try {
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mp3FileName);
    		mp3FileName = LocalDate.now().format(formatter);
    	} catch (IllegalArgumentException | DateTimeException e) {
    		// ignore
    	}
    	String mp3TempSuffix = Objects.requireNonNull(RadioProperties.getProperties().getMp3TempSuffix());
    	StringBuilder mp3File =
    			new StringBuilder(mp3FilePrefix).append("-")
    			.append(mp3FileName).append(".").append(mp3TempSuffix);
		return Path.of(baseDir, mp3File.toString()).toAbsolutePath().normalize();
	}

	/**
	 * TsMediaのCollectionをPATHでソートするルールを取得
	 * @return TsMediaのCollectionをPATHでソートするルール
	 */
	public static synchronized Comparator<TsMedia> getPathComparator() {
		return (tsMedia1, tsMedia2) -> {
			StringTokenizer st1 = new StringTokenizer(tsMedia1.getTsPath().getFileName().toString(), "-");
			StringTokenizer st2 = new StringTokenizer(tsMedia2.getTsPath().getFileName().toString(), "-");

			int ret = 0;
			while (st1.hasMoreElements() && st2.hasMoreElements()) {
				String str1 = st1.nextToken();
				String str2 = st2.nextToken();
				if (str1.length() == str2.length()) {
					ret = str1.compareTo(str2);
				} else {
					int padding = Math.max(str1.length(), str2.length());
					str1 = str1.length() < padding ?  String.format("%" + padding + "s", str1).replace(" ", "0") : str1;
					str2 = str2.length() < padding ?  String.format("%" + padding + "s", str2).replace(" ", "0") : str2;
					ret = str1.compareTo(str2);
				}
				if (ret != 0) {
					return ret;
				}
			}
			return ret;
		};
	}

	/**
	 * TsMediaのCollectionをURIでソートするルールを取得
	 * @return TsMediaのCollectionをURIでソートするルール
	 */
	public static synchronized Comparator<TsMedia> getURIComparator() {
		return (tsMedia1, tsMedia2) -> {
			StringTokenizer st1 = new StringTokenizer(tsMedia1.getTsUri().getPath().toString(), "/");
			StringTokenizer st2 = new StringTokenizer(tsMedia2.getTsUri().getPath().toString(), "/");

			int ret = 0;
			while (st1.hasMoreElements() && st2.hasMoreElements()) {
				String str1 = st1.nextToken();
				String str2 = st2.nextToken();
				if (str1.length() == str2.length()) {
					ret = str1.compareTo(str2);
				} else {
					int padding = Math.max(str1.length(), str2.length());
					str1 = str1.length() < padding ?  String.format("%" + padding + "s", str1).replace(" ", "0") : str1;
					str2 = str2.length() < padding ?  String.format("%" + padding + "s", str2).replace(" ", "0") : str2;
					ret = str1.compareTo(str2);
				}
				if (ret != 0) {
					return ret;
				}
			}
			return ret;
		};
	}
}
