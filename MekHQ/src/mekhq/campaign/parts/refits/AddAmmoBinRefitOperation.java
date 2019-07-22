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
            AmmoType type = getAmmoType();
            // Adding ammo requires base 15 minutes per ton of ammo or 60 minutes per capital missile
            if (type.hasFlag(AmmoType.F_CAP_MISSILE) || type.hasFlag(AmmoType.F_CRUISE_MISSILE) || type.hasFlag(AmmoType.F_SCREEN)) {
                return 60 * ((LargeCraftAmmoBin)ammoBin).getFullShots();
            } else {
                return (int)Math.ceil(15 * Math.max(1, ammoBin.getTonnage()));
            }
        }

        // 120 minutes to install a new AmmoBin
        return 120;
    }
}
