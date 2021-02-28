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

import java.util.Optional;
import java.util.function.Function;

import ws.schild.jave.encode.ArgType;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.ValueArgument;

/**
 *
 */
public class AudioConcatArgument extends ValueArgument {

	private static final String ARGUMENT_NAME = "-filter_complex";

	private static final String KEY_NAME = "concat";

	private static final String FORMAT = "n=%s:v=0:a=1";

	public AudioConcatArgument() {
		super(ArgType.OUTFILE, ARGUMENT_NAME, valueGetter());
	}

	private static Function<EncodingAttributes, Optional<String>> valueGetter() {
		return ea -> {
			return ea.getExtraContext().entrySet().stream()
					.filter(m -> m.getKey().equals(KEY_NAME))
					.map(e -> {
						return new StringBuilder().append(e.getKey()).append("=")
								.append(String.format(FORMAT, e.getValue())).toString();
					}).findFirst();
		};
	}
}
