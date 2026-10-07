package antafes.sfmanager.gui.order;

import antafes.sfmanager.entity.Item;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@UtilityClass
public class LocalizedItemOrder
{
    public Comparator<Item> comparator(String language)
    {
        Comparator<Item> alphabeticalComparator = LocalizedItemOrder.alphabeticalComparator(language);
        return (left, right) -> {
            int rawResourceOrder = Boolean.compare(isRawResource(right), isRawResource(left));
            return rawResourceOrder != 0 ? rawResourceOrder : alphabeticalComparator.compare(left, right);
        };
    }

    public Comparator<Item> alphabeticalComparator(String language)
    {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            "German".equalsIgnoreCase(language) ? "de" : "en"
        ));
        collator.setStrength(Collator.SECONDARY);
        return Comparator.<Item, String>comparing(
            item -> LocalizedItemOrder.displayName(item, language),
            collator
        ).thenComparing(item -> Objects.toString(item.getKey(), ""));
    }

    public boolean isRawResource(Item item)
    {
        return item.getProductions() != null && !item.getProductions().isEmpty();
    }

    public void addTabs(
        JTabbedPane tabs,
        String overviewTitle,
        Component overviewPanel,
        List<Item> items,
        String language,
        Function<Item, String> titleProvider,
        Function<Item, Component> panelProvider
    )
    {
        tabs.addTab(overviewTitle, overviewPanel);
        items.stream()
            .sorted(comparator(language))
            .forEach(item -> tabs.addTab(titleProvider.apply(item), panelProvider.apply(item)));
    }

    private String displayName(Item item, String language)
    {
        return item.getName() == null
            ? Objects.toString(item.getKey(), "")
            : item.getName().getLocalizedName(language);
    }
}
