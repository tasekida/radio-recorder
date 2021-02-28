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
package cyou.obliquerays.status;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルによるプログラム起動状態管理
 */
public class LockFileStatus extends Thread {
    /** ロガー */
    private static final Logger logger = Logger.getLogger(LockFileStatus.class.getName());

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
		logger.log(Level.CONFIG, "開始");
		this.mainThread = Objects.requireNonNull(_thread);
		this.lockFile = Objects.requireNonNull(_lockFile).toAbsolutePath().normalize();

		Files.createFile(this.lockFile);
		logger.log(Level.INFO, "プロセス実行時存在ファイル作成#" + this.lockFile);

		WatchService watchService = FileSystems.getDefault().newWatchService();
		this.watchKey = this.lockFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
		logger.log(Level.INFO, "プロセス実行時存在ファイル監視鍵取得#" + this.lockFile.getParent());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.log(Level.CONFIG, "シャットダウンフック開始");
			try {
				if (Files.deleteIfExists(this.lockFile))
					logger.log(Level.INFO, "プロセス実行時存在ファイル削除#" + this.lockFile);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "プロセス実行時存在ファイル削除失敗#" + this.lockFile, e);
			}
			logger.log(Level.CONFIG, "シャットダウンフック終了");
		}));
		logger.log(Level.INFO, "プロセス実行時存在ファイル削除用シャットダウンフック登録");

		logger.log(Level.CONFIG, "終了");
	}

	@Override
	public void run() {
		logger.log(Level.CONFIG, "開始");
		try {
			while (this.watchKey.isValid()) {
				this.watchKey.pollEvents().stream()
				.filter((watchEvent) -> watchEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE)
				.map((watchEvent) -> ((Path) watchEvent.context()).toAbsolutePath().normalize())
				.filter((deleteFile) -> deleteFile.equals(this.lockFile))
				.forEach((deleteFile) -> {
					logger.log(Level.INFO, "プロセス実行時存在ファイルの削除検知#" + deleteFile);
					this.watchKey.cancel();
					this.mainThread.interrupt();
				});
				if (this.watchKey.isValid())
					TimeUnit.SECONDS.sleep(1L);
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "待機失敗", e);
			this.mainThread.interrupt();
		}
		logger.log(Level.CONFIG, "終了");
	}
}
