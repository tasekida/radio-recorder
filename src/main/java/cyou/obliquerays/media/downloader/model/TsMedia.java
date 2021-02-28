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
package cyou.obliquerays.media.downloader.model;

import java.net.URI;
import java.nio.file.Path;

/**
 * HLSセグメントファイルのPOJO
 */
public class TsMedia {

	/** HLSセグメントファイルのURI */
	private final URI tsUri;

	/** HLSセグメントファイルのPATH */
	private Path tsPath;

	/**
	 * コンストラクタ
	 * @param _tsUri HLSセグメントファイルのURI
	 */
	public TsMedia(URI _tsUri) {
		this.tsUri = _tsUri;
	}

	/**
	 * HLSセグメントファイルのURIを取得
	 * @return tsUri HLSセグメントファイルのURI
	 */
	public URI getTsUri() {
		return this.tsUri;
	}

	/**
	 * HLSセグメントファイルのPATHを取得
	 * @return tsPath HLSセグメントファイルのPATH
	 */
	public Path getTsPath() {
		return this.tsPath;
	}

	/**
	 * HLSセグメントファイルのPATHを設定
	 * @param _tsPath HLSセグメントファイルのPATH
	 */
	public void setTsPath(Path _tsPath) {
		this.tsPath = _tsPath;
	}

	@Override
	public int hashCode() {
		return (this.tsUri == null) ? 0 : this.tsUri.hashCode();
	}

	@Override
	public boolean equals(Object _obj) {
		if (this == _obj)
			return true;
		if (this.tsUri == null || _obj == null)
			return false;
		if (!(_obj instanceof TsMedia))
			return false;
		TsMedia other = (TsMedia) _obj;
		if (this.tsUri.equals(other.tsUri))
			return true;
		return false;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append("TsMedia [tsUri=").append(tsUri)
				.append(", tsPath=").append(tsPath)
				.append("]").toString();
	}
}
