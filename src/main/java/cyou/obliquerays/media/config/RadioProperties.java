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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
	private final ProxySelector proxySelector;
	private final Authenticator proxyAuthenticator;
	private final URI radio;
	private final String filename;
	private final LocalTime start;
	private final LocalTime end;
	private final Set<DayOfWeek> dayOfWeeks = EnumSet.noneOf(DayOfWeek.class);

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
    	if (Boolean.parseBoolean(this.getProperty("proxy.enable"))) {
    		String proxyHost = Objects.requireNonNull(this.getProperty("proxy.host"));
    		int proxyPort = Integer.parseInt(Objects.requireNonNull(this.getProperty("proxy.port")));
    		InetSocketAddress inetSocketProxy = new InetSocketAddress(proxyHost, proxyPort);
    		this.proxySelector = inetSocketProxy.isUnresolved() ? HttpClient.Builder.NO_PROXY : ProxySelector.of(inetSocketProxy);

    		Optional<String> proxyAccount = Optional.ofNullable(this.getProperty("proxy.account"));
    		Optional<String> proxyPassword = Optional.ofNullable(this.getProperty("proxy.password"));
    		this.proxyAuthenticator = (proxyAccount.isPresent() && proxyPassword.isPresent())
    				? new ProxyAuthenticator(proxyAccount.get(), proxyPassword.get())
    				: Authenticator.getDefault();
    	} else {
    		this.proxySelector = HttpClient.Builder.NO_PROXY;
    		this.proxyAuthenticator = Authenticator.getDefault();
    	}

    	this.radio = URI.create(Objects.requireNonNull(this.getProperty("radio.uri")));

    	this.filename = Objects.requireNonNull(this.getProperty("mp3.file"));
    	this.start = LocalTime.parse(Objects.requireNonNull(this.getProperty("start.time")));
    	this.end = LocalTime.parse(Objects.requireNonNull(this.getProperty("end.time")));
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
	}

	/** @return プログラム起動モード */
	public boolean isProcess() {
		return this.process;
	}

	/** @return Proxy接続情報の管理 */
	public ProxySelector getProxySelector() {
		return this.proxySelector;
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
	public String getSaveDir() {
		String saveDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		return Path.of(this.getProperty("base.dir"), saveDir).toAbsolutePath().normalize().toString();
	}

	/** @return 録音ファイル名 */
	public String getFilename() {
		return this.filename;
	}

	/** @return 録音開始時間 */
	public LocalTime getStart() {
		return this.start;
	}

	/** @return 録音終了時間 */
	public LocalTime getEnd() {
		return this.end;
	}

	/** @return 録音する曜日 */
	public Set<DayOfWeek> getDayOfWeeks() {
		return this.dayOfWeeks;
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
