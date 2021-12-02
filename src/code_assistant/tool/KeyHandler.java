package code_assistant.tool;

import java.awt.event.KeyEvent;

import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;

public interface KeyHandler {
	static Map<String, AbstractAction> actions = new HashMap<>();
	
	boolean handlePressed(KeyEvent e);
	
	static Map<String, AbstractAction> getActions() {
		return actions;
	}
}
