package mekhq.campaign.parts.refits;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.WeaponType;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.MissingInfantryArmorPart;
import mekhq.campaign.parts.MissingInfantryMotiveType;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingMekCockpit;
import mekhq.campaign.parts.MissingMekGyro;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public class RefitClassCalculator {
    private boolean isRefurbishing;

    public RefitClassCalculator(boolean isRefurbishing) {
        this.isRefurbishing = isRefurbishing;
    }

    public int calculate(Unit oldUnit, Unit newUnit, List<RefitOperation> operations) {
        if (isRefurbishing) {
            return calculateRefurbishClass(oldUnit, newUnit, operations);
        }

        int refitClass = Refit.NO_CHANGE;

        List<Part> removedParts = operations.stream()
            .filter(op -> op instanceof RemovePartRefitOperation)
            .map(op -> ((RemovePartRefitOperation)op).getPart())
            .collect(Collectors.toList());

        boolean isOmniRefit = calcIsOmniRefit(oldUnit, newUnit);
        for (RefitOperation op : operations) {
            if (op instanceof RemovePartRefitOperation) {
                if (isOmniRefit) {
                    refitClass = max(refitClass, Refit.CLASS_OMNI);
                } else {
                    refitClass = max(refitClass, Refit.CLASS_A);
                }
            } else if (op instanceof MovePartRefitOperation) {
                if (isOmniRefit && ((MovePartRefitOperation)op).isOmniPodded()) {
                    refitClass = max(refitClass, Refit.CLASS_OMNI);
                } else {
                    refitClass = max(refitClass, Refit.CLASS_C);
                }
            } else if (op instanceof AddPartRefitOperation) {
                refitClass = max(refitClass, calculateAddPartClass(oldUnit, newUnit, isOmniRefit,
                    (AddPartRefitOperation)op, removedParts));
            }
        }

        // check for squad size and number changes
        if (oldUnit.getEntity() instanceof Infantry
            && !(oldUnit.getEntity() instanceof BattleArmor)) {
            refitClass = max(refitClass, calculateInfantryRefit(oldUnit, newUnit));
        }

        refitClass = max(refitClass, calculateCaseRefit(oldUnit, newUnit, isOmniRefit));

        return refitClass;
    }

    private int calculateAddPartClass(Unit oldUnit, Unit newUnit, boolean isOmniRefit, 
        AddPartRefitOperation op, List<Part> removedParts) {
        Part nPart = op.getPart();

        /*CHECK REFIT CLASS*/
        if (nPart instanceof MissingEnginePart) {
            if (oldUnit.getEntity().getEngine().getRating() != newUnit.getEntity().getEngine().getRating()) {
                return Refit.CLASS_D;
            }
            if (newUnit.getEntity().getEngine().getEngineType() != oldUnit.getEntity().getEngine().getEngineType()) {
                return Refit.CLASS_F;
            }
        } else if (nPart instanceof MissingMekGyro) {
            return Refit.CLASS_F;
        } else if (nPart instanceof MissingMekLocation) {
            if (((Mech)newUnit.getEntity()).hasTSM() != ((Mech)oldUnit.getEntity()).hasTSM()) {
                return Refit.CLASS_E;
            } else {
                return Refit.CLASS_F;
            }
        } else if (nPart instanceof Armor) {
            return Refit.CLASS_C;
        } else if (nPart instanceof MissingMekCockpit) {
            return Refit.CLASS_F;
        } else if (nPart instanceof MissingMekActuator) {
            if (isOmniRefit && nPart.isOmniPoddable()) {
                return Refit.CLASS_OMNI;
            } else {
                return Refit.CLASS_D;
            }
        } else if (nPart instanceof MissingInfantryMotiveType
            || nPart instanceof MissingInfantryArmorPart) {
            return Refit.CLASS_A;
        } else {
            int refitClass = Refit.NO_CHANGE;

            //determine whether this is A, B, or C
            if (nPart instanceof MissingEquipmentPart || nPart instanceof AmmoBin) {
                // CAW TODO: Figure out just what exactly WHY this does what this does
                //           and try to make it better...as in not rely on removing
                //           parts from a list (i.e. add detection somewhere up the
                //           chain, probably in the Diff tool)
                nPart.setUnit(newUnit);
                int loc = -1;
                EquipmentType type = null;
                if(nPart instanceof MissingEquipmentPart) {
                    type = ((MissingEquipmentPart)nPart).getType();
                } else {
                    type = ((AmmoBin)nPart).getType();
                }

                int crits = type.getCriticals(newUnit.getEntity());
                nPart.setUnit(oldUnit);

                int i = -1;
                boolean matchFound = false;
                int matchIndex = -1;
                refitClass = Refit.CLASS_D;
                for (Part oPart : removedParts) {
                    i++;
                    int oLoc = -1;
                    int oCrits = -1;
                    EquipmentType oType = null;
                    if (oPart instanceof EquipmentPart) {
                        oLoc = ((EquipmentPart)oPart).getLocation();
                        oType = ((EquipmentPart)oPart).getType();
                        oCrits = oType.getCriticals(oldUnit.getEntity());
                    } else if (oPart instanceof MissingEquipmentPart) {
                        oLoc = ((MissingEquipmentPart)oPart).getLocation();
                        oType = ((MissingEquipmentPart)oPart).getType();
                        oCrits = oType.getCriticals(oldUnit.getEntity());
                    }
                    
                    if (loc != oLoc) {
                        continue;
                    }

                    if (crits == oCrits
                        && (type.hasFlag(WeaponType.F_LASER) == oType.hasFlag(WeaponType.F_LASER))
                        && (type.hasFlag(WeaponType.F_MISSILE) == oType.hasFlag(WeaponType.F_MISSILE))
                        && (type.hasFlag(WeaponType.F_BALLISTIC) == oType.hasFlag(WeaponType.F_BALLISTIC))
                        && (type.hasFlag(WeaponType.F_ARTILLERY) == oType.hasFlag(WeaponType.F_ARTILLERY))) {
                        refitClass = Refit.CLASS_A;
                        matchFound = true;
                        matchIndex = i;
                        break;
                    } else if (crits <= oCrits) {
                        refitClass = Refit.CLASS_B;
                        matchFound = true;
                        matchIndex = i;
                        //don't break because we may find something better
                    } else {
                        refitClass = Refit.CLASS_C;
                        matchFound = true;
                        matchIndex = i;
                        //don't break because we may find something better
                    }
                }

                if (isOmniRefit && nPart.isOmniPoddable()) {
                    refitClass = Refit.CLASS_OMNI;
                }

                if (matchFound) {
                    removedParts.remove(matchIndex);
                }
            }

            return refitClass;
        }

        return Refit.NO_CHANGE;
    }

    private int calculateInfantryRefit(Unit oldUnit, Unit newUnit) {
        Entity oldEntity = oldUnit.getEntity();
        Entity newEntity = newUnit.getEntity();
        if(((Infantry)oldEntity).getSquadN() != ((Infantry)newEntity).getSquadN()
            ||((Infantry)oldEntity).getSquadSize() != ((Infantry)newEntity).getSquadSize()) {
            return Refit.CLASS_A;
        }
        
        return Refit.NO_CHANGE;
    }

    private Integer calculateCaseRefit(Unit oldUnit, Unit newUnit, boolean isOmniRefit) {
        Entity oldEntity = oldUnit.getEntity();
        Entity newEntity = newUnit.getEntity();
        
        int refitClass = Refit.NO_CHANGE;
        for(int loc = 0; loc < newEntity.locations(); loc++) {
            if((newEntity.locationHasCase(loc) != oldEntity.locationHasCase(loc)
                    && !(newEntity.isClan() && newEntity instanceof Mech))
                    || (newEntity instanceof Mech
                            && ((Mech)newEntity).hasCASEII(loc) != ((Mech)oldEntity).hasCASEII(loc))) {
                if(isOmniRefit) {
                    refitClass = max(refitClass, Refit.CLASS_OMNI);
                } else {
                    refitClass = max(refitClass, Refit.CLASS_E);
                }
            }
        }

        return refitClass;
    }

    private int calculateRefurbishClass(Unit oldUnit, Unit newUnit, List<RefitOperation> operations) {
        return Refit.CLASS_E;
    }

    private boolean calcIsOmniRefit(Unit oldUnit, Unit newUnit) {
        return oldUnit.getEntity().isOmni() && newUnit.getEntity().isOmni();
    }

    private int max(int class0, int class1) {
        return Math.max(class0, class1);
    }
}