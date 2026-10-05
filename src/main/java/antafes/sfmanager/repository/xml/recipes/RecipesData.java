package antafes.sfmanager.repository.xml.recipes;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "recipes")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecipesData
{
    @XmlElement(name = "recipe")
    public List<RecipeData> recipes;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RecipeData
    {
        @XmlAttribute
        public String key;
        public NameData name;
        public String producedIn;
        public boolean alternate;

        @XmlElementWrapper(name = "ingredients")
        @XmlElement(name = "ingredient")
        public List<ItemData> ingredients;

        @XmlElementWrapper(name = "products")
        @XmlElement(name = "product")
        public List<ItemData> products;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NameData
    {
        public String english;
        public String german;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ItemData
    {
        public String resource;
        public Double amount;
    }
}
