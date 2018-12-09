package mekhq.campaign.parts.refits;

import java.util.Collections;
import java.util.List;

import megamek.common.Entity;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

public class UnitDiffResults {
    private List<PartComparison> comparisons;
    private Unit oldUnit;
    private Unit newUnit;

    public static UnitDiffResults failed(Unit oldUnit, Unit newUnit) {
        return new FailedUnitDiffResults(oldUnit, newUnit);
    }

    public UnitDiffResults(Unit oldUnit, Unit newUnit, List<PartComparison> comparisons) {
        this.oldUnit = oldUnit;
        this.newUnit = newUnit;
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

    public Unit getOldUnit() {
        return oldUnit;
    }

    public Unit getNewUnit() {
        return newUnit;
    }

    public String explain() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s -> %s\n", getOldUnit().getEntity().getShortName(), getNewUnit().getEntity().getShortName()));
        for (PartComparison comparison : getComparisons()) {
            if (comparison instanceof RemovedPart || comparison instanceof ModifiedPart || comparison instanceof MovedPart) {
                Part oldPart = comparison.getOldPart().get();
                builder.append(String.format("- %s", oldPart.getName()));
                if (oldPart.getLocation() != Entity.LOC_NONE) {
                    builder.append(String.format(" (%s)", oldUnit.getEntity().getLocationName(oldPart.getLocation())));
                }
                builder.append('\n');
            }
            if (comparison instanceof AddedPart || comparison instanceof ModifiedPart || comparison instanceof MovedPart) {
                Part newPart = comparison.getNewPart().get();
                builder.append(String.format("+ %s", newPart.getName()));
                if (newPart.getLocation() != Entity.LOC_NONE) {
                    builder.append(String.format(" (%s)", newUnit.getEntity().getLocationName(newPart.getLocation())));
                }
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    private static class FailedUnitDiffResults extends UnitDiffResults {
        public FailedUnitDiffResults(Unit oldUnit, Unit newUnit) {
            super(oldUnit, newUnit, Collections.emptyList());
        }

        @Override
        public boolean success() {
            return false;
        }
    }
}
