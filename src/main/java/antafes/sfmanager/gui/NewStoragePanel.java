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

package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.Storage;
import antafes.sfmanager.gui.renderer.TableStyle;
import antafes.sfmanager.service.StorageService;
import antafes.sfmanager.service.dto.StorageProductionChoice;
import antafes.utilities.gui.OrderFocusTraversalPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewStoragePanel extends BasePanel
{
    @Autowired
    private StorageService storageService;

    private final String selectedLanguage;
    private final NumberFormat numberFormat;
    private DefaultListModel<StorageProductionChoice> choicesModel;
    private JLabel choicesLabel;
    private JList<StorageProductionChoice> choicesList;
    private JButton okButton;
    private JButton cancelButton;

    public NewStoragePanel(JDialog parent, Configuration configuration)
    {
        super(parent, configuration);
        this.selectedLanguage = configuration.getLanguage().getName();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            "German".equals(this.selectedLanguage) ? "de" : "en"
        ));
    }

    @PostConstruct
    private void initializePanel()
    {
        this.init();
    }

    @Override
    protected void init()
    {
        this.choicesModel = new DefaultListModel<>();
        this.storageService.getAvailableChoices().forEach(this.choicesModel::addElement);
        this.choicesLabel = new JLabel();
        this.choicesList = new JList<>(this.choicesModel);
        this.choicesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.choicesList.setCellRenderer((list, choice, index, selected, focused) -> {
            JLabel label = new JLabel(this.choiceDescription(choice));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(TableStyle.getRowColor(index));
                label.setForeground(list.getForeground());
            }
            return label;
        });
        this.okButton = new JButton();
        this.cancelButton = new JButton();

        this.setLayout(new BorderLayout(8, 8));
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        this.add(this.choicesLabel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(this.choicesList);
        scrollPane.setPreferredSize(new java.awt.Dimension(520, 220));
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.add(this.okButton);
        buttonPanel.add(this.cancelButton);
        this.add(buttonPanel, BorderLayout.SOUTH);

        this.okButton.addActionListener(event -> this.addStorageEntries());
        this.cancelButton.addActionListener(event -> this.getParentDialog().dispose());
        this.getParentDialog().getRootPane().setDefaultButton(this.okButton);
        this.setFieldTexts();
        this.createFocusTraversalPolicy();
    }

    @Override
    protected void setFieldTexts()
    {
        this.choicesLabel.setText(this.getLanguage().translate("storageChoices"));
        this.okButton.setText(this.getLanguage().translate("ok"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        Vector<Component> order = new Vector<>();
        order.add(this.choicesList);
        order.add(this.okButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new OrderFocusTraversalPolicy(order));
        this.setFocusTraversalPolicyProvider(true);
    }

    private void addStorageEntries()
    {
        try {
            List<Storage.Entry> entries = this.choicesList.getSelectedValuesList().stream()
                .map(choice -> new Storage.Entry()
                    .setProductionId(choice.production().getId())
                    .setItemKey(choice.item().getKey()))
                .toList();
            this.storageService.addStorageEntries(entries);
            this.getParentDialog().dispose();
        } catch (IOException | RuntimeException exception) {
            String message = Objects.toString(
                exception.getMessage(),
                this.getLanguage().translate("storageSavingFailed")
            );
            JOptionPane.showMessageDialog(
                this.getParentDialog(),
                message,
                this.getLanguage().translate("newStorage"),
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String choiceDescription(StorageProductionChoice choice)
    {
        return this.productionDescription(choice) + " — "
            + this.localizedName(choice.item().getName()) + ": "
            + this.numberFormat.format(choice.rate()) + " / min";
    }

    private String productionDescription(StorageProductionChoice choice)
    {
        List<String> context = new java.util.ArrayList<>();
        context.add(this.localizedName(choice.production().getSelectedBuilding().getName()));
        if (choice.production().getSelectedRecipe() != null) {
            context.add(this.localizedName(choice.production().getSelectedRecipe().getName()));
        }
        if (choice.production().getGroup() != null && !choice.production().getGroup().isBlank()) {
            context.add(choice.production().getGroup());
        }
        if (choice.production().getPosition() != null && !choice.production().getPosition().isBlank()) {
            context.add(choice.production().getPosition());
        }
        return String.join(" — ", context);
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }
}
