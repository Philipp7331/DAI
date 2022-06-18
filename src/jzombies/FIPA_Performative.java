/**
 * 
 */
package jzombies;


/**
 * @author Philipp Fl�gger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */

public enum FIPA_Performative {
	
	//Bei "enum" handelt es sich um einen Datentyp, 
	//mit dem Konstanten definiert werden k�nnen.
	
	//Diese k�nnen anschlie�end mit "FIPA_Performative.NAME" aufgerufen werden.
	
	//Sie die k�nnen bei Bedarf die fehlenden Performatives erg�nzen.
	
	INFORM, REQUEST, CFP, PROPOSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL, INFORM_DONE, INFORM_RESULT, FAILURE
}