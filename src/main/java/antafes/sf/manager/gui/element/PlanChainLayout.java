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

package antafes.sf.manager.gui.element;

import antafes.sf.manager.entity.Item;
import antafes.sf.manager.service.dto.PlanMaterial;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.dto.PlanStep;

import java.util.*;

/**
 * Computes the columns, rows and connections of the visual production chain.
 * Raw materials are placed in the first column, producers to the right of their ingredients.
 */
public final class PlanChainLayout
{
    public enum Kind
    {
        RAW,
        STEP,
        SIDE_STEP,
        TARGET_BYPRODUCT
    }

    public record Node(String id, Kind kind, int column, int row, PlanStep step, PlanMaterial material)
    {
    }

    public record Edge(String fromId, String toId, Item item, double rate, boolean dashed)
    {
    }

    public record Layout(List<Node> nodes, List<Edge> edges)
    {
    }

    private final PlanResult result;
    private final Map<String, PlanStep> mainSteps = new LinkedHashMap<>();
    private final Map<String, PlanMaterial> rawMaterials = new LinkedHashMap<>();
    private final Map<String, Integer> columns = new HashMap<>();
    private final Set<String> inProgress = new HashSet<>();
    private final List<Edge> edges = new ArrayList<>();

    private PlanChainLayout(PlanResult result)
    {
        this.result = result;
    }

    public static Layout compute(PlanResult result)
    {
        return new PlanChainLayout(result).build();
    }

    public static String stepId(PlanStep step)
    {
        return (step.sideProduction() ? "side:" : "step:") + step.item().getKey();
    }

    public static String rawId(Item item)
    {
        return "raw:" + item.getKey();
    }

    private Layout build()
    {
        List<PlanStep> sideSteps = new ArrayList<>();
        for (PlanStep step : this.result.steps()) {
            if (step.sideProduction()) {
                sideSteps.add(step);
            } else {
                this.mainSteps.put(step.item().getKey(), step);
            }
        }
        this.result.rawInputs().forEach(material -> this.rawMaterials.put(material.item().getKey(), material));

        List<Node> nodes = new ArrayList<>();
        Map<Integer, Integer> rowCounters = new HashMap<>();
        List<PlanStep> allSteps = new ArrayList<>(this.mainSteps.values());
        allSteps.addAll(sideSteps);
        for (PlanStep step : allSteps) {
            this.connect(step);
        }

        Set<String> rawIds = new LinkedHashSet<>();
        this.edges.stream()
            .map(Edge::fromId)
            .filter(id -> id.startsWith("raw:"))
            .forEach(rawIds::add);
        this.rawMaterials.values().forEach(material -> rawIds.add(rawId(material.item())));
        for (String rawId : rawIds) {
            PlanMaterial material = this.rawMaterials.get(rawId.substring(4));
            if (material != null) {
                nodes.add(new Node(rawId, Kind.RAW, 0, this.nextRow(rowCounters, 0), null, material));
            }
        }
        for (PlanStep step : allSteps) {
            int column = this.column(stepId(step));
            nodes.add(new Node(
                stepId(step),
                step.sideProduction() ? Kind.SIDE_STEP : Kind.STEP,
                column,
                this.nextRow(rowCounters, column),
                step,
                null
            ));
        }
        this.addTargetByproducts(nodes, rowCounters);

        return new Layout(nodes, this.edges);
    }

    private void addTargetByproducts(List<Node> nodes, Map<Integer, Integer> rowCounters)
    {
        if (this.mainSteps.isEmpty()) {
            return;
        }
        PlanStep target = this.mainSteps.values().iterator().next();
        int column = this.column(stepId(target)) + 1;
        for (PlanMaterial material : this.result.surplus()) {
            if (!material.targetByproduct()) {
                continue;
            }
            String id = "target-by:" + material.item().getKey();
            nodes.add(new Node(
                id,
                Kind.TARGET_BYPRODUCT,
                column,
                this.nextRow(rowCounters, column),
                null,
                material
            ));
            this.edges.add(new Edge(stepId(target), id, material.item(), material.rate(), true));
        }
    }

    private void connect(PlanStep step)
    {
        String stepId = stepId(step);
        for (PlanMaterial input : step.inputs()) {
            String itemKey = input.item().getKey();
            if (step.sideProduction() && itemKey.equals(step.item().getKey())) {
                this.connectByproductSources(step, input);
            } else if (this.mainSteps.containsKey(itemKey)) {
                this.edges.add(new Edge(
                    stepId(this.mainSteps.get(itemKey)),
                    stepId,
                    input.item(),
                    input.rate(),
                    false
                ));
            } else {
                this.rawMaterials.putIfAbsent(itemKey, new PlanMaterial(input.item(), input.rate()));
                this.edges.add(new Edge(rawId(input.item()), stepId, input.item(), input.rate(), false));
            }
        }
    }

    private void connectByproductSources(PlanStep sideStep, PlanMaterial input)
    {
        for (PlanStep source : this.mainSteps.values()) {
            for (PlanMaterial output : source.outputs()) {
                if (output.item().getKey().equals(input.item().getKey())
                    && !source.item().getKey().equals(input.item().getKey())) {
                    this.edges.add(new Edge(stepId(source), stepId(sideStep), input.item(), output.rate(), true));
                }
            }
        }
    }

    private int column(String id)
    {
        if (id.startsWith("raw:")) {
            return 0;
        }
        Integer known = this.columns.get(id);
        if (known != null) {
            return known;
        }
        if (!this.inProgress.add(id)) {
            return 0;
        }
        int column = 1;
        for (Edge edge : this.edges) {
            if (edge.toId().equals(id)) {
                column = Math.max(column, this.column(edge.fromId()) + 1);
            }
        }
        this.inProgress.remove(id);
        this.columns.put(id, column);

        return column;
    }

    private int nextRow(Map<Integer, Integer> rowCounters, int column)
    {
        int row = rowCounters.getOrDefault(column, 0);
        rowCounters.put(column, row + 1);

        return row;
    }
}
