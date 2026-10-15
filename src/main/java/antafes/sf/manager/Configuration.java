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

package antafes.sf.manager;

import antafes.sf.manager.language.English;
import antafes.sf.manager.language.German;
import antafes.utilities.BaseConfiguration;
import antafes.utilities.Utilities;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.UUID;

@Component
public class Configuration extends BaseConfiguration
{
    private static final String DATA_PATH = "data/";
    private static final String SAVE_PATH = "saves/";

    @Override
    public String getBasePath()
    {
        return System.getProperty("user.home") + "/.sfmanager/";
    }

    @Override
    public LanguageInterface getLanguage()
    {
        return Language.valueOf(this.getProperties().getProperty("language", Language.ENGLISH.toString()));
    }

    public String getLastOpenedFactoryId()
    {
        return this.getProperties().getProperty("lastOpenedFactoryId");
    }

    public File getLastImportDirectory()
    {
        String path = this.getProperties().getProperty("lastImportDirectory");

        if (path == null) {
            return null;
        }

        File directory = new File(path);

        return directory.isDirectory() ? directory : null;
    }

    public Dimension getWindowSize()
    {
        String width = this.getProperties().getProperty("windowWidth");
        String height = this.getProperties().getProperty("windowHeight");
        if (width == null || height == null) {
            return null;
        }

        try {
            int parsedWidth = Integer.parseInt(width);
            int parsedHeight = Integer.parseInt(height);

            return parsedWidth > 0 && parsedHeight > 0 ? new Dimension(parsedWidth, parsedHeight) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public void setWindowSize(Dimension size)
    {
        this.getProperties().setProperty("windowWidth", String.valueOf(size.width));
        this.getProperties().setProperty("windowHeight", String.valueOf(size.height));
    }

    public void setLastImportDirectory(File directory)
    {
        if (directory == null) {
            this.getProperties().remove("lastImportDirectory");
        } else {
            this.getProperties().setProperty("lastImportDirectory", directory.getAbsolutePath());
        }
    }

    @Override
    public void loadProperties()
    {
        super.loadProperties();

        if (!this.getProperties().containsKey("language")) {
            this.setLanguage(Language.ENGLISH);
        }
    }

    public void setLastOpenedFactoryId(UUID factoryId)
    {
        if (factoryId == null) {
            this.getProperties().remove("lastOpenedFactoryId");
        } else {
            this.getProperties().setProperty("lastOpenedFactoryId", factoryId.toString());
        }
    }

    @Override
    public File getOpenDirPath()
    {
        return new File(this.getBasePath() + SAVE_PATH);
    }

    @Override
    public File getSaveDirPath()
    {
        return new File(this.getBasePath() + SAVE_PATH);
    }

    @Override
    public void setOpenDirPath(String path)
    {
        throw new UnsupportedOperationException("Setting the open directory path is not supported.");
    }

    @Override
    public void setSaveDirPath(String path)
    {
        throw new UnsupportedOperationException("Setting the save directory path is not supported.");
    }

    @Override
    public File getSaveDirPath(String filename)
    {
        if (!filename.endsWith(".xml")) {
            filename = filename + ".xml";
        }

        String path = this.getSaveDirPath().getAbsolutePath();
        return new File(path + "/" + filename);
    }

    public static String getDataPath()
    {
        return DATA_PATH;
    }

    @Getter
    public enum Language implements LanguageInterface
    {
        ENGLISH(English.class.getName(), "English", "English", "images/english.png"),
        GERMAN(German.class.getName(), "German", "Deutsch", "images/german.png");

        private final String languageString;
        private final String name;
        private final String displayName;
        private final ImageIcon icon;

        Language(String languageString, String name, String displayName, String iconPath) {
            this.languageString = languageString;
            this.name = name;
            this.displayName = displayName;
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(Utilities.getResourceInJar(iconPath));
            this.icon = new ImageIcon(img);
        }
    }
}
