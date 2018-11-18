package mekhq.campaign.parts.refits;

public class MovePartRefitOperation extends RefitOperation {
    private MovedPart part;

    public MovePartRefitOperation(MovedPart part) {
        this.part = part;
    }

    @Override
    public int getLocation() {
        return this.part.getNewPart()
            .map(p -> p.getLocation())
            .orElse(super.getLocation());
    }

	public boolean isOmniPodded() {
        return this.part.getNewPart()
            .map(p -> p.isOmniPodded())
            .orElse(false);
	}
}