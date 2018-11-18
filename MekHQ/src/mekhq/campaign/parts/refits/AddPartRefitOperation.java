package mekhq.campaign.parts.refits;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;

public class AddPartRefitOperation extends RefitOperation {
    private AddedPart part;

    public AddPartRefitOperation(AddedPart part) {
        this.part = part;
    }

    public Part getPart() {
        return this.part.getNewPart().get();
    }

    @Override
    public int getTime() {
        Part nPart = part.getNewPart().get();
        if (nPart instanceof MissingPart) {
            return nPart.getBaseTime();
        }

        // We don't have a time for this (?)
        return 0;
    }

    @Override
    public List<Part> getShoppingList() {
        Part nPart = part.getNewPart().get();

        //its a new part
        //dont actually add the part iself but rather its missing equivalent
        //except in the case of armor
        List<Part> newPartList = new ArrayList<>();
        if(nPart instanceof Armor || nPart instanceof AmmoBin) {
            newPartList.add(nPart);
        } else if (nPart instanceof MissingPart) {
            newPartList.add(nPart);
        }

        return newPartList;
    }
}
