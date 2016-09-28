/*
 * IMekHqModule.java
 *
 * Copyright (c) 2016 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.module;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.Version;
import mekhq.campaign.Campaign;

/**
 * @author Neoancient
 *
 */
public interface IMekHqModule extends MekHqXmlSerializable {

	void initModule(Campaign campaign);
	boolean wantsEvents();
	void loadFieldsFromXmlNode(Node node, Campaign campaign);
	
	public static IMekHqModule generateInstanceFromXML(Node wn, Campaign c, Version version) {
		IMekHqModule retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();

		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (IMekHqModule) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn, c);
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		
		return retVal;		
	}
}
