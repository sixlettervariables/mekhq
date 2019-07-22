package mekhq.campaign.parts.refits;

import megamek.common.weapons.InfantryAttack;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.parts.MissingBattleArmorSuit;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;

public class RefitOperationFactory {
    public RefitOperation create(PartComparison comparison) {
        if (comparison instanceof ModifiedPart) {
            return createModification((ModifiedPart)comparison);
        } else if (comparison instanceof RemovedPart) {
            return createRemoval((RemovedPart)comparison);
        } else if (comparison instanceof MovedPart) {
            return createMove((MovedPart)comparison);
        } else if (comparison instanceof AddedPart) {
            return createAdd((AddedPart)comparison);
        } else {
            // UnchangedPart
            return new NoopRefitOperation(comparison);
        }
    }

    private RefitOperation createAdd(AddedPart comparison) {
        Part addedPart = comparison.getNewPart().get();
        if (addedPart instanceof MissingBattleArmorSuit) {
            return new NoopRefitOperation(comparison);
        } else if (addedPart instanceof Armor) {
            return new AddArmorRefitOperation(comparison);
        } else if (addedPart instanceof AmmoBin) {
            return new AddAmmoBinRefitOperation(comparison);
        }

        return new AddPartRefitOperation(comparison);
    }

    private RefitOperation createRemoval(RemovedPart comparison) {
        Part removedPart = comparison.getOldPart().get();
        if ((removedPart instanceof MissingPart)
            || (removedPart instanceof BattleArmorSuit)
            || (removedPart instanceof TransportBayPart)
            || ((removedPart instanceof EquipmentPart
                && ((EquipmentPart)removedPart).getType() instanceof InfantryAttack))) {
            // No-op to remove these (we don't).
            return new NoopRefitOperation(comparison);
        } else if (removedPart instanceof Armor) {
            return new RemoveArmorRefitOperation(comparison);
        } else if (removedPart instanceof AmmoBin) {
            return new RemoveAmmoBinRefitOperation(comparison);
        }

        return new RemovePartRefitOperation(comparison);
    }

    private RefitOperation createModification(ModifiedPart comparison) {
        Part modifiedPart = comparison.getNewPart().get();
        if (modifiedPart instanceof Armor) {
            return new ModifyArmorRefitOperation(comparison);
        }

        return new ModifyPartRefitOperation(comparison);
    }

    private RefitOperation createMove(MovedPart comparison) {
        Part movedPart = comparison.getNewPart().get();
        if (movedPart instanceof EquipmentPart) {
            return new MoveEquipmentPartRefitOperation(comparison);
        }

        return new MovePartRefitOperation(comparison);
    }
}