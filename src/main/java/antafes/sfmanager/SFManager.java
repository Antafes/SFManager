package antafes.sfmanager;

import antafes.eventDispatcher.Application;
import antafes.sfmanager.gui.BaseWindow;
import antafes.sfmanager.gui.Splash;
import antafes.sfmanager.repository.*;
import antafes.sfmanager.service.FactoryService;
import antafes.utilities.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import javax.swing.*;
import java.awt.*;

@SpringBootApplication
@ComponentScan("antafes.sfmanager")
public class SFManager extends Application implements CommandLineRunner
{
    @Autowired
    private Configuration configuration;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private FactoryRepository factoryRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ProductionRepository productionRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private StorageRepository storageRepository;
    @Autowired
    private FactoryService factoryService;

    public static void main(String[] args)
    {
        new SpringApplicationBuilder(SFManager.class).headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception
    {
        this.configuration.loadProperties();
        this.openBaseWindow();
    }

    private void openBaseWindow()
    {
        SwingUtilities.invokeLater(() -> {
            this.warmUp();
            this.factoryService.restoreLastOpenedFactory();
            BaseWindow baseWindow = this.applicationContext.getBean(BaseWindow.class);
            new Splash(Utilities.getResourceInJar("images/splash.png"), 2000, baseWindow);
            Toolkit kit = Toolkit.getDefaultToolkit();
            Image img = kit.createImage(Utilities.getResourceInJar("images/logo.png"));
            baseWindow.setIconImage(img);
        });
    }

    private void warmUp()
    {
        this.buildingRepository.init();
        this.itemRepository.init();
        this.recipeRepository.init();
        this.factoryRepository.init();
        this.productionRepository.init();
        this.storageRepository.init();
    }
}
