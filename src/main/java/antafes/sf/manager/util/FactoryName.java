/*
 * This file is part of SFManager.
 *
 * SFManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SFManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SFManager. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package SFManager
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2026, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

package antafes.sf.manager.util;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@UtilityClass
public final class FactoryName
{
    private final Map<Character, String> TRANSLITERATIONS = Map.ofEntries(
        Map.entry('ß', "ss"),
        Map.entry('æ', "ae"),
        Map.entry('œ', "oe"),
        Map.entry('ø', "o"),
        Map.entry('ł', "l"),
        Map.entry('đ', "d"),
        Map.entry('ð', "d"),
        Map.entry('þ', "th"),
        Map.entry('ı', "i")
    );

    public String toDirectoryName(String name)
    {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Factory name must not be blank.");
        }

        String lowerCase = name.toLowerCase(Locale.ROOT);
        StringBuilder transliterated = new StringBuilder();
        lowerCase.codePoints().forEach(codePoint -> {
            String replacement = codePoint <= Character.MAX_VALUE
                ? TRANSLITERATIONS.get((char) codePoint)
                : null;
            transliterated.append(replacement == null ? new String(Character.toChars(codePoint)) : replacement);
        });

        String decomposed = Normalizer.normalize(transliterated, Normalizer.Form.NFKD);
        StringBuilder directoryName = new StringBuilder();
        boolean separatorPending = false;
        for (int offset = 0; offset < decomposed.length();) {
            int codePoint = decomposed.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.getType(codePoint) == Character.NON_SPACING_MARK
                || Character.getType(codePoint) == Character.COMBINING_SPACING_MARK
                || Character.getType(codePoint) == Character.ENCLOSING_MARK) {
                continue;
            }

            if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= '0' && codePoint <= '9')) {
                if (separatorPending && !directoryName.isEmpty()) {
                    directoryName.append('-');
                }
                directoryName.appendCodePoint(codePoint);
                separatorPending = false;
            } else {
                separatorPending = !directoryName.isEmpty();
            }
        }

        if (directoryName.isEmpty()) {
            throw new IllegalArgumentException("Factory name must contain at least one letter or digit.");
        }
        return directoryName.toString();
    }
}
