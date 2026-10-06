package antafes.sfmanager.language;

public class English extends antafes.utilities.language.English
{
    public English()
    {
        super();
        this.general();
        this.newProduction();
        this.newFactory();
    }

    private void general()
    {
        this.getTranslations().put("title", "Satisfactory Manager");
        this.getTranslations().put("newProductionMenu", "New Production");
        this.getTranslations().put("newProductionMnemonic", "P");
        this.getTranslations().put("newFactoryMenu", "New Factory");
        this.getTranslations().put("newFactoryMnemonic", "F");
        this.getTranslations().put("openFactoryMenu", "Open Factory");
        this.getTranslations().put("openFactoryMnemonic", "O");
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
        this.getTranslations().put("productionSavingFailed", "Could not save the production.");
    }

    private void newFactory()
    {
        this.getTranslations().put("newFactory", "Create new factory");
        this.getTranslations().put("factoryName", "Factory name");
        this.getTranslations().put("factoryCreationFailed", "Could not create the factory.");
        this.getTranslations().put("openFactory", "Open factory");
        this.getTranslations().put("noFactoryOpen", "No factory open");
        this.getTranslations().put("open", "Open");
    }
}
