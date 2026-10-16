/*
 * This file is part of SFManager.
 *
 * SFManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SFManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SFManager. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package SFManager
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2026, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

package antafes.sf.manager.language;

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
        this.getTranslations().put("newStorage", "New Storage");
        this.getTranslations().put("storageChoices", "Available materials");
        this.getTranslations().put("storageFilterItem", "Filter by item");
        this.getTranslations().put("storageFilterAll", "All items");
        this.getTranslations().put("newStorageMnemonic", "S");
        this.getTranslations().put("importFactoryMenu", "Import factory");
        this.getTranslations().put("importFactoryMnemonic", "I");
        this.getTranslations().put("importSaveFileFilter", "Satisfactory save files (*.sav)");
        this.getTranslations().put("importParseError", "The save file could not be read.");
        this.getTranslations().put("importNothingFound", "The save contains no machines that can be imported.");
        this.getTranslations().put("importSkippedCount", "Skipped machines: %d");
        this.getTranslations().put("importSummary", "Importing %d productions (%d machines skipped). Factory name:");
        this.getTranslations().put("importInvalidName", "The factory could not be created. Please use a valid name that is not used by another factory.");
        this.getTranslations().put("importFailed", "The import failed.");
        this.getTranslations().put("importLoading", "Reading save file…");
        this.getTranslations().put("importCreating", "Creating factory and productions…");
        this.getTranslations().put(
            "importUnsupportedVersion",
            "This save file (save version %d) is too old. Its machine data cannot be read, so it cannot be imported."
        );
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
        this.getTranslations().put("productionBuildings", "Production buildings");
        this.getTranslations().put("pressurizer", "Resource well pressurizer");
        this.getTranslations().put("recipe", "Recipe");
        this.getTranslations().put("item", "Item");
        this.getTranslations().put("amount", "Amount");
        this.getTranslations().put("group", "Group");
        this.getTranslations().put("materialSources", "Material Sources");
        this.getTranslations().put("materialSourceRemaining", "remaining");
        this.getTranslations().put("addMaterialSource", "Add another source");
        this.getTranslations().put("removeMaterialSource", "Remove this source");
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
        this.getTranslations().put("status", "Status");
        this.getTranslations().put("materialStatusRed", "Red: usage exceeds production");
        this.getTranslations().put("materialStatusOrange", "Orange: usage equals production");
        this.getTranslations().put("materialStatusYellow", "Yellow: unused is at most 5 % of the production");
        this.getTranslations().put("materialStatusGreen", "Green: more than 5 % of the production is unused");
        this.getTranslations().put("materialStatusGrey", "Grey: no production and no usage");
        this.getTranslations().put("overview", "Overview");
        this.getTranslations().put("consumers", "Consumers");
        this.getTranslations().put("allBuildings", "All buildings");
        this.getTranslations().put("power", "Power");
        this.getTranslations().put("powerSource", "Power source");
        this.getTranslations().put("powerSources", "Production");
        this.getTranslations().put("consumerGroups", "Consumers");
        this.getTranslations().put("totalPowerProduction", "Total production");
        this.getTranslations().put("totalPowerConsumption", "Total consumption");
        this.getTranslations().put("availablePower", "Available power");
        this.getTranslations().put("generatedPower", "Generated power");
        this.getTranslations().put("consumption", "Consumption");
        this.getTranslations().put("usedMaterials", "Used Materials");
        this.getTranslations().put("expand", "Expand");
        this.getTranslations().put("collapse", "Collapse");
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
        this.getTranslations().put("deleteSelected", "Delete selected");
        this.getTranslations().put("editSelected", "Edit selected");
        this.getTranslations().put("deleteProductions", "Delete productions");
        this.getTranslations().put("confirmDeleteProductions", "The following %d productions will be deleted:");
        this.getTranslations().put("productionsDeletionFailed", "Could not delete the productions.");
        this.getTranslations().put("deletingProductions", "Deleting productions...");
        this.getTranslations().put("editProductions", "Edit productions");
        this.getTranslations().put("editingProductions", "Updating productions...");
        this.getTranslations().put("editProductionsHint", "Only the fields with a checked box are changed for all %d selected productions.");
        this.getTranslations().put("keepUnchanged", "Keep unchanged");
        this.getTranslations().put("differentBuildingsSomersloopsHint", "The selected productions use different buildings.");
        this.getTranslations().put("notOverclockableHint", "Not all selected buildings can be overclocked.");
        this.getTranslations().put("noSomersloopSlotsHint", "Not all selected buildings have Somersloop slots.");
        this.getTranslations().put(
            "differentRecipesMaterialSourcesHint",
            "Material sources can only be changed if all selected productions use the same recipe."
        );
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
