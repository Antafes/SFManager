package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.SFManager;
import antafes.sfmanager.gui.event.RegisterEscapeCloseOperationEvent;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import jakarta.annotation.PostConstruct;
import antafes.utilities.gui.OrderFocusTraversalPolicy;
import antafes.utilities.language.LanguageInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Vector;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OpenFactoryDialog extends JDialog
{
    private final Frame owner;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    private LanguageInterface language;
    private JComboBox<Factory> factorySelect;

    public OpenFactoryDialog(Frame owner, boolean modal)
    {
        super(owner, modal);
        this.owner = owner;
    }

    @PostConstruct
    private void initializeDialog()
    {
        this.language = this.configuration.getLanguageObject();
        this.setTitle(this.language.translate("openFactory"));
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        SFManager.getDispatcher().dispatch(new RegisterEscapeCloseOperationEvent(this));

        this.factorySelect = new JComboBox<>();
        this.factoryService.getFactories().stream()
            .sorted(Comparator.comparing(Factory::getName, String.CASE_INSENSITIVE_ORDER))
            .forEach(this.factorySelect::addItem);
        this.factorySelect.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            )
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                this.setText(value instanceof Factory factory ? factory.getName() : "");
                return this;
            }
        });

        JButton openButton = new JButton(this.language.translate("open"));
        JButton cancelButton = new JButton(this.language.translate("cancel"));
        openButton.addActionListener(event -> this.openSelectedFactory());
        cancelButton.addActionListener(event -> this.dispose());
        this.getRootPane().setDefaultButton(openButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(openButton);
        buttons.add(cancelButton);
        this.setLayout(new BorderLayout(8, 8));
        this.add(this.factorySelect, BorderLayout.CENTER);
        this.add(buttons, BorderLayout.SOUTH);
        this.factorySelect.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        this.pack();
        this.setLocationRelativeTo(this.owner);

        Vector<Component> focusOrder = new Vector<>();
        focusOrder.add(this.factorySelect);
        focusOrder.add(openButton);
        focusOrder.add(cancelButton);
        this.setFocusTraversalPolicy(new OrderFocusTraversalPolicy(focusOrder));
        this.setFocusTraversalPolicyProvider(true);
    }

    private void openSelectedFactory()
    {
        Factory selectedFactory = (Factory) this.factorySelect.getSelectedItem();
        if (selectedFactory != null) {
            this.eventPublisher.publishEvent(new FactorySelectedEvent(selectedFactory));
            this.dispose();
        }
    }
}
