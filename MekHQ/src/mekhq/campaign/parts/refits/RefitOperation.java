package mekhq.campaign.parts.refits;

import java.util.Collections;
import java.util.List;

import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public abstract class RefitOperation {
    public int getLocation() {
        return -1;
    }

    public int getTime() {
        return 0;
    }

    public List<Part> getShoppingList() {
        return Collections.emptyList();
    }

    protected boolean isOmniRefit(Unit oldUnit, Unit newUnit) {
        return oldUnit.getEntity().isOmni() && newUnit.getEntity().isOmni();
    }
}
