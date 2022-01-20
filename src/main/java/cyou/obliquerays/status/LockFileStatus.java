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
package cyou.obliquerays.status;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

/**
 * ファイルによるプログラム起動状態管理
 */
public class LockFileStatus extends Thread {
    /** ロガー */
    private static final Logger LOG = System.getLogger(LockFileStatus.class.getName());

    /** プロセス実行時存在ファイル */
    private final Path lockFile;

    /** プロセス実行時存在ファイル監視鍵 */
    private final WatchKey watchKey;

    /** メインスレッド */
    private final Thread mainThread;

	/**
	 * コンストラクター
	 * @param _thread mainスレッド
	 * @param _lockFile プロセス実行時存在ファイル
	 * @throws IOException プロセス実行時存在ファイル操作エラー
	 */
	public LockFileStatus(Thread _thread, Path _lockFile) throws IOException {
		LOG.log(Level.DEBUG, "開始");
		this.mainThread = Objects.requireNonNull(_thread);
		this.lockFile = Objects.requireNonNull(_lockFile).toAbsolutePath().normalize();

		Files.createFile(this.lockFile);
		LOG.log(Level.INFO, "プロセス実行時存在ファイル作成#" + this.lockFile);

		WatchService watchService = FileSystems.getDefault().newWatchService();
		this.watchKey = this.lockFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
		LOG.log(Level.INFO, "プロセス実行時存在ファイル監視鍵取得#" + this.lockFile.getParent());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOG.log(Level.DEBUG, "シャットダウンフック開始");
			try {
				if (Files.deleteIfExists(this.lockFile))
					LOG.log(Level.INFO, "プロセス実行時存在ファイル削除#" + this.lockFile);
			} catch (IOException e) {
				LOG.log(Level.ERROR, "プロセス実行時存在ファイル削除失敗#" + this.lockFile, e);
			}
			LOG.log(Level.DEBUG, "シャットダウンフック終了");
		}));
		LOG.log(Level.INFO, "プロセス実行時存在ファイル削除用シャットダウンフック登録");

		LOG.log(Level.DEBUG, "終了");
	}

	@Override
	public void run() {
		LOG.log(Level.DEBUG, "開始");
		if (this.watchKey.isValid()) {
			this.watchKey.pollEvents().stream()
			.filter((watchEvent) -> watchEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE)
			.map((watchEvent) -> ((Path) watchEvent.context()).toAbsolutePath().normalize())
			.filter((deleteFile) -> deleteFile.equals(this.lockFile))
			.forEach((deleteFile) -> {
				LOG.log(Level.INFO, "プロセス実行時存在ファイルの削除検知#" + deleteFile);
				this.watchKey.cancel();
				this.mainThread.interrupt();
			});
		} else {
			this.mainThread.interrupt();
		}
		LOG.log(Level.DEBUG, "終了");
	}
}
