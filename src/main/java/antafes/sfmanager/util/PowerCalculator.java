package antafes.sfmanager.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PowerCalculator
{
    public Double calculateConsumption(Double baseConsumption, Double clockSpeed) {
        return calculateConsumption(baseConsumption, clockSpeed, 0, 0);
    }

    public Double calculateConsumption(Double baseConsumption, Double clockSpeed, int usedSomersloops, int somersloopSlots)
    {
        Double multiplier = Math.pow(1 + ((double) usedSomersloops / somersloopSlots), 2);
        return baseConsumption * usedSomersloops * Math.pow(clockSpeed / 100, 1.321928);
    }
}
