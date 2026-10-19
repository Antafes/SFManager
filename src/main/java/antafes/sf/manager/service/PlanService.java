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

import antafes.sf.manager.Configuration;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.repository.PlanRepository;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.dto.PlanStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlanService
{
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ProductionService productionService;
    @Autowired
    private PlanCalculationService planCalculationService;
    @Autowired
    private Configuration configuration;

    public List<Plan> getPlans()
    {
        Factory factory = this.factoryService.getCurrentFactory();
        if (factory == null) {
            return List.of();
        }

        return this.planRepository.findByFactory(factory);
    }

    public Plan createPlan(String name, String targetItemKey, double targetRate) throws IOException
    {
        Plan plan = new Plan()
            .setId(UUID.randomUUID())
            .setFactoryId(this.requireFactory().getId())
            .setName(name == null ? null : name.trim())
            .setTargetItemKey(targetItemKey)
            .setTargetRate(targetRate);
        this.planRepository.savePlan(this.requireFactory(), plan);

        return plan;
    }

    public void savePlan(Plan plan) throws IOException
    {
        this.planRepository.savePlan(this.requireFactory(), plan);
    }

    public void deletePlan(UUID planId) throws IOException
    {
        this.planRepository.deletePlan(this.requireFactory(), planId);
    }

    public PlanResult calculate(Plan plan)
    {
        return this.planCalculationService.calculate(plan);
    }

    /**
     * Creates the productions of a single plan step in the active factory.
     * The machines running at the step's clock speed are created first, followed by the partially used one.
     */
    public List<Production> createProductions(PlanStep step) throws IOException
    {
        this.requireFactory();
        if (step.machineCount() <= 0) {
            throw new IllegalArgumentException("The step needs at least one machine.");
        }
        List<Production> created = new ArrayList<>();
        if (step.fullMachines() > 0) {
            created.addAll(this.productionService.saveProductions(
                this.createProduction(step, step.clockSpeed()),
                step.fullMachines()
            ));
        }
        if (step.partialClockSpeed() > 0) {
            created.add(this.productionService.saveProduction(this.createProduction(step, step.partialClockSpeed())));
        }

        return created;
    }

    private Production createProduction(PlanStep step, double clockSpeed)
    {
        String language = this.configuration.getLanguage().getName();
        var firstProduct = step.recipe().getProducts().getFirst().getResource();
        String group = firstProduct.getName() == null ? null : firstProduct.getName().getLocalizedName(language);

        return new Production()
            .setSelectedBuilding(step.building())
            .setSelectedRecipe(step.recipe())
            .setClockSpeed(clockSpeed)
            .setUsedSomersloops(step.somersloops())
            .setGroup(group);
    }

    private Factory requireFactory()
    {
        Factory factory = this.factoryService.getCurrentFactory();
        if (factory == null) {
            throw new IllegalStateException("Open a factory before using plans.");
        }

        return factory;
    }
}
