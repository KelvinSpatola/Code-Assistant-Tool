package code_assistant.tool;

import java.util.Map;

import javax.swing.Action;

public interface ActionTrigger {
    Map<String, Action> getActions();
}
