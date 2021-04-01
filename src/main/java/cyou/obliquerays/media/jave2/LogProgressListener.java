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
package cyou.obliquerays.media.jave2;

import java.util.logging.Level;
import java.util.logging.Logger;

import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

/**
 *
 */
public class LogProgressListener implements EncoderProgressListener {
	/** ロガー */
    private static final Logger LOGGER = Logger.getLogger(LogProgressListener.class.getName());

	public LogProgressListener() {
		LOGGER.log(Level.CONFIG, "");
	}

	public void message(String _msg) {
		LOGGER.log(Level.CONFIG, (null == _msg) ? "" : _msg);
	}

	public void progress(int p) {
	    //Find %100 progress
	    double progress = p / 10;
	    LOGGER.log(Level.CONFIG, progress + "%");
	}

	public void sourceInfo(MultimediaInfo _info) {
		LOGGER.log(Level.CONFIG, (null == _info) ? "" : _info.toString());
	}
}
