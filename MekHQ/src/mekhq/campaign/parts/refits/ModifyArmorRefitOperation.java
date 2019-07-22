package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Armor;

public class ModifyArmorRefitOperation extends ModifyPartRefitOperation {
    private Armor oldArmor;
    private Armor newArmor;

    public ModifyArmorRefitOperation(ModifiedPart comparison) {
        super(comparison);
        
        oldArmor = (Armor)comparison.getOldPart().get();
        newArmor = (Armor)comparison.getNewPart().get();
    }

    @Override
    public int getLocation() {
        return newArmor.getLocation();
    }

    @Override
    public int getTime() {
        // Only add the delta of the armor
        int totalAmount = newArmor.getTotalAmount() - oldArmor.getTotalAmount();
        return totalAmount * newArmor.getBaseTimeFor(newArmor.getUnit().getEntity());
    }
}