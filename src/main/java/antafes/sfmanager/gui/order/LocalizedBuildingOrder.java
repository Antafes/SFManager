package antafes.sfmanager.gui.order;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Name;
import lombok.experimental.UtilityClass;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@UtilityClass
public class LocalizedBuildingOrder
{
    public Comparator<Building> comparator(String language)
    {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            "German".equalsIgnoreCase(language) ? "de" : "en"
        ));
        collator.setStrength(Collator.SECONDARY);
        return Comparator.<Building, String>comparing(
            building -> LocalizedBuildingOrder.displayName(building, language),
            collator
        ).thenComparing(Building::getKey);
    }

    public void addTabs(
        JTabbedPane tabs,
        List<Building> buildings,
        String language,
        Function<Building, ? extends Component> panelFactory
    )
    {
        buildings.stream()
            .sorted(LocalizedBuildingOrder.comparator(language))
            .forEach(building -> tabs.addTab(
                LocalizedBuildingOrder.displayName(building, language),
                panelFactory.apply(building)
            ));
    }

    private String displayName(Building building, String language)
    {
        Name name = building.getName();
        return name == null ? building.getKey() : name.getLocalizedName(language);
    }
}
