package antafes.sf.manager.util;

import antafes.sf.manager.entity.Production;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionLabelFormatterTest
{
    @Test
    void testAddsPositionInBrackets()
    {
        Production production = new Production();
        production.setGroup("Coal 1");
        production.setPosition("123, -321; 456, 89");

        assertEquals("Coal 1(123, -321; 456, 89)", ProductionLabelFormatter.format(production, "en"));
    }

    @Test
    void testOmitsBracketsWithoutPosition()
    {
        Production production = new Production();
        production.setGroup("Coal 2");

        assertEquals("Coal 2", ProductionLabelFormatter.format(production, "en"));

        production.setPosition("  ");
        assertEquals("Coal 2", ProductionLabelFormatter.format(production, "en"));
    }
}
