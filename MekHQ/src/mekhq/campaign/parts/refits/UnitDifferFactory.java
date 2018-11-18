package mekhq.campaign.parts.refits;

import mekhq.campaign.unit.Unit;

public class UnitDifferFactory {
    private static UnitDifferFactory instance;

    public static UnitDifferFactory getInstance() {
        if (null == instance) {
            instance = new UnitDifferFactory();
        }

        return instance;
    }

    public IUnitDiffer create(Unit oldUnit, Unit newUnit) {
        return new GenericUnitDiffer();
    }
}
