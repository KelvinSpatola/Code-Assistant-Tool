package code_assistant.util;

import java.io.File;

import processing.app.Base;
import processing.app.Preferences;

public final class Constants {
	public static final File TOOL_FOLDER = new File(Base.getSketchbookToolsFolder(), "CodeAssistant");
	public static final File DATA_FOLDER = new File(TOOL_FOLDER, "data");

	public static final String WEBSITE = "https://github.com/KelvinSpatola/Code-Assistant-Tool";

	public static final int TAB_SIZE = Preferences.getInteger("editor.tabs.size");
	public static final String TAB = EditorUtil.addSpaces(TAB_SIZE);
	public static final String NL = "\n";

	public static final String BLOCK_OPENING = "^(?!.*?\\/+.*?\\{.*|\\h*\\*.*|.*?\\\".*?\\{.*?\\\".*).*?\\{.*$";
	public static final String BLOCK_CLOSING = "^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*";

	private Constants() {
	}
}