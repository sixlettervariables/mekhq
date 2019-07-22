package mekhq.campaign.parts.refits;

import megamek.common.AmmoType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;

public class AddAmmoBinRefitOperation extends AddPartRefitOperation {
    private AmmoBin ammoBin;

    public AddAmmoBinRefitOperation(AddedPart comparison) {
        super(comparison);

        ammoBin = (AmmoBin)comparison.getNewPart().get();
    }

    public AmmoType getAmmoType() {
        return (AmmoType)ammoBin.getType();
    }
    
    public int getNeededShots() {
        return getAmmoType().getShots();
    }

    @Override
    public int getTime() {
        if (ammoBin instanceof LargeCraftAmmoBin) {
            // Adding ammo requires base 15 minutes per ton of ammo. Putting in a new
            // capital missile bay can take weeks.
            return (int)(15 * Math.max(1, ammoBin.getTonnage()));
        }

        // 120 minutes to install a new AmmoBin
        return 120;
    }
}