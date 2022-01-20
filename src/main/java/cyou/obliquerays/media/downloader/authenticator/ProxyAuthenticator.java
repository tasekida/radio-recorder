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
package cyou.obliquerays.media.downloader.authenticator;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Proxy認証情報の管理
 */
public class ProxyAuthenticator extends Authenticator {
    /** ロガー */
    private static final Logger LOG = System.getLogger(ProxyAuthenticator.class.getName());

	/** Proxyアカウント */
	private final String proxyUser;

	/** Proxyパスワード */
	private final String proxyPasswd;

	/**
	 * コンストラクタ
	 * @param _proxyUser Proxyアカウント
	 * @param _proxyPasswd Proxyパスワード
	 */
	public ProxyAuthenticator(String _proxyUser, String _proxyPasswd) {
		this.proxyUser = _proxyUser;
		this.proxyPasswd = _proxyPasswd;
	}

	/**
	 * Proxy認証情報を取得
	 * @return Proxy認証情報
	 */
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		if (this.getRequestorType() == RequestorType.PROXY) {
			LOG.log(Level.DEBUG, this.getRequestingURL().toString());
			char[] passwd = this.getProxyPasswd().toCharArray();
			LOG.log(Level.DEBUG, this.getProxyUser());
			return new PasswordAuthentication(this.getProxyUser(), passwd);
		}
		return null;
	}

	/** @return proxyUser Proxyアカウント */
	public String getProxyUser() {
		return this.proxyUser;
	}

	/** @return proxyPasswd Proxyパスワード */
	public String getProxyPasswd() {
		return this.proxyPasswd;
	}
}
