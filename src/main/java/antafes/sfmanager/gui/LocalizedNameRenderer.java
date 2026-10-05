package antafes.sfmanager.gui;

import antafes.sfmanager.entity.Name;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class LocalizedNameRenderer<T> implements ListCellRenderer<T>
{
    private final Function<? super T, Name> nameExtractor;
    private final String language;
    private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();

    public LocalizedNameRenderer(Function<? super T, Name> nameExtractor, String language)
    {
        this.nameExtractor = nameExtractor;
        this.language = language;
    }

    public String getDisplayName(T value)
    {
        if (value == null) {
            return "";
        }
        Name name = this.nameExtractor.apply(value);
        return name == null ? "" : name.getLocalizedName(this.language);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends T> list,
        T value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        JLabel label = (JLabel) this.renderer.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus
        );
        label.setText(this.getDisplayName(value));
        return label;
    }
}
