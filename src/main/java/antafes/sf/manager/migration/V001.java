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

package antafes.sf.manager.migration;

import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.entity.ResourceNode;
import antafes.sf.manager.repository.ResourceNodeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class V001 implements FactoryMigration
{
    private static final String VERSION = "1.1.0";
    private static final double MAX_POSITION_DISTANCE_METERS = 10;

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public void migrate(Factory factory, List<Production> productions, ResourceNodeRepository resourceNodeRepository)
    {
        for (Production production : productions) {
            Building building = production.getSelectedBuilding();
            if (building == null || !this.hasFixedResourceNodes(building.getKey())) {
                continue;
            }
            String position = production.getPosition();
            if (position == null || position.isBlank()) {
                continue;
            }
            ResourceNode.Type type = "well_extractor".equals(building.getKey())
                ? ResourceNode.Type.WELL
                : ResourceNode.Type.NODE;
            List<ResourceNode> matches = this.findMatches(position, type, production, resourceNodeRepository);
            if (matches.size() == 1) {
                production.setPosition(matches.getFirst().getKey());
            }
        }
    }

    private boolean hasFixedResourceNodes(String buildingKey)
    {
        return buildingKey != null && (buildingKey.startsWith("miner_")
            || "oil_extractor".equals(buildingKey)
            || "well_extractor".equals(buildingKey));
    }

    private List<ResourceNode> findMatches(
        String position,
        ResourceNode.Type type,
        Production production,
        ResourceNodeRepository resourceNodeRepository
    )
    {
        String[] coordinates = position.split(",", -1);
        if (coordinates.length != 2) {
            return List.of();
        }

        double x;
        double y;
        try {
            x = Double.parseDouble(coordinates[0].trim());
            y = Double.parseDouble(coordinates[1].trim());
        } catch (NumberFormatException exception) {
            return List.of();
        }
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            return List.of();
        }

        List<ResourceNode> matches = new ArrayList<>();
        // Legacy positions are whole meters; resource-node values were converted from centimeters.
        for (ResourceNode node : resourceNodeRepository.findAll().values()) {
            double distance = Math.hypot(node.getX() - x, node.getY() - y);
            if (node.getType() != type
                || node.getItem() == null
                || production.getSelectedItem() == null
                || !node.getItem().getKey().equals(production.getSelectedItem().getKey())
                || !Double.isFinite(distance)
                || distance > MAX_POSITION_DISTANCE_METERS) {
                continue;
            }
            matches.add(node);
        }
        return matches;
    }
}
