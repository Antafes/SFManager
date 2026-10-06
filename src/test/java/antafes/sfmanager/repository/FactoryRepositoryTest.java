package antafes.sfmanager.repository;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.util.FactoryName;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FactoryRepositoryTest extends BaseTest
{
    @Autowired
    private FactoryRepository factoryRepository;

    @Test
    public void testNormalizesFactoryNamesToAsciiDirectoryNames()
    {
        assertEquals("creme-brulee-42", FactoryName.toDirectoryName("  Crème brûlée 42! "));
        assertEquals("strasse-aether", FactoryName.toDirectoryName("Straße Æther"));
        assertEquals("factory-019", FactoryName.toDirectoryName("Factory 019"));
    }

    @Test
    public void testRejectsFactoryNamesWithoutAsciiLettersOrDigits()
    {
        assertThrows(IllegalArgumentException.class, () -> FactoryName.toDirectoryName("東京"));
        assertThrows(IllegalArgumentException.class, () -> FactoryName.toDirectoryName("  "));
    }

    @Test
    public void testCreatesAndReloadsFactoryInItsNamedDirectory() throws IOException
    {
        Factory created;
        try (AnnotationConfigApplicationContext context = this.createContext(FactoryRepository.class)) {
            this.autowireTest(context);
            this.factoryRepository.init();
            assertTrue(this.factoryRepository.findAll().isEmpty());

            created = this.factoryRepository.createFactory("Crème Factory 42");
            this.factoryRepository.createFactory("Second Factory");

            assertNotNull(created.getId());
            assertEquals("Crème Factory 42", created.getName());
            assertFalse(created.isUseStorage());
            assertEquals(List.of(), created.getProductions());
            assertTrue(this.tempDirectory.resolve("creme-factory-42").resolve("factory.xml").toFile().isFile());
            assertTrue(this.tempDirectory.resolve("second-factory").resolve("factory.xml").toFile().isFile());
            created.getProductions().add("550e8400-e29b-41d4-a716-446655440000");
            created.setUseStorage(true);
            this.factoryRepository.saveFactory(created);
        }

        try (AnnotationConfigApplicationContext context = this.createContext(FactoryRepository.class)) {
            this.autowireTest(context);
            this.factoryRepository.init();

            Factory loaded = this.factoryRepository.findOne(created.getId());
            assertNotNull(loaded);
            assertEquals(created.getId(), loaded.getId());
            assertEquals(created.getName(), loaded.getName());
            assertTrue(loaded.isUseStorage());
            assertEquals(List.of("550e8400-e29b-41d4-a716-446655440000"), loaded.getProductions());
            assertEquals(2, this.factoryRepository.findAll().size());
        }
    }

    @Test
    public void testRejectsDuplicateNormalizedFactoryDirectory() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(FactoryRepository.class)) {
            this.autowireTest(context);
            this.factoryRepository.createFactory("Café");

            assertThrows(IllegalArgumentException.class, () -> this.factoryRepository.createFactory("Cafe"));
            assertFalse(this.factoryRepository.findAll().isEmpty());
            assertEquals(1, this.factoryRepository.findAll().size());
        }
    }

    @Test
    public void testRejectsFactoryXmlThatDoesNotMatchSchema() throws IOException
    {
        Path factoryDirectory = Files.createDirectory(this.tempDirectory.resolve("invalid-factory"));
        Files.writeString(factoryDirectory.resolve("factory.xml"),
            "<factory xmlns=\"http://www.w3schools.com/SFManager/factory\""
                + " id=\"550e8400-e29b-41d4-a716-446655440000\"><name>Invalid</name></factory>");

        try (AnnotationConfigApplicationContext context = this.createContext(FactoryRepository.class)) {
            this.autowireTest(context);

            assertThrows(RuntimeException.class, this.factoryRepository::init);
        }
    }
}
