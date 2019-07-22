package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class MovedPart extends PartComparison {
    public MovedPart(Unit unit, Part oldPart, Part newPart) {
        this.unit = unit;
        this.oldPart = oldPart;
        this.newPart = newPart;
    }

    @Override
    public boolean hasDifferences() {
        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Moved %s from %s to %s", this.newPart.getName(),
            this.oldPart.getLocationName(), getNewLocationName());
    }
}
