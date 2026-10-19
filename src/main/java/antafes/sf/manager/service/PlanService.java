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
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.repository.PlanRepository;
import antafes.sf.manager.service.dto.PlanResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    private PlanCalculationService planCalculationService;

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

    private Factory requireFactory()
    {
        Factory factory = this.factoryService.getCurrentFactory();
        if (factory == null) {
            throw new IllegalStateException("Open a factory before using plans.");
        }

        return factory;
    }
}
