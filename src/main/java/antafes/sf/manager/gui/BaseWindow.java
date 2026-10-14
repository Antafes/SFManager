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

package antafes.sf.manager.gui;

import antafes.sf.manager.Configuration;
import antafes.sf.manager.SFManager;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.gui.event.*;
import antafes.sf.manager.gui.order.LocalizedBuildingOrder;
import antafes.sf.manager.gui.order.LocalizedItemOrder;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ImportService;
import antafes.sf.manager.service.dto.ImportResult;
import antafes.sf.manager.service.exception.UnsupportedSaveVersionException;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import antafes.sf.manager.service.event.ProductionDeletedEvent;
import antafes.sf.manager.service.event.ProductionSavedEvent;
import antafes.sf.manager.service.event.StorageChangedEvent;
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
import java.io.IOException;
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
    @Autowired
    private ImportService importService;

    private LanguageInterface language;
    private JTabbedPane panel;
    private JLabel activeFactoryLabel;
    private JMenuItem openFactoryMenuItem;
    private JMenu fileMenu;
    private JMenu optionsMenu;
    private JMenu languageMenu;
    private JMenuItem closeMenuItem;
    private JMenuItem newProductionMenuItem;
    private JMenuItem newStorageMenuItem;
    private JMenuItem newFactoryMenuItem;
    private JMenuItem importFactoryMenuItem;
    private JCheckBoxMenuItem useStorageMenuItem;

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
        this.newStorageMenuItem = new JMenuItem();
        this.importFactoryMenuItem = new JMenuItem();
        this.optionsMenu = new JMenu();
        this.useStorageMenuItem = new JCheckBoxMenuItem();
        this.languageMenu = new JMenu();

        this.fileMenu.add(this.newFactoryMenuItem);
        this.fileMenu.add(this.openFactoryMenuItem);
        this.fileMenu.add(this.newProductionMenuItem);
        this.fileMenu.add(this.newStorageMenuItem);
        this.fileMenu.addSeparator();
        this.fileMenu.add(this.importFactoryMenuItem);
        this.fileMenu.addSeparator();
        this.fileMenu.add(this.closeMenuItem);
        menuBar.add(this.fileMenu);
        this.optionsMenu.add(this.useStorageMenuItem);
        menuBar.add(this.optionsMenu);
        ButtonGroup languageGroup = new ButtonGroup();
        for (Configuration.Language availableLanguage : Configuration.Language.values()) {
            JRadioButtonMenuItem languageMenuItem = new JRadioButtonMenuItem(
                availableLanguage.getDisplayName(),
                availableLanguage.getIcon(),
                this.configuration.getLanguage() == availableLanguage
            );
            languageMenuItem.addActionListener(event -> this.setLanguage(availableLanguage));
            languageGroup.add(languageMenuItem);
            this.languageMenu.add(languageMenuItem);
        }
        this.languageMenu.setIcon(this.configuration.getLanguage().getIcon());
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(this.languageMenu);

        this.newFactoryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        this.newFactoryMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new NewFactoryEvent()));

        this.openFactoryMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        this.openFactoryMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new OpenFactoryEvent()));
        this.openFactoryMenuItem.setEnabled(!this.factoryService.getFactories().isEmpty());

        this.newProductionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        this.newProductionMenuItem.addActionListener(e -> SFManager.getDispatcher().dispatch(new NewProductionEvent()));
        this.newProductionMenuItem.setEnabled(this.factoryService.getCurrentFactory() != null);

        this.newStorageMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        this.newStorageMenuItem.addActionListener(e -> this.openNewStorageDialog());
        this.updateStorageMenuItem();

        this.importFactoryMenuItem.addActionListener(e -> this.importFactory());
        this.useStorageMenuItem.setSelected(this.isStorageEnabled());
        this.useStorageMenuItem.addActionListener(e -> this.setUseStorage(this.useStorageMenuItem.isSelected()));

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
        dialog.setLocationRelativeTo(this);
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

    private void openNewStorageDialog()
    {
        if (!this.isStorageEnabled()) {
            return;
        }
        NewStorageDialog dialog = this.applicationContext.getBean(NewStorageDialog.class, this, true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
            this.updateStorageMenuItem();
            this.updateActiveFactoryLabel();
            this.refreshResourceTabs();
        });
    }

    @EventListener
    private void onProductionSaved(ProductionSavedEvent event)
    {
        SwingUtilities.invokeLater(this::refreshResourceTabs);
    }

    @EventListener
    private void onProductionDeleted(ProductionDeletedEvent event)
    {
        SwingUtilities.invokeLater(this::refreshResourceTabs);
    }

    @EventListener
    private void onStorageChanged(StorageChangedEvent event)
    {
        SwingUtilities.invokeLater(this::refreshResourceTabs);
    }

    private void refreshResourceTabs()
    {
        String selectedTabTitle = this.getSelectedTabTitle();
        this.panel.removeAll();
        FactoryOverviewPanel overviewPanel = this.applicationContext.getBean(FactoryOverviewPanel.class);
        overviewPanel.refresh();
        this.panel.addTab(this.language.translate("overview"), overviewPanel);
        if (this.isStorageEnabled()) {
            this.panel.addTab(
                this.language.translate("storage"),
                this.applicationContext.getBean(StoragePanel.class)
            );
        }
        String selectedLanguage = this.configuration.getLanguage().getName();
        this.productionService.getResourceItemsWithProductions().stream()
            .sorted(LocalizedItemOrder.comparator(selectedLanguage))
            .forEach(item -> this.panel.addTab(
                item.getName() == null ? item.getKey() : item.getName().getLocalizedName(selectedLanguage),
                this.applicationContext.getBean(ResourceProductionPanel.class, item)
            ));
        LocalizedBuildingOrder.addTabs(
            this.panel,
            this.productionService.getGeneratorBuildingsWithProductions(),
            selectedLanguage,
            building -> this.applicationContext.getBean(GeneratorProductionPanel.class, building)
        );
        this.productionService.getRecipeItemsWithProductions().stream()
            .sorted(LocalizedItemOrder.alphabeticalComparator(selectedLanguage))
            .forEach(item -> this.panel.addTab(
                item.getName() == null ? item.getKey() : item.getName().getLocalizedName(selectedLanguage),
                this.applicationContext.getBean(RecipeProductionPanel.class, item)
            ));
        this.selectTab(selectedTabTitle);
    }

    private String getSelectedTabTitle()
    {
        int selectedIndex = this.panel.getSelectedIndex();
        return selectedIndex < 0 ? null : this.panel.getTitleAt(selectedIndex);
    }

    private void selectTab(String title)
    {
        if (title == null) {
            return;
        }
        for (int index = 0; index < this.panel.getTabCount(); index++) {
            if (title.equals(this.panel.getTitleAt(index))) {
                this.panel.setSelectedIndex(index);
                return;
            }
        }
    }

    private void updateActiveFactoryLabel()
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        this.activeFactoryLabel.setText(currentFactory == null
            ? this.language.translate("noFactoryOpen")
            : currentFactory.getName());
    }

    private void setUseStorage(boolean useStorage)
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            this.useStorageMenuItem.setSelected(false);
            return;
        }
        currentFactory.setUseStorage(useStorage);
        try {
            this.factoryService.saveFactory(currentFactory);
        } catch (IOException exception) {
            currentFactory.setUseStorage(!useStorage);
            this.useStorageMenuItem.setSelected(!useStorage);
            JOptionPane.showMessageDialog(
                this,
                this.language.translate("storageSavingFailed"),
                this.language.translate("storage"),
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        this.updateStorageMenuItem();
        this.refreshResourceTabs();
    }

    private void setLanguage(Configuration.Language selectedLanguage)
    {
        if (this.configuration.getLanguage() == selectedLanguage) {
            return;
        }
        this.configuration.setLanguage(selectedLanguage);
        this.configuration.saveProperties();
        this.language = this.configuration.getLanguageObject();
        this.languageMenu.setIcon(selectedLanguage.getIcon());
        this.setFieldTexts();
        this.updateActiveFactoryLabel();
        this.refreshResourceTabs();
    }

    private void importFactory()
    {
        JFileChooser chooser = new JFileChooser(this.configuration.getLastImportDirectory());
        chooser.setDialogTitle(this.language.translate("importFactoryMenu"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            this.language.translate("importSaveFileFilter"),
            "sav"
        ));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        this.configuration.setLastImportDirectory(chooser.getCurrentDirectory());
        this.configuration.saveProperties();
        java.nio.file.Path path = chooser.getSelectedFile().toPath();
        this.importFactoryMenuItem.setEnabled(false);
        new SwingWorker<ImportResult, Void>()
        {
            @Override
            protected ImportResult doInBackground() throws Exception
            {
                return BaseWindow.this.importService.readSave(path);
            }

            @Override
            protected void done()
            {
                BaseWindow.this.importFactoryMenuItem.setEnabled(true);
                try {
                    BaseWindow.this.finishImport(this.get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException exception) {
                    if (exception.getCause() instanceof UnsupportedSaveVersionException unsupported) {
                        BaseWindow.this.showImportError(String.format(
                            BaseWindow.this.language.translate("importUnsupportedVersion"),
                            unsupported.getSaveVersion()
                        ));
                        return;
                    }
                    BaseWindow.this.showImportError(
                        BaseWindow.this.language.translate("importParseError")
                            + "\n" + exception.getCause().getMessage()
                    );
                }
            }
        }.execute();
    }

    private void finishImport(ImportResult result)
    {
        if (result.productions().isEmpty()) {
            this.showImportError(
                this.language.translate("importNothingFound") + "\n"
                    + String.format(this.language.translate("importSkippedCount"), result.skippedMachines().size())
            );
            return;
        }

        String message = String.format(
            this.language.translate("importSummary"),
            result.productions().size(),
            result.skippedMachines().size()
        );
        Object name = JOptionPane.showInputDialog(
            this,
            message,
            this.language.translate("importFactoryMenu"),
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            result.saveName()
        );

        if (name == null) {
            return;
        }

        try {
            this.importService.importFactory(result, name.toString().trim());
            this.openFactoryMenuItem.setEnabled(true);
        } catch (IllegalArgumentException exception) {
            this.showImportError(this.language.translate("importInvalidName") + "\n" + exception.getMessage());
        } catch (IOException | RuntimeException exception) {
            Logger.getLogger(BaseWindow.class.getName()).log(Level.SEVERE, null, exception);
            this.showImportError(this.language.translate("importFailed") + "\n" + exception.getMessage());
        }
    }

    private void showImportError(String message)
    {
        JOptionPane.showMessageDialog(
            this,
            message,
            this.language.translate("importFactoryMenu"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void updateStorageMenuItem()
    {
        this.newStorageMenuItem.setVisible(
            this.isStorageEnabled()
        );
        this.useStorageMenuItem.setSelected(this.isStorageEnabled());
    }

    private boolean isStorageEnabled()
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        return currentFactory != null && currentFactory.isUseStorage();
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
        this.setTitle(this.language.translate("title"));
        this.fileMenu.setText(this.language.translate("file"));
        this.fileMenu.setMnemonic(this.language.translate("fileMnemonic").charAt(0));
        this.newFactoryMenuItem.setText(this.language.translate("newFactoryMenu"));
        this.newFactoryMenuItem.setMnemonic(this.language.translate("newFactoryMnemonic").charAt(0));
        this.openFactoryMenuItem.setText(this.language.translate("openFactoryMenu"));
        this.openFactoryMenuItem.setMnemonic(this.language.translate("openFactoryMnemonic").charAt(0));
        this.newProductionMenuItem.setText(this.language.translate("newProductionMenu"));
        this.newProductionMenuItem.setMnemonic(this.language.translate("newProductionMnemonic").charAt(0));
        this.newStorageMenuItem.setText(this.language.translate("newStorageMenu"));
        this.newStorageMenuItem.setMnemonic(this.language.translate("newStorageMnemonic").charAt(0));
        this.importFactoryMenuItem.setText(this.language.translate("importFactoryMenu"));
        this.importFactoryMenuItem.setMnemonic(this.language.translate("importFactoryMnemonic").charAt(0));
        this.optionsMenu.setText(this.language.translate("options"));
        this.optionsMenu.setMnemonic(this.language.translate("optionsMnemonic").charAt(0));
        this.useStorageMenuItem.setText(this.language.translate("useStorage"));
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
