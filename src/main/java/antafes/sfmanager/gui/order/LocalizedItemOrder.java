package antafes.sfmanager.gui.order;

import antafes.sfmanager.entity.Item;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public final class LocalizedItemOrder
{
    private LocalizedItemOrder()
    {
    }

    public static Comparator<Item> comparator(String language)
    {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            "German".equalsIgnoreCase(language) ? "de" : "en"
        ));
        collator.setStrength(Collator.SECONDARY);
        return (left, right) -> {
            int rawResourceOrder = Boolean.compare(isRawResource(right), isRawResource(left));
            if (rawResourceOrder != 0) {
                return rawResourceOrder;
            }

            int displayNameOrder = collator.compare(displayName(left, language), displayName(right, language));
            if (displayNameOrder != 0) {
                return displayNameOrder;
            }
            return Objects.toString(left.getKey(), "").compareTo(Objects.toString(right.getKey(), ""));
        };
    }

    public static boolean isRawResource(Item item)
    {
        return item.getProductions() != null && !item.getProductions().isEmpty();
    }

    public static void addTabs(
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

    private static String displayName(Item item, String language)
    {
        return item.getName() == null
            ? Objects.toString(item.getKey(), "")
            : item.getName().getLocalizedName(language);
    }
}
