package mekhq.campaign.parts.refits;

public class ModifyPartRefitOperation extends RefitOperation {
    private ModifiedPart comparison;

    public ModifyPartRefitOperation(ModifiedPart comparison) {
        this.comparison = comparison;
    }
}
