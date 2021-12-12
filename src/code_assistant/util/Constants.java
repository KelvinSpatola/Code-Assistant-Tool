package code_assistant.util;

import processing.app.Preferences;

public final class Constants {
	public static final String WEBSITE = "https://github.com/KelvinSpatola/Code-Assistant-Tool";
	
	public static final int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
	public static final String TAB = EditorUtil.addSpaces(TAB_SIZE);
	public static final String NL = "\n";

	public static final String BLOCK_OPENING = "^(?!.*?\\/+.*?\\{.*|.*\\/\\*.*|\\h*\\*.*).*?\\{.*";
	public static final String BLOCK_CLOSING = "^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*";

	private Constants() {
	} 
}