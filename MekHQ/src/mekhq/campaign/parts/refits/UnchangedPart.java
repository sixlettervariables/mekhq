package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class UnchangedPart extends PartComparison {
    public UnchangedPart(Unit unit, Part oldPart, Part newPart) {
        this.unit = unit;
        this.oldPart = oldPart;
        this.newPart = newPart;
    }

    @Override
    public boolean hasDifferences() {
        return false;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) unchanged", this.oldPart.getName(),
            this.oldPart.getLocationName());
    }
}
