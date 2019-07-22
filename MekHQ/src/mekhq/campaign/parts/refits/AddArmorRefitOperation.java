package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Armor;

public class AddArmorRefitOperation extends AddPartRefitOperation {
    private Armor armor;

    public AddArmorRefitOperation(AddedPart comparison) {
        super(comparison);

        armor = (Armor)comparison.getNewPart().get();
    }

    public int getArmorPoints() {
        return armor.getTotalAmount();
    }

    @Override
    public int getTime() {
        return getArmorPoints() * armor.getBaseTimeFor(armor.getUnit().getEntity());
    }
}