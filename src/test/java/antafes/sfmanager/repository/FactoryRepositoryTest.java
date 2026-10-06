package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.util.FactoryName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FactoryRepositoryTest
{
    @TempDir
    private Path tempDirectory;

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
        try (AnnotationConfigApplicationContext context = this.createContext()) {
            FactoryRepository repository = context.getBean(FactoryRepository.class);
            repository.init();
            assertTrue(repository.findAll().isEmpty());

            created = repository.createFactory("Crème Factory 42");
            repository.createFactory("Second Factory");

            assertNotNull(created.getId());
            assertEquals("Crème Factory 42", created.getName());
            assertEquals(List.of(), created.getProductions());
            assertTrue(this.tempDirectory.resolve("creme-factory-42").resolve("factory.xml").toFile().isFile());
            assertTrue(this.tempDirectory.resolve("second-factory").resolve("factory.xml").toFile().isFile());
        }

        try (AnnotationConfigApplicationContext context = this.createContext()) {
            FactoryRepository repository = context.getBean(FactoryRepository.class);
            repository.init();

            Factory loaded = repository.findOne(created.getId());
            assertNotNull(loaded);
            assertEquals(created.getId(), loaded.getId());
            assertEquals(created.getName(), loaded.getName());
            assertTrue(loaded.getProductions().isEmpty());
            assertEquals(2, repository.findAll().size());
        }
    }

    @Test
    public void testRejectsDuplicateNormalizedFactoryDirectory() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext()) {
            FactoryRepository repository = context.getBean(FactoryRepository.class);
            repository.createFactory("Café");

            assertThrows(IllegalArgumentException.class, () -> repository.createFactory("Cafe"));
            assertFalse(repository.findAll().isEmpty());
            assertEquals(1, repository.findAll().size());
        }
    }

    @Test
    public void testRejectsFactoryXmlThatDoesNotMatchSchema() throws IOException
    {
        Path factoryDirectory = Files.createDirectory(this.tempDirectory.resolve("invalid-factory"));
        Files.writeString(factoryDirectory.resolve("factory.xml"),
            "<factory xmlns=\"http://www.w3schools.com/SFManager/factory\""
                + " id=\"550e8400-e29b-41d4-a716-446655440000\"><name>Invalid</name></factory>");

        try (AnnotationConfigApplicationContext context = this.createContext()) {
            FactoryRepository repository = context.getBean(FactoryRepository.class);

            assertThrows(RuntimeException.class, repository::init);
        }
    }

    private AnnotationConfigApplicationContext createContext()
    {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(Configuration.class, () -> new TestConfiguration(this.tempDirectory.toFile()));
        context.register(FactoryRepository.class);
        context.refresh();
        return context;
    }

    private static class TestConfiguration extends Configuration
    {
        private final File saveDirectory;

        private TestConfiguration(File saveDirectory)
        {
            this.saveDirectory = saveDirectory;
        }

        @Override
        public File getSaveDirPath()
        {
            return this.saveDirectory;
        }
    }
}
