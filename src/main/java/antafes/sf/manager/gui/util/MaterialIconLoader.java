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

package antafes.sf.manager.gui.util;

import antafes.sf.manager.entity.Item;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches scaled item icons from the classpath.
 */
public final class MaterialIconLoader
{
    public static final int ICON_SIZE = 40;
    public static final String FALLBACK_ICON_PATH = "images/items/uranium_pellet.png";

    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    private MaterialIconLoader()
    {
    }

    public static Icon load(Item item)
    {
        return load(item == null ? null : item.getIcon(), ICON_SIZE);
    }

    public static Icon load(String path, int size)
    {
        String effectivePath = path == null || path.isBlank() ? FALLBACK_ICON_PATH : path;
        return CACHE.computeIfAbsent(effectivePath + "@" + size, key -> {
            BufferedImage image = read(effectivePath);
            if (image == null) {
                image = read(FALLBACK_ICON_PATH);
            }
            if (image == null) {
                return new ImageIcon(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB));
            }

            return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        });
    }

    private static BufferedImage read(String path)
    {
        try (InputStream stream = MaterialIconLoader.class.getClassLoader().getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }
}
