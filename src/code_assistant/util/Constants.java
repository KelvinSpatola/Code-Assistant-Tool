package code_assistant.util;

import processing.app.Preferences;

public final class Constants {
	public static final int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
	public static final String TAB = EditorUtil.addSpaces(TAB_SIZE);
	public static final String NL = "\n";

	
	private Constants() {
	}
}