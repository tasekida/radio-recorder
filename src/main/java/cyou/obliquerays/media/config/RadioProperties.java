/**
 *  Copyright (C) 2021 tasekida
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
package cyou.obliquerays.media.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import cyou.obliquerays.media.downloader.authenticator.ProxyAuthenticator;

/**
 * パラメータ一覧
 */
public final class RadioProperties extends Properties {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(RadioProperties.class.getName());

    /** プロパティファイル名 */
	private static final String PROPERTY_FILENAME = "radio.properties";

    /** パラメータ一覧 */
	private static RadioProperties PROP;

	private final boolean process;
	private final Set<DayOfWeek> dayOfWeeks = EnumSet.noneOf(DayOfWeek.class);
	private final LocalTime start;
	private final LocalTime end;
	private final URI radio;
	private final String mp3FilePrefix;
	private final String mp3FileName;
	private final String mp3FileSuffix;
	private final boolean proxy;
	private final boolean proxyAuth;
	private final ProxySelector proxySelector;
	private final Authenticator proxyAuthenticator;
	private final long startAdjustmentSeconds;
	private final long endAdjustmentSeconds;

	/** コンストラクタ */
	private RadioProperties() {
		LOGGER.log(Level.CONFIG, "RadioProperties");

    	try (InputStream in = this.getInputStream(PROPERTY_FILENAME)) {
    		this.load(in);
    	} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "設定ファイルの読み取りに失敗#" + PROPERTY_FILENAME, e);
			throw new UncheckedIOException(e);
    	}
    	this.process = Boolean.parseBoolean(this.getProperty("process"));
		if (Boolean.parseBoolean(this.getProperty("sunday")))
			this.dayOfWeeks.add(DayOfWeek.SUNDAY);
		if (Boolean.parseBoolean(this.getProperty("monday")))
			this.dayOfWeeks.add(DayOfWeek.MONDAY);
		if (Boolean.parseBoolean(this.getProperty("tuesday")))
			this.dayOfWeeks.add(DayOfWeek.TUESDAY);
		if (Boolean.parseBoolean(this.getProperty("wednesday")))
			this.dayOfWeeks.add(DayOfWeek.WEDNESDAY);
		if (Boolean.parseBoolean(this.getProperty("thursday")))
			this.dayOfWeeks.add(DayOfWeek.THURSDAY);
		if (Boolean.parseBoolean(this.getProperty("friday")))
			this.dayOfWeeks.add(DayOfWeek.FRIDAY);
		if (Boolean.parseBoolean(this.getProperty("saturday")))
			this.dayOfWeeks.add(DayOfWeek.SATURDAY);
    	this.start = LocalTime.parse(Objects.requireNonNull(this.getProperty("start.time")));
    	this.end = LocalTime.parse(Objects.requireNonNull(this.getProperty("end.time")));
    	this.radio = URI.create(Objects.requireNonNull(this.getProperty("radio.uri")));

    	this.mp3FilePrefix = Objects.requireNonNull(this.getProperty("mp3.file.prefix"));
    	this.mp3FileName = Objects.requireNonNull(this.getProperty("mp3.file.name"));
    	this.mp3FileSuffix = Objects.requireNonNull(this.getProperty("mp3.file.suffix"));

    	this.proxy = Boolean.parseBoolean(this.getProperty("proxy"));
    	if (this.proxy) {
    		String proxyHost = Objects.requireNonNull(this.getProperty("proxy.host"));
    		int proxyPort = Integer.parseInt(Objects.requireNonNull(this.getProperty("proxy.port")));
    		InetSocketAddress inetSocketProxy = new InetSocketAddress(proxyHost, proxyPort);
    		this.proxySelector = inetSocketProxy.isUnresolved() ? HttpClient.Builder.NO_PROXY : ProxySelector.of(inetSocketProxy);

    		Optional<String> proxyAccount = Optional.ofNullable(this.getProperty("proxy.account"));
    		Optional<String> proxyPassword = Optional.ofNullable(this.getProperty("proxy.password"));
    		if ((proxyAccount.isPresent() && proxyPassword.isPresent())) {
    			this.proxyAuth = true;
    			this.proxyAuthenticator = new ProxyAuthenticator(proxyAccount.get(), proxyPassword.get());
    		} else {
    			this.proxyAuth = false;
    			this.proxyAuthenticator = ProxyAuthenticator.getDefault();
    		}
    	} else {
    		this.proxyAuth = false;
    		this.proxySelector = HttpClient.Builder.NO_PROXY;
    		this.proxyAuthenticator = ProxyAuthenticator.getDefault();
    	}
    	this.startAdjustmentSeconds = Long.parseLong(Objects.requireNonNull(this.getProperty("start.adjustment.seconds")));
    	this.endAdjustmentSeconds = Long.parseLong(Objects.requireNonNull(this.getProperty("end.adjustment.seconds")));
	}

	/** @return プログラム起動モード */
	public boolean isProcess() {
		return this.process;
	}

	/** @return Proxy設定 */
	public boolean isProxy() {
		return this.proxy;
	}

	/** @return Proxy接続情報の管理 */
	public ProxySelector getProxySelector() {
		return this.proxySelector;
	}

	/** @return Proxy認証設定 */
	public boolean isProxyAuth() {
		return this.proxyAuth;
	}

	/** @return Proxy認証情報の管理 */
	public Authenticator getProxyAuthenticator() {
		return this.proxyAuthenticator;
	}

	/** @return RADIOストリーミングのURI */
	public URI getRadio() {
		return this.radio;
	}

	/** @return 録音ファイルディレクトリ */
	public String getBaseDir() {
		return this.getProperty("base.dir");
	}

	/**
	 * 録音ファイル名[prefix]-[name].[suffix]の[prefix]<br>
	 * DateTimeFormatterの解析用パターンをサポート
	 * @return 録音ファイル名の[prefix]
	 */
	public String getMp3FilePrefix() {
		return this.mp3FilePrefix;
	}

	/**
	 * 録音ファイル名[prefix]-[name].[suffix]の[name]<br>
	 * DateTimeFormatterの解析用パターンをサポート
	 * @return 録音ファイル名の[name]
	 */
	public String getMp3FileName() {
		return this.mp3FileName;
	}

	/**
	 * 録音ファイル名[prefix]-[name].[suffix]の[suffix]<br>
	 * DateTimeFormatterの解析用パターンをサポート
	 * @return 録音ファイル名の[suffix]
	 */
	public String getMp3FileSuffix() {
		return this.mp3FileSuffix;
	}

	/** @return 録音開始時間 */
	public LocalTime getStart() {
		return this.start.plusSeconds(this.getStartAdjustmentSeconds());
	}

	/** @return 録音終了時間 */
	public LocalTime getEnd() {
		return this.end.plusSeconds(this.getEndAdjustmentSeconds());
	}

	/** @return 録音する曜日 */
	public Set<DayOfWeek> getDayOfWeeks() {
		return this.dayOfWeeks;
	}

	/** @return 録音を開始時間の調整 */
	public long getStartAdjustmentSeconds() {
		return this.startAdjustmentSeconds;
	}

	/** @return 録音を終了時間の調整 */
	public long getEndAdjustmentSeconds() {
		return this.endAdjustmentSeconds;
	}

	/**
	 * ファイル読み取りストリームへのアクセス
	 * @param _fileName ファイル名
	 * @return ファイル読み取りストリーム
	 * @throws IOException 読み取りエラー
	 */
	private InputStream getInputStream(String _fileName) throws IOException {
		Objects.requireNonNull(_fileName);
		Path file = Path.of("/data/config", _fileName).toAbsolutePath().normalize();
		if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
			return Files.newInputStream(file, StandardOpenOption.READ);
		} else {
			return ClassLoader.getSystemResourceAsStream(_fileName);
		}
	}

	/** @return パラメータ一覧へアクセス */
	public static RadioProperties getProperties() {
		if (null == PROP) {
			synchronized (RadioProperties.class) {
				if (null == PROP) {
					PROP = new RadioProperties();
				}
			}
		}
		return PROP;
	}
}
