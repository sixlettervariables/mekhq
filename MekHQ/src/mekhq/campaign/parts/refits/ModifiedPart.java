package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class ModifiedPart extends PartComparison {
    public ModifiedPart(Unit unit, Part oldPart, Part newPart) {
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
        return String.format("%s (%s) modified", this.oldPart.getName(),
            this.oldPart.getLocationName());
    }
}
