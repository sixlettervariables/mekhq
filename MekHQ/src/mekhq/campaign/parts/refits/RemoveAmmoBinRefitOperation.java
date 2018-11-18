package mekhq.campaign.parts.refits;

import megamek.common.AmmoType;
import mekhq.campaign.parts.equipment.AmmoBin;

public class RemoveAmmoBinRefitOperation extends RemovePartRefitOperation {
    private AmmoBin ammoBin;

    public RemoveAmmoBinRefitOperation(RemovedPart comparison) {
        super(comparison);

        ammoBin = (AmmoBin)comparison.getOldPart().get();
    }

    public AmmoType getAmmoType() {
        return (AmmoType)ammoBin.getType();
    }

    public int getRemainingShots() {
        return ammoBin.getFullShots() - ammoBin.getShotsNeeded();
    }

    @Override
    public int getTime() {
        return getRemainingShots() > 0 ? 120 : 0;
    }
}