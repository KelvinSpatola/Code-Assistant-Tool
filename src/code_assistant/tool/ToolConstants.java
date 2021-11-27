package code_assistant.tool;

//import java.awt.event.InputEvent;

import processing.app.Preferences;

public interface ToolConstants {
//	int CTRL = InputEvent.CTRL_MASK;
//	int ALT = InputEvent.ALT_MASK;
//	int SHIFT = InputEvent.SHIFT_MASK;
//
//	int CTRL_ALT = CTRL | ALT;
//	int CTRL_SHIFT = CTRL | SHIFT;
//	int ALT_SHIFT = ALT | SHIFT;
//	int ALT_GRAPH = CTRL | ALT | InputEvent.ALT_GRAPH_MASK;
//	String ALT_GRAPH = "control alt altGraph BRACELEFT";

	int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
	String TAB = ToolUtilities.addSpaces(TAB_SIZE);
	String NL = "\n";

	String COMMENT_TEXT = "^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)";
	String STRING_TEXT = "^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}";
	String SPLIT_STRING_TEXT = "^\\h*\\+\\s*(?:\\\".*){2}";
}