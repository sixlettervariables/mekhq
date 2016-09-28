/*
 * AgainstTheBot.java
 *
 * Copyright (c) 2016 Carl Spain. All rights reserved.
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
package mekhq.module.atb;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingFactory;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.module.IMekHqModule;

/**
 * @author Neoancient
 *
 */
public class AgainstTheBot implements IMekHqModule {
		
	private Campaign campaign;
	private AtBData atbData;
	
    private HashMap<Integer, Lance> lances = new HashMap<>();
	private int fatigueLevel = 0;
    private RetirementDefectionTracker retirementDefectionTracker = new RetirementDefectionTracker();
	
	public void initModule(Campaign campaign) {
		this.campaign = campaign;
		atbData = AtBData.loadFromXml();
	}
	
	public AtBData getAtBData() {
		return atbData;
	}
	
	public Lance getLance(int id) {
		return lances.get(id);
	}
	
	public void createLance(int forceId) {
		lances.put(forceId, new Lance(forceId, campaign));
	}
	
	public void removeLance(int forceId) {
		lances.remove(forceId);
	}
	
    /** Adds force and all its subforces to the AtB lance table
     */

    public void addAllLances(Force force) {
		if (force.getUnits().size() > 0) {
			lances.put(force.getId(), new Lance(force.getId(), campaign));
		}
		for (Force f : force.getSubForces()) {
			addAllLances(f);
		}
    }

    public HashMap<Integer, Lance> getLances() {
    	return lances;
    }
	
    public List<Lance> getLanceList() {
    	return lances.values().stream().filter(l -> campaign.getForce(l.getForceId()) != null)
    			.collect(Collectors.toList());
    }

    public AtBContract getAttachedAtBContract(Unit unit) {
		if (null != unit &&
				null != lances.get(unit.getForceId())) {
			return lances.get(unit.getForceId()).getContract(campaign);
		}
		return null;
    }

    public int getDeploymentDeficit(AtBContract contract) {
    	int total = -contract.getRequiredLances();
    	int role = -Math.max(1, contract.getRequiredLances() / 2);
    	for (Lance l : lances.values()) {
    		if (l.getMissionId() == contract.getId() && l.getRole() != Lance.ROLE_UNASSIGNED) {
    			total++;
    			if (l.getRole() == contract.getRequiredLanceType()) {
    				role++;
    			}
    		}
    	}

    	if (total >= 0 && role >= 0) {
    		return 0;
    	}
    	return Math.max(total, role);
    }

    public RetirementDefectionTracker getRetirementDefectionTracker() {
    	return retirementDefectionTracker;
    }
    
    public void setRetirementDefectionTracker(RetirementDefectionTracker tracker) {
    	retirementDefectionTracker = tracker;
    }

    public int getFatigueLevel() {
    	return fatigueLevel;
    }
    
    public void setFatigueLevel(int level) {
    	fatigueLevel = level;
    }
    
    public boolean applyRetirement(long totalPayout, HashMap<UUID, UUID> unitAssignments) {
		if (null != getRetirementDefectionTracker().getRetirees()) {
			if (campaign.getFinances().debit(totalPayout,
					Transaction.C_SALARY, "Final Payout", campaign.getDate())) {
				for (UUID pid : getRetirementDefectionTracker().getRetirees()) {
					if (campaign.getPerson(pid).isActive()) {
						campaign.changeStatus(campaign.getPerson(pid), Person.S_RETIRED);
						campaign.addReport(campaign.getPerson(pid).getFullName() + " has retired.");
					}
					if (Person.T_NONE != getRetirementDefectionTracker().getPayout(pid).getRecruitType()) {
						campaign.getPersonnelMarket().addPerson(campaign.newPerson(getRetirementDefectionTracker().getPayout(pid).getRecruitType()));
					}
					if (getRetirementDefectionTracker().getPayout(pid).hasHeir()) {
						Person p = campaign.newPerson(campaign.getPerson(pid).getPrimaryRole());
						p.setOriginalUnitWeight(campaign.getPerson(pid).getOriginalUnitWeight());
						p.setOriginalUnitTech(campaign.getPerson(pid).getOriginalUnitTech());
						p.setOriginalUnitId(campaign.getPerson(pid).getOriginalUnitId());
						if (unitAssignments.containsKey(pid)) {
							campaign.getPersonnelMarket().addPerson(p, campaign.getUnit(unitAssignments.get(pid)).getEntity());
						} else {
							campaign.getPersonnelMarket().addPerson(p);
						}
					}
					int dependents = getRetirementDefectionTracker().getPayout(pid).getDependents();
			    	while(dependents > 0 ) {
			    		Person p = campaign.newPerson(Person.T_ASTECH);
			    		p.setDependent(true);
			    		if (campaign.recruitPerson(p)) {
			    			dependents--;
			    		} else {
			    			dependents = 0;
			    		}
			    	}
					if (unitAssignments.containsKey(pid)) {
						campaign.removeUnit(unitAssignments.get(pid));
					}
				}
				getRetirementDefectionTracker().resolveAllContracts();
				return true;
			} else {
				campaign.addReport("<font color='red'>You cannot afford to make the final payments.</font>");
			}
		}
		return false;
    }


    public boolean checkRetirementDefections() {
        if (getRetirementDefectionTracker().getRetirees().size() > 0) {
            Object[] options = { "Show Payout Dialog", "Cancel" };
            if (JOptionPane.YES_OPTION == JOptionPane
                    .showOptionDialog(
                            null,
                            "You have personnel who have left the unit or been killed in action but have not received their final payout.\nYou must deal with these payments before advancing the day.\nHere are some options:\n  - Sell off equipment to generate funds.\n  - Pay one or more personnel in equipment.\n  - Just cheat and use GM mode to edit the settlement.",
                            "Unresolved Final Payments",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[0])) {
                return true;
            }
        }
        return false;
    }

    public boolean checkYearlyRetirements() {
        if (Utilities.getDaysBetween(getRetirementDefectionTracker()
                        .getLastRetirementRoll().getTime(), campaign.getDate()) == 365) {
            Object[] options = { "Show Retirement Dialog", "Not Now" };
            if (JOptionPane.YES_OPTION == JOptionPane
                    .showOptionDialog(
                            null,
                            "It has been a year since the last retirement/defection roll, and it is time to do another.",
                            "Retirement/Defection roll required",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[0])) {
                return true;
            }
        }
        return false;
    }
	
	@Subscribe
	public void processNewDay(NewDayEvent event) {
		campaign.getContractMarket().generateContractOffers(campaign);
		campaign.getUnitMarket().generateUnitOffers(campaign);
		
		if (campaign.getCalendar().get(Calendar.DAY_OF_YEAR) == 1) {
			adjustDependents();
		}
		
		if (campaign.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) {
        	updateFactionTables();
            checkMorale();
            checkFatigue();
		}

		for (Mission m : campaign.getMissions()) {
    		if (!m.isActive() || !(m instanceof AtBContract) ||
					campaign.getDate().before(((Contract)m).getStartDate())) {
    			continue;
    		}
    		/* Situations like a delayed start or running out of funds during transit
    		 * can delay arrival until after the contract start. In that case, shift the
    		 * starting and ending dates before making any battle rolls. We check that the
    		 * unit is actually on route to the planet in case the user is using a custom system
    		 * for transport or splitting the unit, etc.
    		 */
    		if (!campaign.getLocation().isOnPlanet() && //null != getLocation().getJumpPath() &&
    				campaign.getLocation().getJumpPath().getLastPlanet().getId().equals(m.getPlanetName())) {
    			/*transitTime is measured in days; round up to the next
    			 * whole day, then convert to milliseconds */
    			GregorianCalendar cal = (GregorianCalendar)campaign.getCalendar().clone();
    			cal.add(Calendar.DATE, (int)Math.ceil(campaign.getLocation().getTransitTime()));
    			((AtBContract)m).getStartDate().setTime(cal.getTimeInMillis());
    			cal.add(Calendar.MONTH, ((AtBContract)m).getLength());
    			((AtBContract)m).getEndingDate().setTime(cal.getTimeInMillis());
    			campaign.addReport("The start and end dates of " + m.getName() +
    					" have been shifted to reflect the current ETA.");
    			continue;
       		}
        	if (campaign.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
        		int deficit = getDeploymentDeficit((AtBContract)m);
        		if (deficit > 0) {
        			((AtBContract)m).addPlayerMinorBreaches(deficit);
        			campaign.addReport("Failure to meet " + m.getName() +
        					" requirements resulted in " + deficit +
        					((deficit==1)?" minor contract breach":" minor contract breaches"));
        		}
        	}

    		for (Scenario s : m.getScenarios()) {
    			if (!s.isCurrent() || !(s instanceof AtBScenario)) {
    				continue;
    			}
    			if (s.getDate().before(campaign.getCalendar().getTime())) {
    				s.setStatus(Scenario.S_DEFEAT);
    				s.clearAllForcesAndPersonnel(campaign);
    				((AtBContract)m).addPlayerMinorBreach();
    				campaign.addReport("Failure to deploy for " + s.getName() +
    						" resulted in defeat and a minor contract breach.");
    				((AtBScenario)s).generateStub(campaign);
    			}
    		}
    	}

    	/* Iterate through the list of lances and make a battle roll for each,
    	 * then sort them by date before adding them to the campaign.
    	 * Contracts with enemy morale level of invincible have a base attack
    	 * (defender) battle each week. If there is a base attack (attacker)
    	 * battle, that is the only one for the week on that contract.
    	 */
    	if (campaign.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
    		ArrayList<AtBScenario> sList = new ArrayList<AtBScenario>();
    		AtBScenario baseAttack = null;

    		for (Lance l : lances.values()) {
    			if (null == l.getContract(campaign) || !l.getContract(campaign).isActive() ||
    					!l.isEligible(campaign) ||
    					campaign.getDate().before(l.getContract(campaign).getStartDate())) {
    				continue;
    			}
    			if (l.getRole() == Lance.ROLE_TRAINING) {
    				awardTrainingXP(l);
    			}
    			if (l.getContract(campaign).getMoraleLevel() <= AtBContract.MORALE_VERYLOW) {
    				continue;
    			}
    			AtBScenario scenario = l.checkForBattle(campaign);
    			if (null != scenario) {
    				sList.add(scenario);
    				if (scenario.getBattleType() == AtBScenario.BASEATTACK && scenario.isAttacker()) {
    					baseAttack = scenario;
    					break;
    				}
    			}
    		}

    		/* If there is a base attack (attacker), all other battles on
    		 * that contract are cleared.
    		 */
    		if (null != baseAttack) {
    			ArrayList<Scenario> sameContract = new ArrayList<Scenario>();
    			for (AtBScenario s : sList) {
    				if (s != baseAttack && s.getMissionId() == baseAttack.getMissionId()) {
    					sameContract.add(s);
    				}
    			}
    			sList.removeAll(sameContract);
    		}

    		/* Make sure invincible morale has base attack */
    		for (Mission m : campaign.getMissions()) {
    			if (m.isActive() && m instanceof AtBContract &&
    					((AtBContract)m).getMoraleLevel() == AtBContract.MORALE_INVINCIBLE) {
					boolean hasBaseAttack = false;
    				for (AtBScenario s : sList) {
    					if (s.getMissionId() == m.getId() &&
    							s.getBattleType() == AtBScenario.BASEATTACK &&
    							!s.isAttacker()) {
    						hasBaseAttack = true;
    						break;
    					}
    				}
    				if (!hasBaseAttack) {
    					/* find a lance to act as defender, giving preference
    					 * first to those assigned to the same contract,
    					 * then to those assigned to defense roles
    					 */
    					ArrayList<Lance> lList = new ArrayList<Lance>();
        				for (Lance l : lances.values()) {
        					if (l.getMissionId() == m.getId()
        							&& l.getRole() == Lance.ROLE_DEFEND
        							&& l.isEligible(campaign)) {
        						lList.add(l);
        					}
        				}
        				if (lList.size() == 0) {
        					for (Lance l : lances.values()) {
        						if (l.getMissionId() == m.getId()
        								&& l.isEligible(campaign)) {
        							lList.add(l);
        						}
        					}
        				}
        				if (lList.size() == 0) {
        					for (Lance l : lances.values()) {
        						if (l.isEligible(campaign)) {
        							lList.add(l);
        						}
        					}
        				}
        				if (lList.size() > 0) {
        					Lance lance = Utilities.getRandomItem(lList);
        					AtBScenario scenario = new AtBScenario(campaign, lance, AtBScenario.BASEATTACK, false,
        							Lance.getBattleDate(campaign.getCalendar()));
        					for (int i = 0; i < sList.size(); i++) {
        						if (sList.get(i).getLanceForceId() ==
        								lance.getForceId()) {
        							sList.set(i, scenario);
        							break;
        						}
        					}
        					if (!sList.contains(scenario)) {
        						sList.add(scenario);
        					}
        				} else {
        					//TODO: What to do if there are no lances assigned to this contract?
        				}
    				}
    			}
    		}

    		/* Sort by date and add to the campaign */
    		Collections.sort(sList, new Comparator<AtBScenario>() {
				public int compare(AtBScenario s1, AtBScenario s2) {
					return s1.getDate().compareTo(s2.getDate());
				}
    		});
    		for (AtBScenario s : sList) {
    			campaign.addScenario(s, campaign.getMission(s.getMissionId()));
    			s.setForces(campaign);
    		}
    	}

    	for (Mission m : campaign.getMissions()) {
    		if (m.isActive() && m instanceof AtBContract &&
    				!((AtBContract)m).getStartDate().after(campaign.getDate())) {
    			((AtBContract)m).checkEvents(campaign);
    		}
    		/* If there is a standard battle set for today, deploy
    		 * the lance.
    		 */
    		for (Scenario s : m.getScenarios()) {
    			if (s.getDate() != null && s.getDate().equals(campaign.getCalendar().getTime())) {
    				int forceId = ((AtBScenario)s).getLanceForceId();
    				if (null != lances.get(forceId)
    						&& !campaign.getForce(forceId).isDeployed()) {

						// If any unit in the force is under repair, don't deploy the force
						// Merely removing the unit from deployment would break with user expectation
						boolean forceUnderRepair = false;
						for (UUID uid : campaign.getForce(forceId).getAllUnits()) {
    						Unit u = campaign.getUnit(uid);
							if (null != u && u.isUnderRepair()) {
    							forceUnderRepair = true;
								break;
    						}
						}

						if(!forceUnderRepair) {
							campaign.getForce(forceId).setScenarioId(s.getId());
							s.addForces(forceId);
							for (UUID uid : campaign.getForce(forceId).getAllUnits()) {
								Unit u = campaign.getUnit(uid);
								if (null != u) {
									u.setScenarioId(s.getId());
								}
							}
						}
    				}
    			}
    		}
    	}
	}

	private void adjustDependents() {
    	int numPersonnel = 0;
    	ArrayList<Person> dependents = new ArrayList<Person>();
    	for (Person p : campaign.getPersonnel()) {
    		if (p.isActive()) {
    			numPersonnel++;
    			if (p.isDependent()) {
    				dependents.add(p);
    			}
    		}
    	}
    	int roll = Compute.d6(2) + campaign.getUnitRatingMod() - 2;
    	if (roll < 2) roll = 2;
    	if (roll > 12) roll = 12;
    	int change = numPersonnel * (roll - 5) / 100;
    	while (change < 0 && dependents.size() > 0) {
    		campaign.removePerson(Utilities.getRandomItem(dependents).getId());
    		change++;
    	}
		for (int i = 0; i < change; i++) {
    		Person p = campaign.newPerson(Person.T_ASTECH);
    		p.setDependent(true);
            p.setId(UUID.randomUUID());
            campaign.addPersonWithoutId(p, true);
    	}
	}
	
	/**
	 * 
	 */
	private void updateFactionTables() {
		RandomFactionGenerator.getInstance().updateTables(campaign.getCalendar().getTime(),
				campaign.getCurrentPlanet(), campaign.getCampaignOptions());
		IUnitRating rating = UnitRatingFactory.getUnitRating(campaign);
		rating.reInitialize();
	}
	
	/**
	 * 
	 */
	private void checkMorale() {
		for (Mission m : campaign.getMissions()) {
			if (m.isActive() && m instanceof AtBContract &&
					!((AtBContract)m).getStartDate().after(campaign.getDate())) {
				((AtBContract)m).checkMorale(campaign.getCalendar(), campaign.getUnitRatingMod());
				campaign.addReport("Enemy morale is now " +
						((AtBContract)m).getMoraleLevelName() + " on contract " +
						m.getName());
			}
		}
	}

	/**
	 * 
	 */
	private void checkFatigue() {
		if (campaign.getCampaignOptions().getTrackUnitFatigue()) {
			boolean inContract = false;
			for (Mission m : campaign.getMissions()) {
				if (!m.isActive() || !(m instanceof AtBContract) ||
						campaign.getDate().before(((Contract)m).getStartDate())) {
					continue;
				}
				switch (((AtBContract)m).getMissionType()) {
				case AtBContract.MT_GARRISONDUTY:
				case AtBContract.MT_SECURITYDUTY:
				case AtBContract.MT_CADREDUTY:
					fatigueLevel -= 1;
					break;
				case AtBContract.MT_RIOTDUTY:
				case AtBContract.MT_GUERRILLAWARFARE:
				case AtBContract.MT_PIRATEHUNTING:
					fatigueLevel += 1;
					break;
				case AtBContract.MT_RELIEFDUTY:
				case AtBContract.MT_PLANETARYASSAULT:
					fatigueLevel += 2;
					break;
				case AtBContract.MT_DIVERSIONARYRAID:
				case AtBContract.MT_EXTRACTIONRAID:
				case AtBContract.MT_RECONRAID:
				case AtBContract.MT_OBJECTIVERAID:
					fatigueLevel += 3;
					break;
				}
				inContract = true;
			}
			if (!inContract && campaign.getLocation().isOnPlanet()) {
				fatigueLevel -= 2;
			}
			fatigueLevel = Math.max(fatigueLevel, 0);
		}
	}
	
    private void awardTrainingXP(Lance l) {
		for (UUID trainerId : campaign.getForce(l.getForceId()).getAllUnits()) {
			if (campaign.getUnit(trainerId).getCommander() != null &&
					campaign.getUnit(trainerId).getCommander().getRank().isOfficer() &&
					campaign.getUnit(trainerId).getCommander().getExperienceLevel(false) > SkillType.EXP_REGULAR) {
				for (UUID traineeId : campaign.getForce(l.getForceId()).getAllUnits()) {
					for (Person p : campaign.getUnit(traineeId).getCrew()) {
						if (p.getExperienceLevel(false) < SkillType.EXP_REGULAR) {
							p.setXp(p.getXp() + 1);
							campaign.addReport(p.getHyperlinkedName() + " has gained 1 XP from training.");
						}
					}
				}
				break;
			}
		}
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<module type=\""
				+ this.getClass().getName()
				+ "\">");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "fatigueLevel", fatigueLevel);
        if (lances.size() > 0)   {
        	pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<lances>");
        	for (Lance l : lances.values()) {
        		if (campaign.getForce(l.getForceId()) != null) {
        			l.writeToXml(pw1, indent + 2);
        		}
        	}
        	pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</lances>");
            retirementDefectionTracker.writeToXml(pw1, indent + 1);
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</module>");
	}

	@Override
	public boolean wantsEvents() {
		return true;
	}

	@Override
	public void loadFieldsFromXmlNode(Node node, Campaign c) {
		NodeList nl = node.getChildNodes();
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);
			switch (wn.getNodeName()) {
			case "fatigueLevel":
				fatigueLevel = Integer.parseInt(wn.getTextContent());
				break;
			case "lances":
				processLanceNode(wn);
				break;
			case "retirementDefectionTracker":
				retirementDefectionTracker = RetirementDefectionTracker.generateInstanceFromXML(wn, c);
				break;
			}
		}
	}

    public void processLanceNode(Node wn) {
        NodeList wList = wn.getChildNodes();

        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("lance")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                MekHQ.logMessage("Unknown node type not loaded in Lance nodes: "
                                 + wn2.getNodeName());

                continue;
            }

            Lance l = Lance.generateInstanceFromXML(wn2);

            if (l != null) {
            	lances.put(l.getForceId(), l);
            }
        }
    }
}
