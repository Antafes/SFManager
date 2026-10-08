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

package antafes.sf.manager.service;

import antafes.sf.manager.entity.item.Production;
import antafes.sf.manager.entity.recipe.Item;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Storage;
import antafes.sf.manager.repository.ProductionRepository;
import antafes.sf.manager.repository.StorageRepository;
import antafes.sf.manager.service.dto.StorageItem;
import antafes.sf.manager.service.dto.StorageProductionChoice;
import antafes.sf.manager.service.event.ProductionDeletedEvent;
import antafes.sf.manager.service.event.StorageChangedEvent;
import antafes.sf.manager.util.ProductionCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StorageService
{
    @Autowired
    private StorageRepository storageRepository;
    @Autowired
    private ProductionRepository productionRepository;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public List<StorageProductionChoice> getAvailableChoices()
    {
        Factory factory = this.getCurrentFactory();
        Set<String> storedEntryKeys = this.storageRepository.getStorage(factory).getEntries().stream()
            .map(this::toEntryKey)
            .collect(Collectors.toSet());
        return this.getProductionChoices(this.getFactoryProductions(factory)).stream()
            .filter(choice -> !storedEntryKeys.contains(this.toEntryKey(choice)))
            .toList();
    }

    public List<StorageItem> getStoredItems()
    {
        Factory factory = this.getCurrentFactory();
        Map<String, StorageProductionChoice> choicesByEntryKey = this.getProductionChoices(
            this.getFactoryProductions(factory)
        ).stream().collect(Collectors.toMap(this::toEntryKey, choice -> choice));
        Map<String, MutableStorageItem> storedItems = new LinkedHashMap<>();
        for (Storage.Entry entry : this.storageRepository.getStorage(factory).getEntries()) {
            StorageProductionChoice choice = choicesByEntryKey.get(this.toEntryKey(entry));
            if (choice == null) {
                continue;
            }
            MutableStorageItem storageItem = storedItems.computeIfAbsent(
                choice.item().getKey(),
                ignored -> new MutableStorageItem(choice.item())
            );
            storageItem.rate += choice.rate();
            storageItem.sources.add(choice);
        }
        return storedItems.values().stream()
            .map(storageItem -> new StorageItem(storageItem.item, storageItem.rate, storageItem.sources))
            .toList();
    }

    public void addStorageEntries(List<Storage.Entry> entries) throws IOException
    {
        Factory factory = this.getCurrentFactory();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("At least one storage entry is required.");
        }

        Map<String, StorageProductionChoice> choicesByEntryKey = this.getProductionChoices(
            this.getFactoryProductions(factory)
        ).stream().collect(Collectors.toMap(this::toEntryKey, choice -> choice));
        Storage storage = this.storageRepository.getStorage(factory);
        List<Storage.Entry> originalEntries = List.copyOf(storage.getEntries());
        Set<String> entryKeys = originalEntries.stream().map(this::toEntryKey).collect(Collectors.toSet());
        List<Storage.Entry> entriesToAdd = new ArrayList<>();
        for (Storage.Entry entry : entries) {
            String entryKey = this.toEntryKey(entry);
            if (!choicesByEntryKey.containsKey(entryKey)) {
                throw new IllegalArgumentException("The selected storage output is not available.");
            }
            if (!entryKeys.add(entryKey)) {
                throw new IllegalArgumentException("The selected storage output already exists.");
            }
            entriesToAdd.add(new Storage.Entry()
                .setProductionId(entry.getProductionId())
                .setItemKey(entry.getItemKey()));
        }

        storage.getEntries().addAll(entriesToAdd);
        try {
            this.storageRepository.saveStorage(factory, storage);
        } catch (IOException | RuntimeException failure) {
            storage.setEntries(new ArrayList<>(originalEntries));
            throw failure;
        }
        this.eventPublisher.publishEvent(new StorageChangedEvent(factory));
    }

    public void removeStorageEntries(List<Storage.Entry> entries) throws IOException
    {
        Factory factory = this.getCurrentFactory();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("At least one storage entry is required.");
        }

        Storage storage = this.storageRepository.getStorage(factory);
        List<Storage.Entry> originalEntries = List.copyOf(storage.getEntries());
        Set<String> entryKeys = entries.stream().map(this::toEntryKey).collect(Collectors.toSet());
        if (!originalEntries.stream().map(this::toEntryKey).collect(Collectors.toSet()).containsAll(entryKeys)) {
            throw new IllegalArgumentException("The selected storage output does not exist.");
        }
        storage.getEntries().removeIf(entry -> entryKeys.contains(this.toEntryKey(entry)));
        try {
            this.storageRepository.saveStorage(factory, storage);
        } catch (IOException | RuntimeException failure) {
            storage.setEntries(new ArrayList<>(originalEntries));
            throw failure;
        }
        this.eventPublisher.publishEvent(new StorageChangedEvent(factory));
    }

    public double getStoredRate(UUID productionId, String itemKey)
    {
        return this.getStoredItems().stream()
            .flatMap(storageItem -> storageItem.sources().stream())
            .filter(choice -> productionId.equals(choice.production().getId())
                && itemKey.equals(choice.item().getKey()))
            .mapToDouble(StorageProductionChoice::rate)
            .sum();
    }

    @EventListener
    private void onProductionDeleted(ProductionDeletedEvent event)
    {
        Factory factory = this.factoryService.getCurrentFactory();
        if (factory == null) {
            return;
        }
        Set<UUID> deletedProductionIds = event.productions().stream()
            .map(antafes.sf.manager.entity.Production::getId)
            .collect(Collectors.toSet());
        Storage storage = this.storageRepository.getStorage(factory);
        List<Storage.Entry> originalEntries = List.copyOf(storage.getEntries());
        if (!storage.getEntries().removeIf(entry -> deletedProductionIds.contains(entry.getProductionId()))) {
            return;
        }
        try {
            this.storageRepository.saveStorage(factory, storage);
        } catch (IOException | RuntimeException exception) {
            storage.setEntries(new ArrayList<>(originalEntries));
            throw new IllegalStateException("Could not remove deleted productions from storage.", exception);
        }
        this.eventPublisher.publishEvent(new StorageChangedEvent(factory));
    }

    private Factory getCurrentFactory()
    {
        Factory factory = this.factoryService.getCurrentFactory();
        if (factory == null) {
            throw new IllegalStateException("Open a factory before managing storage.");
        }
        return factory;
    }

    private List<antafes.sf.manager.entity.Production> getFactoryProductions(Factory factory)
    {
        return factory.getProductions().stream()
            .map(UUID::fromString)
            .map(productionRepository::findOne)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<StorageProductionChoice> getProductionChoices(List<antafes.sf.manager.entity.Production> productions)
    {
        Map<UUID, antafes.sf.manager.entity.Production> productionsById = productions.stream()
            .collect(Collectors.toMap(antafes.sf.manager.entity.Production::getId, production -> production));
        List<StorageProductionChoice> choices = new ArrayList<>();
        for (antafes.sf.manager.entity.Production production : productions) {
            if (production.getSelectedItem() != null) {
                choices.add(new StorageProductionChoice(
                    production,
                    production.getSelectedItem(),
                    this.getResourceRate(production, productionsById)
                ));
                continue;
            }
            if (production.getSelectedRecipe() == null
                || production.getSelectedRecipe().getProducts() == null) {
                continue;
            }
            for (Item product : production.getSelectedRecipe().getProducts()) {
                if (product.getResource() != null && product.getAmount() != null) {
                    choices.add(new StorageProductionChoice(
                        production,
                        product.getResource(),
                        ProductionCalculator.calculateProduction(
                            product.getAmount(),
                            production.getClockSpeed(),
                            production.getUsedSomersloops(),
                            production.getSelectedBuilding().getSomersloopSlots()
                        )
                    ));
                }
            }
        }
        return choices;
    }

    private double getResourceRate(antafes.sf.manager.entity.Production production, Map<UUID, antafes.sf.manager.entity.Production> productionsById)
    {
        antafes.sf.manager.entity.Item item = production.getSelectedItem();
        Production itemProduction = item.getProductions().stream()
            .filter(candidate -> candidate.getBuilding() != null
                && production.getSelectedBuilding().getKey().equals(candidate.getBuilding().getKey()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("The resource production is not available."));
        antafes.sf.manager.entity.Production rateProduction = production;
        if ("well_extractor".equals(production.getSelectedBuilding().getKey())) {
            rateProduction = productionsById.get(production.getPressurizerId());
            if (rateProduction == null) {
                throw new IllegalStateException("The well extractor has no pressurizer production.");
            }
        }
        return ProductionCalculator.calculateResourceProduction(
            (double) itemProduction.getAmount(),
            production.getPurity(),
            rateProduction.getClockSpeed(),
            rateProduction.getUsedSomersloops(),
            rateProduction.getSelectedBuilding().getSomersloopSlots()
        );
    }

    private String toEntryKey(Storage.Entry entry)
    {
        return entry.getProductionId() + ":" + entry.getItemKey();
    }

    private String toEntryKey(StorageProductionChoice choice)
    {
        return choice.production().getId() + ":" + choice.item().getKey();
    }

    private static class MutableStorageItem
    {
        private final antafes.sf.manager.entity.Item item;
        private final List<StorageProductionChoice> sources = new ArrayList<>();
        private double rate;

        private MutableStorageItem(antafes.sf.manager.entity.Item item)
        {
            this.item = item;
        }
    }
}
