package code_assistant.tool;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;

public interface RegistrableActions {
	HashMap<String, AbstractAction> actions = new HashMap<>();

	static Map.Entry<String, AbstractAction> getAction(String actionName) {
		if (actions.containsKey(actionName)) {
			return new AbstractMap.SimpleEntry<String, AbstractAction>(actionName, actions.get(actionName));
		}
		throw new IllegalArgumentException("There is no \"" + actionName + "\" action in this class");
	}
}