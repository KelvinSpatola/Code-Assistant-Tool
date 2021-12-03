package code_assistant.util;

//import java.awt.event.InputEvent;

import processing.app.Preferences;

public interface ToolConstants {
	
	int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
	String TAB = EditorUtil.addSpaces(TAB_SIZE);
	String NL = "\n";

	String COMMENT_TEXT = "^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)";
	String STRING_TEXT = "^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}";
	String SPLIT_STRING_TEXT = "^\\h*\\+\\s*(?:\\\".*){2}";
	
	String BLOCK_OPENING = "^(?!.*?\\/+.*?\\{.*|.*\\/\\*.*|\\h*\\*.*).*?\\{.*";
	String BLOCK_CLOSING = "^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*";
}