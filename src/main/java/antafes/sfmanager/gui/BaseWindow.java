package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.SFManager;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.gui.event.*;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import antafes.sfmanager.service.event.ProductionSavedEvent;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Lazy
public class BaseWindow extends JFrame
{
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ProductionService productionService;

    private LanguageInterface language;
    private JTabbedPane panel;
    private JLabel activeFactoryLabel;
    private JMenuItem openFactoryMenuItem;
    private JMenu fileMenu;
    private JMenuItem closeMenuItem;
    private JMenuItem newProductionMenuItem;
    private JMenuItem newFactoryMenuItem;

    public BaseWindow()
    {
    }

    @PostConstruct
    private void initializeWindow()
    {
        this.language = this.configuration.getLanguageObject();
        this.initComponents();
        this.registerEvents();
        this.init();
        this.setFieldTexts();
    }

    private void initComponents()
    {
        this.createMenu();

        this.activeFactoryLabel = new JLabel();
        this.activeFactoryLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        this.panel = new JTabbedPane();
        this.refreshResourceTabs();
        JPanel contentPanel = new JPanel();
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(contentPanel);
        contentPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(this.activeFactoryLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
                .addComponent(this.panel, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(this.activeFactoryLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(this.panel, javax.swing.GroupLayout.DEFAULT_SIZE, 654, Short.MAX_VALUE)
        );
        this.setContentPane(contentPanel);
        this.updateActiveFactoryLabel();

        pack();
    }

    private void createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        this.fileMenu = new JMenu();
        this.closeMenuItem = new JMenuItem();
        this.newFactoryMenuItem = new JMenuItem();
        this.openFactoryMenuItem = new JMenuItem();
        this.newProductionMenuItem = new JMenuItem();

        this.fileMenu.add(this.newFactoryMenuItem);
        this.fileMenu.add(this.openFactoryMenuItem);
        this.fileMenu.add(this.newProductionMenuItem);
        this.fileMenu.add(this.closeMenuItem);
        menuBar.add(this.fileMenu);

        this.newFactoryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        this.newFactoryMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new NewFactoryEvent()));

        this.openFactoryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        this.openFactoryMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new OpenFactoryEvent()));
        this.openFactoryMenuItem.setEnabled(!this.factoryService.getFactories().isEmpty());

        this.newProductionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        this.newProductionMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new NewProductionEvent()));
        this.newProductionMenuItem.setEnabled(this.factoryService.getCurrentFactory() != null);

        this.closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        this.closeMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new CloseProgramEvent()));

        this.setJMenuBar(menuBar);
    }

    private void registerEvents()
    {
        SFManager.getDispatcher().addListener(
            CloseProgramEvent.class,
            new CloseProgramListener((closeProgramEvent) -> this.closeProgram())
        );
        SFManager.getDispatcher().addListener(
            NewProductionEvent.class,
            new NewProductionListener(newProductionEvent -> this.openNewProductionDialog())
        );
        SFManager.getDispatcher().addListener(
            NewFactoryEvent.class,
            new NewFactoryListener(newFactoryEvent -> this.openNewFactoryDialog())
        );
        SFManager.getDispatcher().addListener(
            OpenFactoryEvent.class,
            new OpenFactoryListener(openFactoryEvent -> this.openFactoryDialog())
        );
        SFManager.getDispatcher().addListener(
            RegisterEscapeCloseOperationEvent.class,
            new RegisterEscapeCloseOperationListener((event) -> this.registerEscapeCloseOperation(event.getDialog()))
        );
    }

    private void openNewProductionDialog()
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            return;
        }

        int x, y, width, height;

        NewProductionDialog dialog = this.applicationContext.getBean(
            NewProductionDialog.class, this, true
        );
        dialog.setVisible(false);

        width = dialog.getWidth();
        height = dialog.getHeight();
        x = this.configuration.getWindowLocation().x + (this.getWidth() / 2 - width / 2);
        y = this.configuration.getWindowLocation().y + (this.getHeight() / 2 - height / 2);

        dialog.setBounds(x, y, width, height);
        dialog.setVisible(true);
    }

    private void openNewFactoryDialog()
    {
        NewFactoryDialog dialog = this.applicationContext.getBean(
            NewFactoryDialog.class, this, true
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        this.openFactoryMenuItem.setEnabled(!this.factoryService.getFactories().isEmpty());
    }

    private void openFactoryDialog()
    {
        if (this.factoryService.getFactories().isEmpty()) {
            this.openFactoryMenuItem.setEnabled(false);
            return;
        }

        OpenFactoryDialog dialog = this.applicationContext.getBean(
            OpenFactoryDialog.class, this, true
        );
        dialog.setVisible(true);
    }

    @EventListener
    private void onFactorySelected(FactorySelectedEvent event)
    {
        SwingUtilities.invokeLater(() -> {
            Factory currentFactory = this.factoryService.getCurrentFactory();
            this.newProductionMenuItem.setEnabled(currentFactory != null);
            this.updateActiveFactoryLabel();
            this.refreshResourceTabs();
        });
    }

    @EventListener
    private void onProductionSaved(ProductionSavedEvent event)
    {
        SwingUtilities.invokeLater(this::refreshResourceTabs);
    }

    private void refreshResourceTabs()
    {
        this.panel.removeAll();
        FactoryOverviewPanel overviewPanel = this.applicationContext.getBean(FactoryOverviewPanel.class);
        overviewPanel.refresh();
        this.panel.addTab(this.language.translate("overview"), overviewPanel);
        for (Item item : this.productionService.getResourceItemsWithProductions()) {
            String title = item.getName() == null
                ? item.getKey()
                : item.getName().getLocalizedName(this.configuration.getLanguage().getName());
            ResourceProductionPanel resourceProductionPanel = this.applicationContext.getBean(
                ResourceProductionPanel.class,
                item
            );
            this.panel.addTab(title, resourceProductionPanel);
        }
    }

    private void updateActiveFactoryLabel()
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        this.activeFactoryLabel.setText(currentFactory == null
            ? this.language.translate("noFactoryOpen")
            : currentFactory.getName());
    }

    private void registerEscapeCloseOperation(JDialog dialog)
    {
        Action dispatchClosing = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispatchEvent(new WindowEvent(
                    dialog, WindowEvent.WINDOW_CLOSING
                ));
            }
        };
        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        String dispatchWindowClosingActionMapKey = "com.spodding.tackline.dispatch:WINDOW_CLOSING";
        JRootPane root = dialog.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            escapeStroke, dispatchWindowClosingActionMapKey
        );
        root.getActionMap().put(dispatchWindowClosingActionMapKey, dispatchClosing);
    }

    private void closeProgram()
    {
        this.configuration.setWindowLocation(this.getLocationOnScreen());
        this.configuration.setExtendedState(this.getExtendedState());
        this.configuration.saveProperties();
        System.exit(0);
    }

    private void setFieldTexts()
    {
        this.fileMenu.setText(this.language.translate("file"));
        this.fileMenu.setMnemonic(this.language.translate("fileMnemonic").charAt(0));
        this.newFactoryMenuItem.setText(this.language.translate("newFactoryMenu"));
        this.newFactoryMenuItem.setMnemonic(this.language.translate("newFactoryMnemonic").charAt(0));
        this.openFactoryMenuItem.setText(this.language.translate("openFactoryMenu"));
        this.openFactoryMenuItem.setMnemonic(this.language.translate("openFactoryMnemonic").charAt(0));
        this.newProductionMenuItem.setText(this.language.translate("newProductionMenu"));
        this.newProductionMenuItem.setMnemonic(this.language.translate("newProductionMnemonic").charAt(0));
        this.closeMenuItem.setText(this.language.translate("quit"));
        this.closeMenuItem.setMnemonic(this.language.translate("quitMnemonic").charAt(0));
    }

    private void init()
    {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(BaseWindow.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setLocation(this.configuration.getWindowLocation());
        this.setExtendedState(this.configuration.getExtendedState());
    }
}
