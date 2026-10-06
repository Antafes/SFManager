package antafes.sfmanager.service;

import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import jakarta.annotation.PostConstruct;
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
    @Getter
    private Factory currentFactory;

    @PostConstruct
    private void initialize()
    {
        this.factoryRepository.init();
    }

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
    }
}
