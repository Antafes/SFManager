package antafes.sfmanager.repository;

import antafes.sfmanager.entity.Factory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FactoryRepositoryTest
{
    @Test
    public void testLoadsFactoryAndProductionIdsFromXml()
    {
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(FactoryRepository.class)) {
            FactoryRepository repository = context.getBean(FactoryRepository.class);

            repository.init();

            assertEquals(1, repository.findAll().size());
            UUID factoryId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            Factory factory = repository.findOne(factoryId);
            assertNotNull(factory);
            assertEquals(factoryId, factory.getId());
            assertEquals("Main Factory", factory.getName());
            assertEquals(List.of("production_one", "production_two"), factory.getProductions());
        }
    }
}
