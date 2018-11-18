package mekhq.campaign.parts.refits;

import java.util.Optional;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public abstract class PartComparison {
    protected Unit unit;
    protected Part oldPart;
    protected Part newPart;

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit u) {
        unit = u;
    }

    public Optional<Part> getOldPart() {
        return Optional.ofNullable(oldPart);
    }

    public void setOldPart(Part part) {
        oldPart = part;
    }

    public Optional<Part> getNewPart() {
        return Optional.ofNullable(newPart);
    }

    public void setNewPart(Part part) {
        newPart = part;
    }

    protected String getNewLocationName() {
        if (null != newPart) {
            return unit.getEntity().getLocationName(newPart.getLocation());
        } else {
            return "<Missing Location>";
        }
    }

    public abstract String getDescription();

    public abstract boolean hasDifferences();
}
