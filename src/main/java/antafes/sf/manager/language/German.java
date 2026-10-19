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

public class German extends antafes.utilities.language.German
{
    public German()
    {
        super();
        this.general();
        this.newProduction();
        this.newFactory();
    }

    private void general()
    {
        this.getTranslations().put("title", "Satisfactory-Manager");
        this.getTranslations().put("newProductionMenu", "Neue Produktion");
        this.getTranslations().put("newProductionMnemonic", "P");
        this.getTranslations().put("newFactoryMenu", "Neue Fabrik");
        this.getTranslations().put("newFactoryMnemonic", "F");
        this.getTranslations().put("openFactoryMenu", "Fabrik öffnen");
        this.getTranslations().put("openFactoryMnemonic", "Ö");
        this.getTranslations().put("deleteFactoryMenu", "Fabrik löschen");
        this.getTranslations().put("deleteFactoryMnemonic", "C");
        this.getTranslations().put("newStorageMenu", "Neuer Lagereintrag");
        this.getTranslations().put("newStorage", "Neuer Lagereintrag");
        this.getTranslations().put("storageChoices", "Verfügbare Materialien");
        this.getTranslations().put("storageFilterItem", "Nach Material filtern");
        this.getTranslations().put("storageFilterAll", "Alle Materialien");
        this.getTranslations().put("newStorageMnemonic", "L");
        this.getTranslations().put("importFactoryMenu", "Fabrik importieren");
        this.getTranslations().put("importFactoryMnemonic", "I");
        this.getTranslations().put("importSaveFileFilter", "Satisfactory-Spielstände (*.sav)");
        this.getTranslations().put("importParseError", "Der Spielstand konnte nicht gelesen werden.");
        this.getTranslations().put("importNothingFound", "Der Spielstand enthält keine importierbaren Maschinen.");
        this.getTranslations().put("importSkippedCount", "Übersprungene Maschinen: %d");
        this.getTranslations().put("importSummary", "%d Produktionen werden importiert (%d Maschinen übersprungen). Fabrikname:");
        this.getTranslations().put("importInvalidName", "Die Fabrik konnte nicht angelegt werden. Bitte einen gültigen Namen verwenden, der nicht bereits von einer anderen Fabrik genutzt wird.");
        this.getTranslations().put("importFailed", "Der Import ist fehlgeschlagen.");
        this.getTranslations().put("importLoading", "Spielstand wird gelesen…");
        this.getTranslations().put("importCreating", "Fabrik und Produktionen werden angelegt…");
        this.getTranslations().put(
            "importUnsupportedVersion",
            "Dieser Spielstand (Speicherversion %d) ist zu alt. Seine Maschinendaten können nicht gelesen werden, daher kann er nicht importiert werden."
        );
        this.getTranslations().put("options", "Optionen");
        this.getTranslations().put("optionsMnemonic", "O");
        this.getTranslations().put("useStorage", "Lager verwenden");
        this.getTranslations().put("storage", "Lager");
        this.getTranslations().put("planning", "Planung");
        this.getTranslations().put("planPlan", "Plan");
        this.getTranslations().put("planNew", "Neuer Plan");
        this.getTranslations().put("planDelete", "Plan löschen");
        this.getTranslations().put("planName", "Name des Plans");
        this.getTranslations().put("planInvalidName", "Der Name darf nicht leer sein.");
        this.getTranslations().put("planConfirmDelete", "Soll dieser Plan wirklich gelöscht werden?");
        this.getTranslations().put("planTargetItem", "Zielgegenstand");
        this.getTranslations().put("planTargetRate", "Gegenstände / min");
        this.getTranslations().put("planMachines", "Maschinen");
        this.getTranslations().put("planInputs", "Eingänge / min");
        this.getTranslations().put("planOutputs", "Ausgänge / min");
        this.getTranslations().put("planRate", "Menge");
        this.getTranslations().put("planAvailable", "In der Fabrik verfügbar");
        this.getTranslations().put("planOrigin", "Herkunft");
        this.getTranslations().put("planRawMaterials", "Rohstoffe");
        this.getTranslations().put("planSurplus", "Nebenprodukte");
        this.getTranslations().put("planTargetByproduct", "Nebenprodukt des Ziels");
        this.getTranslations().put("planSideProduction", "Nebenproduktion");
        this.getTranslations().put("planNone", "Keine Nebenproduktion");
        this.getTranslations().put("planAlternate", "Alternativ");
        this.getTranslations().put("planTotalPower", "Gesamter Stromverbrauch");
        this.getTranslations().put("planCreateProduction", "Produktion erstellen");
        this.getTranslations().put("planProductionsCreated", "%d Produktion(en) erstellt.");
        this.getTranslations().put("planSavingFailed", "Der Plan konnte nicht gespeichert werden.");
        this.getTranslations().put("planDeletionFailed", "Der Plan konnte nicht gelöscht werden.");
        this.getTranslations().put("planProductionCreationFailed", "Die Produktionen konnten nicht erstellt werden.");
        this.getTranslations().put("planNoTarget", "Kein Zielgegenstand ausgewählt.");
        this.getTranslations().put("planTargetRaw", "Der Zielgegenstand ist ein Rohstoff.");
        this.getTranslations().put("planCycle", "Eine zirkuläre Rezeptabhängigkeit wurde erkannt und als Rohstoff behandelt.");
        this.getTranslations().put("planNotConverged", "Die Nebenproduktionen konnten nicht aufgelöst werden.");
        this.getTranslations().put("storedRate", "Gelagert / min");
        this.getTranslations().put("sourceProductions", "Quellproduktionen");
        this.getTranslations().put("storageSavingFailed", "Die Lageroption konnte nicht gespeichert werden.");
        this.getTranslations().put("confirmDeleteStorage", "Möchten Sie diesen gelagerten Gegenstand wirklich entfernen?");
        this.getTranslations().put("deleteStorage", "Gelagerten Gegenstand entfernen");
        this.getTranslations().put("storageDeletionFailed", "Der gelagerte Gegenstand konnte nicht entfernt werden.");
    }

    private void newProduction()
    {
        this.getTranslations().put("newProduction", "Neue Produktionen erstellen");
        this.getTranslations().put("building", "Gebäude");
        this.getTranslations().put("productionBuildings", "Produktionsgebäude");
        this.getTranslations().put("pressurizer", "Quellenkompressor");
        this.getTranslations().put("recipe", "Rezept");
        this.getTranslations().put("item", "Gegenstand");
        this.getTranslations().put("amount", "Anzahl");
        this.getTranslations().put("group", "Gruppe");
        this.getTranslations().put("materialSources", "Materialquellen");
        this.getTranslations().put("materialSourceRemaining", "übrig");
        this.getTranslations().put("addMaterialSource", "Weitere Quelle hinzufügen");
        this.getTranslations().put("removeMaterialSource", "Diese Quelle entfernen");
        this.getTranslations().put("requiredFieldsHint", "* Pflichtfeld");
        this.getTranslations().put("position", "Position");
        this.getTranslations().put("purity", "Reinheit");
        this.getTranslations().put("impure", "Unrein");
        this.getTranslations().put("normal", "Normal");
        this.getTranslations().put("pure", "Rein");
        this.getTranslations().put("clockSpeed", "Taktgeschwindigkeit");
        this.getTranslations().put("usedSomersloops", "Verwendete Somersloops");
        this.getTranslations().put("ingredients", "Zutaten");
        this.getTranslations().put("production", "Produktion");
        this.getTranslations().put("usage", "Verwendung");
        this.getTranslations().put("material", "Material");
        this.getTranslations().put("unused", "Ungenutzt");
        this.getTranslations().put("status", "Status");
        this.getTranslations().put("materialStatusRed", "Rot: Verwendung übersteigt die Produktion");
        this.getTranslations().put("materialStatusOrange", "Orange: Verwendung entspricht der Produktion");
        this.getTranslations().put("materialStatusYellow", "Gelb: Ungenutzt höchstens 5 % der Produktion");
        this.getTranslations().put("materialStatusGreen", "Grün: mehr als 5 % der Produktion ungenutzt");
        this.getTranslations().put("materialStatusGrey", "Grau: keine Produktion und keine Verwendung");
        this.getTranslations().put("overview", "Übersicht");
        this.getTranslations().put("consumers", "Verbraucher");
        this.getTranslations().put("allBuildings", "Alle Gebäude");
        this.getTranslations().put("power", "Energie");
        this.getTranslations().put("powerSource", "Energiequelle");
        this.getTranslations().put("powerSources", "Produktionen");
        this.getTranslations().put("consumerGroups", "Verbraucher");
        this.getTranslations().put("totalPowerProduction", "Gesamterzeugung");
        this.getTranslations().put("totalPowerConsumption", "Gesamtverbrauch");
        this.getTranslations().put("availablePower", "Verfügbare Energie");
        this.getTranslations().put("powerStatusRed", "Rot: keine Energie mehr verfügbar");
        this.getTranslations().put("powerStatusYellow", "Gelb: verbleibende Energie höchstens 5 % der Gesamterzeugung");
        this.getTranslations().put("powerStatusGreen", "Grün: mehr als 5 % der Gesamterzeugung verbleiben");
        this.getTranslations().put("generatedPower", "Erzeugte Energie");
        this.getTranslations().put("consumption", "Verbrauch");
        this.getTranslations().put("usedMaterials", "Verwendete Materialien");
        this.getTranslations().put("expand", "Aufklappen");
        this.getTranslations().put("collapse", "Zuklappen");
        this.getTranslations().put("somersloops", "Somersloops");
        this.getTranslations().put("producedMaterials", "Produzierte Materialien");
        this.getTranslations().put("delete", "Löschen");
        this.getTranslations().put("edit", "Bearbeiten");
        this.getTranslations().put("editProduction", "Produktion bearbeiten");
        this.getTranslations().put("upgrade", "Aufrüsten");
        this.getTranslations().put("upgradeMiner", "Miner aufrüsten");
        this.getTranslations().put("deleteProduction", "Produktion löschen");
        this.getTranslations().put("confirmDeleteProduction", "Möchten Sie diese Produktion wirklich löschen?");
        this.getTranslations().put("productionDeletionFailed", "Die Produktion konnte nicht gelöscht werden.");
        this.getTranslations().put("productionUpgradeFailed", "Die Produktion konnte nicht aufgerüstet werden.");
        this.getTranslations().put("productionSavingFailed", "Die Produktion konnte nicht gespeichert werden.");
        this.getTranslations().put("productionEditingFailed", "Die Produktion konnte nicht aktualisiert werden.");
        this.getTranslations().put("deleteSelected", "Ausgewählte löschen");
        this.getTranslations().put("editSelected", "Ausgewählte bearbeiten");
        this.getTranslations().put("deleteProductions", "Produktionen löschen");
        this.getTranslations().put("confirmDeleteProductions", "Die folgenden %d Produktionen werden gelöscht:");
        this.getTranslations().put("productionsDeletionFailed", "Die Produktionen konnten nicht gelöscht werden.");
        this.getTranslations().put("deletingProductions", "Produktionen werden gelöscht...");
        this.getTranslations().put("editProductions", "Produktionen bearbeiten");
        this.getTranslations().put("editingProductions", "Produktionen werden aktualisiert...");
        this.getTranslations().put("creatingProductions", "Produktionen werden angelegt...");
        this.getTranslations().put("editProductionsHint", "Nur die Felder mit gesetztem Haken werden für alle %d ausgewählten Produktionen geändert.");
        this.getTranslations().put("keepUnchanged", "Unverändert lassen");
        this.getTranslations().put("differentBuildingsSomersloopsHint", "Die ausgewählten Produktionen verwenden unterschiedliche Gebäude.");
        this.getTranslations().put("notOverclockableHint", "Nicht alle ausgewählten Gebäude können übertaktet werden.");
        this.getTranslations().put("noSomersloopSlotsHint", "Nicht alle ausgewählten Gebäude haben Somersloop-Slots.");
        this.getTranslations().put(
            "differentRecipesMaterialSourcesHint",
            "Materialquellen können nur geändert werden, wenn alle ausgewählten Produktionen dasselbe Rezept nutzen."
        );
    }

    private void newFactory()
    {
        this.getTranslations().put("newFactory", "Neue Fabrik erstellen");
        this.getTranslations().put("factoryName", "Fabrikname");
        this.getTranslations().put("factoryCreationFailed", "Die Fabrik konnte nicht erstellt werden.");
        this.getTranslations().put("openFactory", "Fabrik öffnen");
        this.getTranslations().put("noFactoryOpen", "Keine Fabrik geöffnet");
        this.getTranslations().put("open", "Öffnen");
        this.getTranslations().put("deleteFactory", "Fabrik löschen");
        this.getTranslations().put("confirmDeleteFactory", "Möchten Sie die Fabrik \"%s\" wirklich mit allen Daten löschen?");
        this.getTranslations().put("factoryDeletionFailed", "Die Fabrik konnte nicht gelöscht werden.");
    }
}
