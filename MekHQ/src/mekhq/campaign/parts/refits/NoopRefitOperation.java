package mekhq.campaign.parts.refits;

public class NoopRefitOperation extends RefitOperation {
    private PartComparison comparison;
    
    public NoopRefitOperation(PartComparison c) {
        this.comparison = c;
    }
}
