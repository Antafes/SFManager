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

import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.migration.FactoryMigration;
import antafes.sf.manager.migration.MigrationVersion;
import antafes.sf.manager.repository.FactoryRepository;
import antafes.sf.manager.repository.ProductionRepository;
import antafes.sf.manager.repository.ResourceNodeRepository;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class FactoryMigrationService
{
    @Autowired
    private FactoryRepository factoryRepository;
    @Autowired
    private ProductionRepository productionRepository;
    @Autowired
    private ResourceNodeRepository resourceNodeRepository;
    @Autowired(required = false)
    private List<FactoryMigration> factoryMigrations = List.of();

    @EventListener
    public void onFactorySelected(FactorySelectedEvent event)
    {
        try {
            this.migrateFactory(event.factory());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save factory migrations.", exception);
        }
    }

    private void migrateFactory(Factory factory) throws IOException
    {
        if (factory == null || factory.getId() == null) {
            throw new IllegalArgumentException("A saved factory is required.");
        }
        Factory savedFactory = this.factoryRepository.findOne(factory.getId());
        if (savedFactory == null) {
            throw new IllegalArgumentException("Factory must already exist in the repository.");
        }
        this.productionRepository.init();

        List<FactoryMigration> migrations = this.factoryMigrations.stream()
            .sorted(Comparator.comparing(migration -> MigrationVersion.parse(migration.getVersion())))
            .toList();
        MigrationVersion previousVersion = MigrationVersion.parse(FactoryMigration.INITIAL_VERSION);
        for (FactoryMigration migration : migrations) {
            MigrationVersion migrationVersion = MigrationVersion.parse(migration.getVersion());
            if (migrationVersion.compareTo(previousVersion) <= 0) {
                throw new IllegalStateException("Factory migration versions must be finite and strictly increasing.");
            }
            previousVersion = migrationVersion;
        }

        MigrationVersion currentVersion = MigrationVersion.parse(this.factoryRepository.getVersion(savedFactory));
        MigrationVersion latestVersion = migrations.isEmpty()
            ? MigrationVersion.parse(FactoryMigration.INITIAL_VERSION)
            : MigrationVersion.parse(migrations.getLast().getVersion());
        if (currentVersion.compareTo(latestVersion) > 0) {
            throw new IllegalStateException("Factory version is newer than this application supports.");
        }

        MigrationVersion storedVersion = currentVersion;
        if (migrations.stream().noneMatch(migration ->
            MigrationVersion.parse(migration.getVersion()).compareTo(storedVersion) > 0
        )) {
            return;
        }
        this.resourceNodeRepository.init();

        for (FactoryMigration migration : migrations) {
            MigrationVersion migrationVersion = MigrationVersion.parse(migration.getVersion());
            if (migrationVersion.compareTo(currentVersion) <= 0) {
                continue;
            }

            List<Production> productions = savedFactory.getProductions().stream()
                .map(UUID::fromString)
                .map(this.productionRepository.findAll()::get)
                .filter(Objects::nonNull)
                .toList();
            if (productions.size() != savedFactory.getProductions().size()) {
                throw new IllegalStateException("A factory production could not be loaded for migration.");
            }
            Map<Production, String> originalPositions = new IdentityHashMap<>();
            productions.forEach(production -> originalPositions.put(production, production.getPosition()));
            try {
                migration.migrate(savedFactory, productions, this.resourceNodeRepository);
                this.productionRepository.saveFactoryProductions(savedFactory);
                this.factoryRepository.saveVersion(savedFactory, migration.getVersion());
                currentVersion = migrationVersion;
            } catch (IOException | RuntimeException failure) {
                originalPositions.forEach(Production::setPosition);
                throw failure;
            }
        }
    }
}
