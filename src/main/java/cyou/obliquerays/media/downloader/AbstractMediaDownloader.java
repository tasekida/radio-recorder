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
