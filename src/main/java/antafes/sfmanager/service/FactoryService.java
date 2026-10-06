package antafes.sfmanager.service;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.sfmanager.service.event.FactorySelectedEvent;
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
