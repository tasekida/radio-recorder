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
package cyou.obliquerays.media.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
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
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		Path file = Path.of(_fileName).toAbsolutePath().normalize();
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
