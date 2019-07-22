package mekhq.campaign.parts.refits;

import mekhq.campaign.unit.Unit;

public class UnitDifferFactory {
    public IUnitDiffer create(Unit oldUnit, Unit newUnit) {
        return new GenericUnitDiffer();
    }
}
