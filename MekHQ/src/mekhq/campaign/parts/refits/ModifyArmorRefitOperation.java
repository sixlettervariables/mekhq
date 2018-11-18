package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

public class ModifyArmorRefitOperation extends ModifyPartRefitOperation {
    private ModifiedPart comparison;

    public ModifyArmorRefitOperation(ModifiedPart comparison) {
        super(comparison);
        if (!(comparison.getNewPart().get() instanceof Armor)) {
            // TODO: throw.
        }
    }

    @Override
    public int getLocation() {
        return comparison.getNewPart().map(Part::getLocation)
            .orElse(super.getLocation());
    }

    @Override
    public int getTime() {
        // Only add the delta of the armor
        Armor oPart = (Armor)comparison.getOldPart().get();
        Armor nPart = (Armor)comparison.getNewPart().get();
        int totalAmount = nPart.getTotalAmount() - oPart.getTotalAmount();
        return totalAmount * nPart.getBaseTimeFor(comparison.getUnit().getEntity());
    }
}