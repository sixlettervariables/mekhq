package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class RemovedPart extends PartComparison {
    public RemovedPart(Unit unit, Part oldPart) {
        this.unit = unit;
        this.oldPart = oldPart;
    }

    @Override
    public boolean hasDifferences() {
        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Removed %s from %s", this.oldPart.getName(),
            this.oldPart.getLocationName());
    }
}
