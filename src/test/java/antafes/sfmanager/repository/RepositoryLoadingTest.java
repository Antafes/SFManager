package antafes.sfmanager.repository;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Recipe;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class RepositoryLoadingTest extends BaseTest
{
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void setUp()
    {
        super.setUp();
        this.context = new AnnotationConfigApplicationContext(
            BuildingRepository.class, ItemRepository.class, RecipeRepository.class
        );
    }

    @AfterEach
    public void tearDown()
    {
        this.context.close();
    }

    @Test
    public void testLoadsBuildingsFromXml()
    {
        BuildingRepository repository = this.context.getBean(BuildingRepository.class);

        repository.init();

        assertEquals(44, repository.findAll().size());
        Building smelter = repository.findOne("smelter");
        assertNotNull(smelter);
        assertEquals("smelter", smelter.getKey());
        assertEquals(-4, smelter.getPower());
    }

    @Test
    public void testLoadsItemsAndResolvesBuildingReferences()
    {
        ItemRepository repository = this.context.getBean(ItemRepository.class);

        repository.init();

        assertEquals(38, repository.findAll().size());
        Item iron = repository.findOne("iron");
        assertNotNull(iron);
        assertEquals("Iron Ore", iron.getName().getEnglish());
        assertEquals(3, iron.getProductions().size());
        assertSame(this.context.getBean(BuildingRepository.class).findOne("miner_1"),
            iron.getProductions().getFirst().getBuilding());

        Item ironIngot = repository.findOne("iron_ingot");
        assertNotNull(ironIngot);
        assertNotNull(ironIngot.getProductions());
        assertEquals(0, ironIngot.getProductions().size());
    }

    @Test
    public void testLoadsRecipesAndResolvesBuildingAndItemReferences()
    {
        RecipeRepository repository = this.context.getBean(RecipeRepository.class);

        repository.init();

        assertEquals(39, repository.findAll().size());
        Recipe ironIngot = repository.findOne("iron_ingot");
        assertNotNull(ironIngot);
        assertSame(this.context.getBean(BuildingRepository.class).findOne("smelter"),
            ironIngot.getProducedIn());
        assertSame(this.context.getBean(ItemRepository.class).findOne("iron"),
            ironIngot.getIngredients().getFirst().getResource());
        assertSame(this.context.getBean(ItemRepository.class).findOne("iron_ingot"),
            ironIngot.getProducts().getFirst().getResource());
    }
}
