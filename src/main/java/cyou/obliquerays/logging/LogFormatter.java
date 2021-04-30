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
package cyou.obliquerays.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * カスタムログフォーマッター<br>
 * コンテナでプロセスを実行するので標準出力で良かった為、java.util.loggingを使用<br>
 */
public class LogFormatter extends Formatter {

	/** ログのタイムスタンプフォーマット */
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn");

    /** 一般的なログレベルとのマッピング表 */
    private static final Map<Level, String> LOG_LEVEL = Map.of(
    		Level.SEVERE, "ERROR"
    		, Level.WARNING, "WARN"
    		, Level.INFO, "INFO"
    		, Level.CONFIG, "DEBUG"
    		, Level.FINE, "DEBUG"
    		, Level.FINER, "DEBUG"
    		, Level.FINEST, "DEBUG");

    /** ログ出力ホストアドレス */
    private static String HOSTNAME;
    static {
        try {
            HOSTNAME = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            HOSTNAME = "unknown_host";
        }
    }

    /** パッケージクラス名処理用の正規表現 */
    private static Pattern REGEX = Pattern.compile("\\.");

    /**
     *  指定されたログ・レコードをカスタム形式の文字列にフォーマット
     */
    @Override
    public String format(LogRecord record) {
    	String[] packageClass = REGEX.split(record.getSourceClassName());
        StringBuilder sb = new StringBuilder();
        sb.append(LocalDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault()).format(FORMATTER));
        sb.append(" ");
        sb.append(HOSTNAME);
        sb.append(" ");
        sb.append(LOG_LEVEL.get(record.getLevel()));
        sb.append(" ");
        sb.append("[");
        sb.append("Thread-" + record.getThreadID());
        sb.append("] ");
        sb.append("[");
        sb.append(packageClass[packageClass.length-1]);
        sb.append(" ");
        sb.append(record.getSourceMethodName());
        sb.append("] ");
        sb.append(formatMessage(record));
        sb.append(System.lineSeparator());
        if (record.getThrown() != null) {
            StringWriter stringWriter = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(stringWriter));
            sb.append(stringWriter.toString());
        }
        return sb.toString();
    }
}
