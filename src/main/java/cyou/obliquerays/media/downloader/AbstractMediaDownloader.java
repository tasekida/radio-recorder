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
package cyou.obliquerays.media.downloader;

import java.util.Queue;

/**
 * Queueを保持するスケルトン実装
 */
abstract class AbstractMediaDownloader<T> implements MediaDownloader<T> {

	/** 処理対象のQUEUE */
	private final Queue<T> queue;

	/**
	 * コンストラクタ
	 * @param _queue Queue
	 */
	AbstractMediaDownloader(final Queue<T> _queue) {
		this.queue = _queue;
	}

	@Override
	public Queue<T> media() {
		return this.queue;
	}
}
