package mekhq.campaign.parts.refits;

import java.util.Collections;
import java.util.List;

public class UnitDiffResults {
    private static UnitDiffResults FAILED = new FailedUnitDiffResults();
    private List<PartComparison> comparisons;

    public static UnitDiffResults failed() {
        return FAILED;
    }

    public UnitDiffResults(List<PartComparison> comparisons) {
        this.comparisons = comparisons;
    }

    public boolean success() {
        return true;
    }

    public boolean hasDifferences() {
        return comparisons.stream().anyMatch(pc -> pc.hasDifferences());
    }

    public List<PartComparison> getComparisons() {
        return Collections.unmodifiableList(this.comparisons);
    }

    private static class FailedUnitDiffResults extends UnitDiffResults {
        public FailedUnitDiffResults() {
            super(Collections.emptyList());
        }

        @Override
        public boolean success() {
            return false;
        }
    }
}
