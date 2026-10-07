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
        this.getTranslations().put("newStorageMenu", "New Storage");
        this.getTranslations().put("newStorageMnemonic", "S");
        this.getTranslations().put("options", "Options");
        this.getTranslations().put("optionsMnemonic", "O");
        this.getTranslations().put("useStorage", "Use storage");
        this.getTranslations().put("storage", "Storage");
        this.getTranslations().put("storedRate", "Stored / min");
        this.getTranslations().put("sourceProductions", "Source productions");
        this.getTranslations().put("storageSavingFailed", "Could not save the storage option.");
        this.getTranslations().put("confirmDeleteStorage", "Are you sure you want to remove this stored item?");
        this.getTranslations().put("deleteStorage", "Remove stored item");
        this.getTranslations().put("storageDeletionFailed", "Could not remove the stored item.");
    }

    private void newProduction()
    {
        this.getTranslations().put("newProduction", "Create new productions");
        this.getTranslations().put("building", "Building");
        this.getTranslations().put("pressurizer", "Resource well pressurizer");
        this.getTranslations().put("recipe", "Recipe");
        this.getTranslations().put("item", "Item");
        this.getTranslations().put("amount", "Amount");
        this.getTranslations().put("group", "Group");
        this.getTranslations().put("materialSources", "Material Sources");
        this.getTranslations().put("requiredFieldsHint", "* Required field");
        this.getTranslations().put("position", "Position");
        this.getTranslations().put("purity", "Purity");
        this.getTranslations().put("impure", "Impure");
        this.getTranslations().put("normal", "Normal");
        this.getTranslations().put("pure", "Pure");
        this.getTranslations().put("clockSpeed", "Clock speed");
        this.getTranslations().put("usedSomersloops", "Used Somersloops");
        this.getTranslations().put("ingredients", "Ingredients");
        this.getTranslations().put("production", "Production");
        this.getTranslations().put("usage", "Usage");
        this.getTranslations().put("material", "Material");
        this.getTranslations().put("unused", "Unused");
        this.getTranslations().put("overview", "Overview");
        this.getTranslations().put("power", "Power");
        this.getTranslations().put("powerSource", "Power source");
        this.getTranslations().put("powerSources", "Production");
        this.getTranslations().put("totalPowerProduction", "Total production");
        this.getTranslations().put("totalPowerConsumption", "Total consumption");
        this.getTranslations().put("availablePower", "Available power");
        this.getTranslations().put("generatedPower", "Generated power");
        this.getTranslations().put("consumption", "Consumption");
        this.getTranslations().put("usedMaterials", "Used Materials");
        this.getTranslations().put("somersloops", "Somersloops");
        this.getTranslations().put("producedMaterials", "Produced Materials");
        this.getTranslations().put("delete", "Delete");
        this.getTranslations().put("edit", "Edit");
        this.getTranslations().put("editProduction", "Edit production");
        this.getTranslations().put("upgrade", "Upgrade");
        this.getTranslations().put("upgradeMiner", "Upgrade miner");
        this.getTranslations().put("deleteProduction", "Delete production");
        this.getTranslations().put("confirmDeleteProduction", "Are you sure you want to delete this production?");
        this.getTranslations().put("productionDeletionFailed", "Could not delete the production.");
        this.getTranslations().put("productionUpgradeFailed", "Could not upgrade the production.");
        this.getTranslations().put("productionSavingFailed", "Could not save the production.");
        this.getTranslations().put("productionEditingFailed", "Could not update the production.");
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
