package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Armor;

public class RemoveArmorRefitOperation extends RemovePartRefitOperation {
    private Armor armor;

    public RemoveArmorRefitOperation(RemovedPart comparison) {
        super(comparison);
        
        armor = (Armor)comparison.getOldPart().get();
    }

    public int getArmorPoints() {
        return armor.getTotalAmount();
    }

    @Override
    public int getTime() {
        return getArmorPoints() * armor.getBaseTimeFor(armor.getUnit().getEntity());
    }
}
