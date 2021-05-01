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
package cyou.obliquerays.media.model;

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
