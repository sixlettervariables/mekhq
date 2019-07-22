package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.equipment.EquipmentPart;

public class MoveEquipmentPartRefitOperation extends MovePartRefitOperation {
    private MovedPart part;

    public MoveEquipmentPartRefitOperation(MovedPart part) {
        super(part);
        
        if (!(part.getNewPart().get() instanceof EquipmentPart)) {
            // TODO: throw.
        }
        this.part = part;
    }

    @Override
    public int getTime() {
        return part.getNewPart()
            .map(p -> {
                p.getUnit().setSalvage(true);
                int time = p.getBaseTime();
                p.getUnit().setSalvage(false);
                return time;
            }).orElse(0);
    }
}