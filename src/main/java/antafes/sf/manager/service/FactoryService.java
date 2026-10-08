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
import antafes.sf.manager.repository.FactoryRepository;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class FactoryService
{
    @Autowired
    private FactoryRepository factoryRepository;
    @Autowired
    private Configuration configuration;
    @Getter
    private Factory currentFactory;

    public List<Factory> getFactories()
    {
        return List.copyOf(this.factoryRepository.findAll().values());
    }

    public Factory createFactory(String name) throws IOException
    {
        return this.factoryRepository.createFactory(name);
    }

    public void saveFactory(Factory factory) throws IOException
    {
        this.factoryRepository.saveFactory(factory);
    }

    public Factory findFactory(UUID id)
    {
        return this.factoryRepository.findOne(id);
    }

    public Factory restoreLastOpenedFactory()
    {
        String factoryId = this.configuration.getLastOpenedFactoryId();
        if (factoryId == null || factoryId.isBlank()) {
            return null;
        }

        Factory factory;
        try {
            factory = this.factoryRepository.findOne(UUID.fromString(factoryId));
        } catch (IllegalArgumentException exception) {
            this.configuration.setLastOpenedFactoryId(null);
            return null;
        }
        if (factory == null) {
            this.configuration.setLastOpenedFactoryId(null);
            return null;
        }

        this.currentFactory = factory;
        return factory;
    }

    @EventListener
    private void onFactorySelected(FactorySelectedEvent event)
    {
        Factory selectedFactory = event.factory();
        if (selectedFactory == null || selectedFactory.getId() == null) {
            throw new IllegalArgumentException("Selected factory is not loaded in the repository.");
        }
        Factory repositoryFactory = this.factoryRepository.findOne(selectedFactory.getId());
        if (repositoryFactory == null) {
            throw new IllegalArgumentException("Selected factory is not loaded in the repository.");
        }
        this.currentFactory = repositoryFactory;
        this.configuration.setLastOpenedFactoryId(repositoryFactory.getId());
    }
}
