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

import java.util.Comparator;

@UtilityClass
public class NaturalOrder
{
    public final Comparator<String> COMPARATOR = Comparator.nullsLast(NaturalOrder::compare);

    public int compare(String left, String right)
    {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftChar = left.charAt(leftIndex);
            char rightChar = right.charAt(rightIndex);
            if (isDigit(leftChar) && isDigit(rightChar)) {
                int leftEnd = digitRunEnd(left, leftIndex);
                int rightEnd = digitRunEnd(right, rightIndex);
                int result = compareNumbers(left.substring(leftIndex, leftEnd), right.substring(rightIndex, rightEnd));
                if (result != 0) {
                    return result;
                }
                leftIndex = leftEnd;
                rightIndex = rightEnd;
                continue;
            }

            int result = Character.compare(
                Character.toLowerCase(Character.toUpperCase(leftChar)),
                Character.toLowerCase(Character.toUpperCase(rightChar))
            );
            if (result != 0) {
                return result;
            }
            leftIndex++;
            rightIndex++;
        }

        int lengthOrder = Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
        return lengthOrder != 0 ? lengthOrder : left.compareTo(right);
    }

    private boolean isDigit(char character)
    {
        return character >= '0' && character <= '9';
    }

    private int digitRunEnd(String value, int start)
    {
        int end = start;
        while (end < value.length() && isDigit(value.charAt(end))) {
            end++;
        }
        return end;
    }

    private int compareNumbers(String left, String right)
    {
        String leftNumber = stripLeadingZeros(left);
        String rightNumber = stripLeadingZeros(right);
        int lengthOrder = Integer.compare(leftNumber.length(), rightNumber.length());
        return lengthOrder != 0 ? lengthOrder : leftNumber.compareTo(rightNumber);
    }

    private String stripLeadingZeros(String value)
    {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }
}
