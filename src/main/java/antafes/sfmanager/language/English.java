package antafes.sfmanager.language;

public class English extends antafes.utilities.language.English
{
    public English()
    {
        super();
        this.general();
        this.newProduction();
    }

    private void general()
    {
        this.getTranslations().put("title", "Satisfactory Manager");
        this.getTranslations().put("newProductionMenu", "New Production");
        this.getTranslations().put("newProductionMnemonic", "P");
    }

    private void newProduction()
    {
        this.getTranslations().put("newProduction", "Create new productions");
        this.getTranslations().put("building", "Building");
        this.getTranslations().put("pressurizer", "Resource well pressurizer");
        this.getTranslations().put("recipe", "Recipe");
        this.getTranslations().put("amount", "Amount");
        this.getTranslations().put("group", "Group");
        this.getTranslations().put("position", "Position");
        this.getTranslations().put("purity", "Purity");
        this.getTranslations().put("impure", "Impure");
        this.getTranslations().put("normal", "Normal");
        this.getTranslations().put("pure", "Pure");
        this.getTranslations().put("clockSpeed", "Clock speed");
        this.getTranslations().put("usedSomersloops", "Used Somersloops");
        this.getTranslations().put("ingredients", "Ingredients");
        this.getTranslations().put("production", "Production");
        this.getTranslations().put("generatedPower", "Generated power");
        this.getTranslations().put("consumption", "Consumption");
    }
}
