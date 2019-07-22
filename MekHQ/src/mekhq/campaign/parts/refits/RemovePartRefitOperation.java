package mekhq.campaign.parts.refits;

import mekhq.campaign.parts.Part;

public class RemovePartRefitOperation extends RefitOperation {
    private RemovedPart comparison;

    public RemovePartRefitOperation(RemovedPart comparison) {
        this.comparison = comparison;
    }

    public Part getPart() {
        return getComparison().getOldPart().get();
    }

    public RemovedPart getComparison() {
        return comparison;
    }

    @Override
    public int getLocation() {
        return getComparison().getOldPart()
            .map(Part::getLocation)
            .orElse(super.getLocation());
    }

    @Override
    public int getTime() {
        return getComparison().getOldPart()
            .map(p -> {
                boolean isSalvaging = p.getUnit().isSalvage();
                p.getUnit().setSalvage(true);
                int time = p.getBaseTime();
                p.getUnit().setSalvage(isSalvaging);
                return time;
            })
            .orElse(super.getTime());
    }
}