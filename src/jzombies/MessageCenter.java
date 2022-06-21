/**
 * 
 */
package jzombies;

/**
 * @author Philipp Flügger 1361053, Patrick Mertes 1368734, Nhat Tran 1373869
 *
 */

import java.util.ArrayList;

public class MessageCenter {

	ArrayList<FIPA_Message> messageList;
	
	//Das MessageCenter dient als Plattform zum Nachrichtenaustausch.
	
	public MessageCenter(){
		this.messageList = new ArrayList<FIPA_Message>();
	}
	
	public void messageListToString() {
		for (FIPA_Message msg : messageList) {
			System.out.println(msg.toString());
		}
	}
	
	public void addMessage(FIPA_Message message){
		this.messageList.add(message);
	}
	
	//Prüft, ob noch eine Nachricht verfügbar ist.
	public boolean messagesAvailable(int receiver){
		for(FIPA_Message message: this.messageList){		
			if( message.getReceiver() == receiver ){
				return true;
			}
		}
		return false;
	}
	
	//Fragt genau eine Nachricht ab.
	public FIPA_Message getMessage(int receiver){
		for(FIPA_Message message: this.messageList){
			if( message.getReceiver() == receiver ){
				this.messageList.remove(message);
				return message;
			}
		}
		return null;
	}
	
	public void send(int sender, int receiver, int subject, FIPA_Performative performative, String content){
		this.addMessage( new FIPA_Message(sender, receiver, subject, performative, content) );
	}
}