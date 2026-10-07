package antafes.sfmanager;

import antafes.sfmanager.language.English;
import antafes.sfmanager.language.German;
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
