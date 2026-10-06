package antafes.sfmanager.service;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FactoryServiceTest extends BaseTest
{
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private FactoryRepository factoryRepository;

    @Test
    public void testManagesFactoryLifecycleAndCurrentSelection() throws IOException
    {
        UUID factoryId;
        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            assertNull(this.factoryService.getCurrentFactory());
            assertEquals(List.of(), this.factoryService.getFactories());

            Factory factory = this.factoryService.createFactory("Service Factory");
            factoryId = factory.getId();
            assertEquals(List.of(factory), this.factoryService.getFactories());

            context.publishEvent(new FactorySelectedEvent(factory));
            assertSame(factory, this.factoryService.getCurrentFactory());

            factory.getProductions().add("production-id");
            this.factoryService.saveFactory(factory);
        }

        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            Factory loadedFactory = this.factoryService.findFactory(factoryId);
            assertNotNull(loadedFactory);
            assertEquals(List.of("production-id"), loadedFactory.getProductions());
            assertNull(this.factoryService.getCurrentFactory());
        }
    }

}
