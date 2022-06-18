/**
 * 
 */
package jzombies;


/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */

public enum FIPA_Performative {
	
	//Bei "enum" handelt es sich um einen Datentyp, 
	//mit dem Konstanten definiert werden können.
	
	//Diese können anschließend mit "FIPA_Performative.NAME" aufgerufen werden.
	
	//Sie die können bei Bedarf die fehlenden Performatives ergänzen.
	
	INFORM, REQUEST, CFP, PROPOSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL, INFORM_DONE, INFORM_RESULT, FAILURE
}