package antafes.sfmanager.util;

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
