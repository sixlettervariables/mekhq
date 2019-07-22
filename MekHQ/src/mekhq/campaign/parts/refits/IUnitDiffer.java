package mekhq.campaign.parts.refits;

import mekhq.campaign.unit.Unit;

public interface IUnitDiffer {
    public UnitDiffResults diff(Unit oldUnit, Unit newUnit);
}
