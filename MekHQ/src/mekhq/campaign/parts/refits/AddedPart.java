package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class AddedPart extends PartComparison {
    public AddedPart(Unit unit, Part newPart) {
        this.unit = unit;
        this.newPart = newPart;
    }

    @Override
    public boolean hasDifferences() {
        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Added %s to %s", this.newPart.getName(), getNewLocationName());
    }
}
