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

package antafes.sf.manager.repository;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.*;
import antafes.sf.manager.migration.FactoryMigration;
import antafes.sf.manager.migration.MigrationVersion;
import antafes.sf.manager.migration.V001;
import antafes.sf.manager.service.FactoryMigrationService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FactoryMigrationTest extends BaseTest
{
    @Test
    public void testNewFactoriesUseHighestRegisteredMigrationVersion() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createMigrationContext()) {
            FactoryRepository factories = context.getBean(FactoryRepository.class);
            factories.init();
            Factory factory = factories.createFactory("Latest migration");

            String latestVersion = this.getLatestMigrationVersion(context);
            assertEquals(MigrationVersion.parse(latestVersion).toString(), factories.getVersion(factory));
        }
    }

    @Test
    public void testMigratesLegacyResourcePositionsOnceAndPreservesUnmatchedValues() throws IOException
    {
        UUID matchedId = UUID.randomUUID();
        UUID unmatchedId = UUID.randomUUID();
        String matchingPosition;
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ResourceNodeRepository.class,
            FactoryRepository.class,
            ProductionRepository.class
        )) {
            this.autowireTest(context);
            BuildingRepository buildings = context.getBean(BuildingRepository.class);
            ItemRepository items = context.getBean(ItemRepository.class);
            FactoryRepository factories = context.getBean(FactoryRepository.class);
            ProductionRepository productions = context.getBean(ProductionRepository.class);
            buildings.init();
            items.init();
            ResourceNodeRepository resourceNodes = context.getBean(ResourceNodeRepository.class);
            resourceNodes.init();
            factories.init();
            productions.init();

            ResourceNode node = this.findUniqueIronNode(context);
            matchingPosition = Math.round(node.getX()) + ", " + Math.round(node.getY());
            Factory factory = factories.createFactory("Position migration");
            Building miner = buildings.findOne("miner_1");
            Item iron = items.findOne("iron");
            Production matched = this.miner(matchedId, miner, iron, matchingPosition);
            Production unmatched = this.miner(unmatchedId, miner, iron, "A1");
            factory.getProductions().addAll(List.of(matchedId.toString(), unmatchedId.toString()));
            factories.saveFactory(factory);
            productions.saveProductions(factory, List.of(matched, unmatched));
            Path factoryFile = this.tempDirectory.resolve("position-migration").resolve("factory.xml");
            Path productionsFile = this.tempDirectory.resolve("position-migration").resolve("productions.xml");
            assertEquals(matchingPosition, matched.getPosition());
            assertTrue(Files.readString(productionsFile).contains("<position>" + matchingPosition + "</position>"));
            Files.writeString(
                factoryFile,
                Files.readString(factoryFile).replaceAll("<version>[^<]+</version>", "")
            );
        }

        String latestVersion = FactoryMigration.INITIAL_VERSION;
        for (int load = 0; load < 2; load++) {
            try (AnnotationConfigApplicationContext context = this.createMigrationContext()) {
                FactoryRepository factories = context.getBean(FactoryRepository.class);
                ProductionRepository productions = context.getBean(ProductionRepository.class);
                factories.init();
                Factory factory = factories.findAll().values().iterator().next();
                productions.init();

                latestVersion = this.getLatestMigrationVersion(context);
                String migratedPosition = this.findUniqueIronNode(context).getKey();
                if (load == 0) {
                    assertEquals("1.0.0", factories.getVersion(factory));
                    assertEquals(matchingPosition, productions.findOne(matchedId).getPosition());
                } else {
                    assertEquals(latestVersion, factories.getVersion(factory));
                    assertEquals(migratedPosition, productions.findOne(matchedId).getPosition());
                }
                context.publishEvent(new FactorySelectedEvent(factory));
                assertEquals(latestVersion, factories.getVersion(factory));
                assertEquals(migratedPosition, productions.findOne(matchedId).getPosition());
                assertEquals("A1", productions.findOne(unmatchedId).getPosition());
            }
        }
        assertTrue(Files.readString(this.tempDirectory.resolve("position-migration").resolve("factory.xml"))
            .contains("<version>" + latestVersion + "</version>"));
    }

    @Test
    public void testDoesNotMigrateAmbiguousCoordinates()
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            ResourceNodeRepository.class
        )) {
            BuildingRepository buildings = context.getBean(BuildingRepository.class);
            ItemRepository items = context.getBean(ItemRepository.class);
            ResourceNodeRepository nodes = context.getBean(ResourceNodeRepository.class);
            buildings.init();
            items.init();
            nodes.init();
            ResourceNode existing = this.findUniqueIronNode(context);
            nodes.findAll().put("test-duplicate-node", new ResourceNode()
                .setKey("test-duplicate-node")
                .setType(ResourceNode.Type.NODE)
                .setItem(items.findOne("iron"))
                .setX(existing.getX())
                .setY(existing.getY())
                .setZ(existing.getZ() + 100));
            String position = Math.round(existing.getX()) + ", " + Math.round(existing.getY());
            Production production = this.miner(
                UUID.randomUUID(),
                buildings.findOne("miner_1"),
                items.findOne("iron"),
                position
            );

            new V001().migrate(new Factory(), List.of(production), nodes);

            assertEquals(position, production.getPosition());
        }
    }

    @Test
    public void testMatchesPositionsWithinTenMetersButNotBeyond()
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            ResourceNodeRepository.class
        )) {
            BuildingRepository buildings = context.getBean(BuildingRepository.class);
            ItemRepository items = context.getBean(ItemRepository.class);
            ResourceNodeRepository nodes = context.getBean(ResourceNodeRepository.class);
            buildings.init();
            items.init();
            nodes.init();
            String nodeKey = "test-ten-meter-node";
            double nodeX = 100_000;
            double nodeY = -100_000;
            nodes.findAll().put(nodeKey, new ResourceNode()
                .setKey(nodeKey)
                .setType(ResourceNode.Type.NODE)
                .setItem(items.findOne("iron"))
                .setX(nodeX)
                .setY(nodeY));
            Production production = this.miner(
                UUID.randomUUID(),
                buildings.findOne("miner_1"),
                items.findOne("iron"),
                (nodeX + 10) + ", " + nodeY
            );
            V001 migration = new V001();

            migration.migrate(new Factory(), List.of(production), nodes);

            assertEquals(nodeKey, production.getPosition());

            String outsidePosition = (nodeX + 10.01) + ", " + nodeY;
            production.setPosition(outsidePosition);
            migration.migrate(new Factory(), List.of(production), nodes);

            assertEquals(outsidePosition, production.getPosition());
        }
    }

    private AnnotationConfigApplicationContext createMigrationContext()
    {
        AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ResourceNodeRepository.class,
            FactoryRepository.class,
            V001.class,
            ProductionRepository.class,
            FactoryMigrationService.class
        );
        context.getBean(BuildingRepository.class).init();
        context.getBean(ItemRepository.class).init();
        context.getBean(RecipeRepository.class).init();
        context.getBean(ResourceNodeRepository.class).init();
        return context;
    }

    private String getLatestMigrationVersion(AnnotationConfigApplicationContext context)
    {
        return context.getBeansOfType(FactoryMigration.class)
            .values()
            .stream()
            .map(FactoryMigration::getVersion)
            .max(Comparator.comparing(MigrationVersion::parse))
            .map(version -> MigrationVersion.parse(version).toString())
            .orElse(FactoryMigration.INITIAL_VERSION);
    }

    private ResourceNode findUniqueIronNode(AnnotationConfigApplicationContext context)
    {
        List<ResourceNode> nodes = context.getBean(ResourceNodeRepository.class)
            .findByItemAndType("iron", ResourceNode.Type.NODE);
        V001 migration = new V001();
        Item iron = context.getBean(ItemRepository.class).findOne("iron");
        Building miner = context.getBean(BuildingRepository.class).findOne("miner_1");
        return nodes.stream()
            .filter(node -> {
                String position = Math.round(node.getX()) + ", " + Math.round(node.getY());
                Production candidate = this.miner(UUID.randomUUID(), miner, iron, position);
                migration.migrate(
                    new Factory(),
                    List.of(candidate),
                    context.getBean(ResourceNodeRepository.class)
                );
                return node.getKey().equals(candidate.getPosition());
            })
            .findFirst()
            .orElseThrow();
    }

    private Production miner(UUID id, Building building, Item item, String position)
    {
        return new Production()
            .setId(id)
            .setSelectedBuilding(building)
            .setSelectedItem(item)
            .setGroup("Miners")
            .setPosition(position)
            .setPurity(Purity.NORMAL)
            .setClockSpeed(100.0)
            .setUsedSomersloops(0);
    }
}
