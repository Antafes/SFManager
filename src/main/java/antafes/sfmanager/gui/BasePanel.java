package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.utilities.language.LanguageInterface;
import lombok.AccessLevel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class BasePanel extends JPanel
{
    private final Map<Component, Boolean> enteredFields = new HashMap<>();
    private final JDialog parent;
    @Getter(AccessLevel.PROTECTED)
    protected LanguageInterface language;

    public BasePanel(JDialog parent, Configuration configuration)
    {
        this.parent = parent;
        this.language = configuration.getLanguageObject();
    }

    protected JDialog getParentDialog()
    {
        return this.parent;
    }

    protected BasePanel addField(Component field)
    {
        this.enteredFields.put(field, Boolean.FALSE);

        return this;
    }

    protected boolean checkFieldsFilled()
    {
        return !this.enteredFields.containsValue(Boolean.FALSE);
    }

    abstract protected void init();
    abstract protected void setFieldTexts();
    abstract protected void createFocusTraversalPolicy();
}
