package mekhq.campaign.parts.refits;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import megamek.common.Aero;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.logging.LogLevel;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestTank;
import megamek.common.weapons.InfantryAttack;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.AeroLifeSupport;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.VeeStabiliser;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public class GenericUnitDiffer implements IUnitDiffer {
    public UnitDiffResults diff(Unit oldUnit, Unit newUnit) {
        List<PartComparison> comparisons = new ArrayList<>();

        Entity oldEntity = oldUnit.getEntity();
        Entity newEntity = newUnit.getEntity();

        boolean isOmniRefit = oldUnit.getEntity().isOmni() && newUnit.getEntity().isOmni();
        if (isOmniRefit && !Utilities.isOmniVariant(oldUnit.getEntity(), newEntity)) {
            return UnitDiffResults.failed(oldUnit, newUnit);
        }

        boolean sameArmorType = newEntity.getArmorType(0) == oldUnit.getEntity().getArmorType(0);

        boolean didCrewSizeChange = crewSizeChanged(oldEntity, newEntity);

        //Step 1: put all of the parts from the current unit into a new arraylist so they can
        //be removed when we find a match.
        List<Part> oldUnitParts = new ArrayList<>();
        for(Part p : oldUnit.getParts()) {
            if ((!isOmniRefit || p.isOmniPodded())
                    || (p instanceof TransportBayPart)) {
                oldUnitParts.add(p);
            }
        }

        //Step 2a: loop through the parts arraylist in the newUnit and attempt to find the
        //corresponding part of missing part in the parts arraylist we just created. Depending on
        //what we find, we may have:
        //a) An exact copy in the same location - we move the part from the oldunit parts to the
        //newunit parts. Nothing needs to be changed in terms of refit class, time, or anything.
        //b) An exact copy in a different location - move this part to the newunit part list, but
        //change its location id. Change refit class to C and add time for removing and reinstalling
        //part.
        //c) We dont find the part in the oldunit part list.  That means this is a new part.  Add
        //this to the newequipment arraylist from step 3.  Don't change anything in terms of refit
        //stats yet, that will happen later.
        List<Part> partsRemaining = new ArrayList<>();
        for(Part part : newUnit.getParts()) {
            Part foundOldPart = null;
            int i = -1;
            for(Part oPart : oldUnitParts) {
                i++;

                //Part oPart = oldUnit.campaign.getPart(pid);
                if (isOmniRefit && !oPart.isOmniPodded()) {
                    continue;
                }

                //FIXME: There have been instances of null oParts here. Save/load will fix these, but
                //I would like to figure out the source. From experimentation, I think it has to do with
                //cancelling a prior refit.
                if ((oPart instanceof MissingPart && ((MissingPart)oPart).isAcceptableReplacement(part, true))
                        || oPart.isSamePartType(part)
                        // We're not going to require replacing the life support system just because the
                        // number of bay personnel changes.
                        || ((oPart instanceof AeroLifeSupport)
                                && (part instanceof AeroLifeSupport)
                                && !didCrewSizeChange)) {
                    //need a special check for location and armor amount for armor
                    if (oPart instanceof Armor
                            && (((Armor)oPart).getLocation() != ((Armor)part).getLocation()
                                    || ((Armor)oPart).getTotalAmount() != ((Armor)part).getTotalAmount())) {
                        continue;
                    }

                    if ((oPart instanceof VeeStabiliser)
                            && (oPart.getLocation() != part.getLocation())) {
                        continue;
                    }

                    if (part instanceof EquipmentPart) {
                        //check the location to see if this moved. If so, then don't break, but
                        //save this in case we fail to find equipment in the same location.
                        int loc = ((EquipmentPart)part).getLocation();
                        boolean rear = ((EquipmentPart)part).isRearFacing();
                        if((oPart instanceof EquipmentPart
                                && (((EquipmentPart)oPart).getLocation() != loc || ((EquipmentPart)oPart).isRearFacing() != rear))
                                || (oPart instanceof MissingEquipmentPart
                                        && (((MissingEquipmentPart)oPart).getLocation() != loc || ((MissingEquipmentPart)oPart).isRearFacing() != rear))) {
                            continue;
                        }
                    }

                    foundOldPart = oPart;
                    break;
                }
            }

            if (null != foundOldPart) {
                oldUnitParts.remove(i);
                comparisons.add(new UnchangedPart(newUnit, foundOldPart, part));
            } else {
                // Address new and moved parts next
                partsRemaining.add(part);
            }
        }

        // Step 2b: Find parts that moved or add them as new parts
        for (Part part : partsRemaining) {
            Part movedPart = null;
            int moveIndex = 0;
            int i = -1;
            for (Part oPart : oldUnitParts) {
                i++;
                if (isOmniRefit && !oPart.isOmniPodded()) {
                    continue;
                }
                //FIXME: There have been instances of null oParts here. Save/load will fix these, but
                //I would like to figure out the source. From experimentation, I think it has to do with
                //cancelling a prior refit.
                if ((oPart instanceof MissingPart && ((MissingPart)oPart).isAcceptableReplacement(part, true))
                        || oPart.isSamePartType(part)
                        // We're not going to require replacing the life support system just because the
                        // number of bay personnel changes.
                        || ((oPart instanceof AeroLifeSupport)
                                && (part instanceof AeroLifeSupport)
                                && !didCrewSizeChange)) {

                                    //need a special check for location and armor amount for armor
                    if(oPart instanceof Armor
                            && (((Armor)oPart).getLocation() != ((Armor)part).getLocation()
                                    || ((Armor)oPart).getTotalAmount() != ((Armor)part).getTotalAmount())) {
                        continue;
                    }

                    if ((oPart instanceof VeeStabiliser)
                            && (oPart.getLocation() != part.getLocation())) {
                        continue;
                    }

                    if (part instanceof EquipmentPart) {
                        //check the location to see if this moved. If so, then don't break, but
                        //save this in case we fail to find equipment in the same location.
                        int loc = ((EquipmentPart)part).getLocation();
                        boolean rear = ((EquipmentPart)part).isRearFacing();
                        if((oPart instanceof EquipmentPart
                                && (((EquipmentPart)oPart).getLocation() != loc || ((EquipmentPart)oPart).isRearFacing() != rear))
                                || (oPart instanceof MissingEquipmentPart
                                        && (((MissingEquipmentPart)oPart).getLocation() != loc || ((MissingEquipmentPart)oPart).isRearFacing() != rear))) {
                            movedPart = oPart;
                            moveIndex = i;
                            break;
                        }
                    }
                }
            }

            // Actually move the part or add the new part
            if (null != movedPart) {
                comparisons.add(new MovedPart(newUnit, movedPart, part));
                oldUnitParts.remove(moveIndex);
            } else {
                //its a new part
                //dont actually add the part iself but rather its missing equivalent
                //except in the case of armor
                if(part instanceof Armor || part instanceof AmmoBin) {
                    comparisons.add(new AddedPart(newUnit, part));
                } else {
                    MissingPart mPart = part.getMissingPart();
                    if(null != mPart) {
                        mPart.setUnit(newUnit);
                        comparisons.add(new AddedPart(newUnit, mPart));
                    } else {
                        MekHQ.getLogger().log(getClass(), "diff", LogLevel.ERROR,
                                "null missing part for " + part.getName() + " during refit calculations"); //$NON-NLS-1$
                    }
                }
            }
        }

        //Step 4: loop through remaining equipment on oldunit parts and add time for removing.
        for (Part oPart : oldUnitParts) {
            //Part oPart = oldUnit.campaign.getPart(pid);
            //We're pretending we're changing the old suit rather than removing it.
            //We also want to avoid accounting for legacy InfantryAttack parts.
            if ((oPart instanceof BattleArmorSuit)
                    || (oPart instanceof TransportBayPart)
                    || ((oPart instanceof EquipmentPart
                            && ((EquipmentPart)oPart).getType() instanceof InfantryAttack))) {
                continue;
            }
            if (oPart instanceof MissingPart) {
                continue;
            }
            if (oPart instanceof AmmoBin) {
                int remainingShots = ((AmmoBin)oPart).getFullShots() - ((AmmoBin)oPart).getShotsNeeded();
                if (remainingShots > 0) {
                    comparisons.add(new RemovedPart(newUnit, oPart));
                }
                continue;
            }
            if (oPart instanceof Armor && sameArmorType) {
                // Search for the armor we 'added' and make it a modification instead
                Armor existingArmor = ((Armor)oPart);
                Optional<PartComparison> addedArmor = findMatchingArmor(comparisons, existingArmor);
                if (addedArmor.isPresent()) {
                    PartComparison comparison = addedArmor.get();
                    comparisons.remove(comparison);
                    comparisons.add(new ModifiedPart(newUnit, oPart, comparison.getNewPart().get()));
                    continue;
                }
            }
            comparisons.add(new RemovedPart(newUnit, oPart));
        }

        /*
         * Figure out how many untracked heat sinks are needed to complete the refit or will
         * be removed. These are engine integrated heat sinks for Mechs or ASFs that change
         * the heat sink type or heat sinks required for energy weapons for vehicles and
         * conventional fighters.
         */
        if ((newEntity instanceof Mech)
            || ((newEntity instanceof Aero) && !(newEntity instanceof ConvFighter))) {
            Part oldHS = heatSinkPart(oldEntity);
            Part newHS = heatSinkPart(newEntity);
            int oldCount = untrackedHeatSinkCount(oldEntity);
            int newCount = untrackedHeatSinkCount(newEntity);
            if (oldHS.isSamePartType(newHS)) {
                // If the number changes we need to add them to either the warehouse at the end of
                // refit or the shopping list at the beginning.
                for (int i = 0; i < oldCount - newCount; i++) {
                    comparisons.add(new RemovedPart(newUnit, oldHS.clone()));
                }
                for (int i = 0; i < newCount - oldCount; i++) {
                    comparisons.add(new AddedPart(newUnit, oldHS.clone()));
                }
            } else {
                for (int i = 0; i < oldCount; i++) {
                    comparisons.add(new RemovedPart(newUnit, oldHS.clone()));
                }
                for (int i = 0; i < newCount; i++) {
                    comparisons.add(new AddedPart(newUnit, newHS.clone()));
                }
            }
        } else if ((newEntity instanceof Tank)
                || (newEntity instanceof ConvFighter)) {
            int oldHS = untrackedHeatSinkCount(oldEntity);
            int newHS = untrackedHeatSinkCount(newEntity);
            // We're only concerned with heat sinks that have to be installed in excess of what
            // may be provided by the engine.
            if (oldUnit.getEntity().hasEngine()) {
                oldHS = Math.max(0, oldHS - oldUnit.getEntity().getEngine().integralHeatSinkCapacity(false));
            }
            if (newEntity.hasEngine()) {
                newHS = Math.max(0, newHS - newEntity.getEngine().integralHeatSinkCapacity(false));
            }
            if (oldHS != newHS) {
                Part hsPart = heatSinkPart(newEntity); // only single HS allowed, so they have to be of the same type
                for (int i = oldHS; i < newHS; i++) {
                    comparisons.add(new AddedPart(newUnit, hsPart.clone()));
                }
                for (int i = newHS; i < oldHS; i++) {
                    comparisons.add(new RemovedPart(newUnit, hsPart.clone()));
                }
            }
        }

        /*
		 * Cargo and transport bays are essentially just open space and while it may take time and materials
		 * to change the cubicles or the number of doors, the bay itself does not require any refit work
		 * unless the size changes. First we create a list of all bays on each unit, then we attempt to
		 * match them by size and number of doors. Any remaining are matched on size, and difference in
		 * number of doors is noted as moving doors has to be accounted for in the time calculation.
		 */
        Map<Integer, TransportBayPart> oldBayParts = oldUnit.getParts().stream().filter(p -> p instanceof TransportBayPart)
            .map(p -> (TransportBayPart)p)
            .filter(p -> !p.getBay().isQuarters())
            .collect(Collectors.toMap(p -> p.getBayNumber(), p -> p));
        List<TransportBayPart> newBayParts = newUnit.getParts().stream().filter(p -> p instanceof TransportBayPart)
            .map(p -> (TransportBayPart)p)
            .filter(p -> !p.getBay().isQuarters())
            .collect(Collectors.toList());
        for (TransportBayPart newBayPart : newBayParts) {
            TransportBayPart oldBayPart = oldBayParts.remove(newBayPart.getBayNumber());
            if (null != oldBayPart) {
                // Modified?
                if (oldBayPart.getBay().getCapacity() != newBayPart.getBay().getCapacity()
                    || oldBayPart.getBay().getDoors() != newBayPart.getBay().getDoors())
                {
                    comparisons.add(new ModifiedPart(newUnit, oldBayPart, newBayPart));
                }
            } else {
                // Add.
                comparisons.add(new AddedPart(newUnit, newBayPart));
            }
        }

        // The bays left have been removed.
        for (TransportBayPart oldBayPart : oldBayParts.values()) {
            comparisons.add(new RemovedPart(newUnit, oldBayPart));
        }

        return new UnitDiffResults(oldUnit, newUnit, comparisons);
    }

    private Optional<PartComparison> findMatchingArmor(List<PartComparison> comparisons, Armor existingArmor) {
        return comparisons.stream()
                .filter(pc -> pc instanceof AddedPart && isArmorMatch(pc.getNewPart(), existingArmor)).findFirst();
    }

    private boolean isArmorMatch(Optional<Part> newPart, Armor existingArmor) {
        return newPart.isPresent()
            && newPart.get() instanceof Armor
            && newPart.get().getLocation() == existingArmor.getLocation();
    }

    /**
     * Requiring the life support system to be changed just because the number of bay personnel changes
     * is a bit much. Instead we'll limit it to changes in crew size, measured by quarters.
     * @return true if the crew quarters capacity changed.
     */
    private boolean crewSizeChanged(Entity oldEntity, Entity newEntity) {
        int oldCrew = oldEntity.getTransportBays()
                .stream().filter(b -> b.isQuarters())
                .mapToInt(b -> (int) b.getCapacity())
                .sum();
        int newCrew = newEntity.getTransportBays()
                .stream().filter(b -> b.isQuarters())
                .mapToInt(b -> (int) b.getCapacity())
                .sum();
        return oldCrew != newCrew;
    }


    /**
     * Refits may require adding or removing heat sinks that are not tracked as parts. For Mechs and
     * ASFs this would be engine-integrated heat sinks if the heat sink type is changed. For vehicles and
     * conventional fighters this would be heat sinks required by energy weapons.
     *
     * @param entity Either the starting or the ending unit of the refit.
     * @return       The number of heat sinks the unit mounts that are not tracked as parts.
     */
    private int untrackedHeatSinkCount(Entity entity) {
        if (entity instanceof Mech) {
            return Math.min(((Mech) entity).heatSinks(), entity.getEngine().integralHeatSinkCapacity(((Mech) entity).hasCompactHeatSinks()));
        } else if ((entity instanceof Aero)
                && (entity.getEntityType() & (Entity.ETYPE_CONV_FIGHTER | Entity.ETYPE_SMALL_CRAFT | Entity.ETYPE_JUMPSHIP)) == 0) {
            return entity.getEngine().integralHeatSinkCapacity(false);
        } else {
            EntityVerifier verifier = EntityVerifier.getInstance(new File(
                    "data/mechfiles/UnitVerifierOptions.xml"));
            TestEntity te = null;
            if (entity instanceof Tank) {
                te = new TestTank((Tank) entity, verifier.tankOption, null);
                return te.getCountHeatSinks();
            } else if (entity instanceof ConvFighter) {
                te = new TestAero((Aero) entity, verifier.aeroOption, null);
                return te.getCountHeatSinks();
            } else {
                return 0;
            }
        }
    }

    /**
     * Creates an independent heat sink part appropriate to the unit that can be used to track
     * needed and leftover parts for heat sinks that are not actually tracked by the unit.
     *
     * @param entity Either the original or the new unit.
     * @return       The part corresponding to the type of heat sink for the unit.
     */
    private Part heatSinkPart(Entity entity) {
        if (entity instanceof Aero) {
            return new AeroHeatSink(0, ((Aero) entity).getHeatType(), false, null);
        } else if (entity instanceof Mech) {
            Optional<Mounted> mount = entity.getMisc().stream()
                    .filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
                            || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
                    .findAny();
            if (mount.isPresent()) {
                return new HeatSink(0, mount.get().getType(), -1, false, null);
            }
        }
        return new HeatSink(0, EquipmentType.get("Heat Sink"), -1, false, null);
    }
}
