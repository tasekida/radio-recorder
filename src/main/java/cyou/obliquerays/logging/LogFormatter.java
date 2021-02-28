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
package cyou.obliquerays.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * カスタムログフォーマッター<br>
 * コンテナでプロセスを実行するので標準出力で良かった為、java.util.loggingを使用<br>
 */
public class LogFormatter extends Formatter {

	/** ログのタイムスタンプフォーマット */
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnn");

    /** 一般的なログレベルとのマッピング表 */
    private static final Map<Level, String> loglevel = Collections.unmodifiableMap(new HashMap<>() {{
        put(Level.SEVERE,  "ERROR");
        put(Level.WARNING, "WARN");
        put(Level.INFO,    "INFO");
        put(Level.CONFIG,  "DEBUG");
        put(Level.FINE,    "DEBUG");
        put(Level.FINER,   "DEBUG");
        put(Level.FINEST,  "DEBUG");
    }});

    /** ログ出力ホストアドレス */
    private static String hostname;
    static {
        try {
            hostname = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown_host";
        }
    }

    /**
     *  指定されたログ・レコードをカスタム形式の文字列にフォーマット
     */
    @Override
    public String format(LogRecord record) {
    	String[] packageClass = record.getSourceClassName().split("\\.");
        StringBuilder sb = new StringBuilder();
        sb.append(LocalDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault()).format(formatter));
        sb.append(" ");
        sb.append(hostname);
        sb.append(" ");
        sb.append(loglevel.get(record.getLevel()));
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
