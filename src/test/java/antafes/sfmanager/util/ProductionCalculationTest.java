package antafes.sfmanager.util;

import antafes.sfmanager.entity.Purity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductionCalculationTest
{
    @Test
    public void calculatesProductionWithFractionalSomersloopBoost()
    {
        assertEquals(67.5, ProductionCalculator.calculateProduction(30.0, 150.0, 1, 2), 0.0001);
    }

    @Test
    public void calculatesProductionWithoutSomersloopSlots()
    {
        assertEquals(30.0, ProductionCalculator.calculateProduction(30.0, 100.0, 0, 0), 0.0001);
    }

    @Test
    public void preservesRecipeRatesAtDefaultClockSpeed()
    {
        assertEquals(37.5, ProductionCalculator.calculateProduction(37.5, 100.0, 0, 2), 0.0001);
        assertEquals(7.5, ProductionCalculator.calculateProduction(7.5, 100.0, 0, 2), 0.0001);
    }

    @Test
    public void calculatesResourceProductionForEachPurity()
    {
        assertEquals(
            30.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.IMPURE, 100.0, 0, 0),
            0.0001
        );
        assertEquals(
            60.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.NORMAL, 100.0, 0, 0),
            0.0001
        );
        assertEquals(
            120.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.PURE, 100.0, 0, 0),
            0.0001
        );
        assertEquals(
            270.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.PURE, 150.0, 1, 2),
            0.0001
        );
    }

    @Test
    public void calculatesPowerConsumptionWithClockSpeedAndSomersloops()
    {
        assertEquals(9.0, PowerCalculator.calculateConsumption(4.0, 100.0, 1, 2), 0.0001);
    }

    @Test
    public void calculatesPowerConsumptionWithoutSomersloopSlots()
    {
        assertEquals(4.0, PowerCalculator.calculateConsumption(4.0, 100.0), 0.0001);
    }
}
