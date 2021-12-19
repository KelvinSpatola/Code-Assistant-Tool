package code_assistant.util;

import code_assistant.tool.CodeAssistant;
import processing.app.Preferences;

public final class ToolPreferences {
	private static final String preffix = format(CodeAssistant.TOOL_NAME).concat(".");
	
	private ToolPreferences() {
	}

	static public void init() {
		// set default attributes
		set("autoformat.strings", "true");
		set("autoformat.comments", "true");
		set("autoformat.line_length", "80");
		set("move_lines.auto_indent", "true");
		set("bracket_closing.auto_close", "true");
		set("bracket_closing.expand", "false");
	}

	static public void set(String attribute, String value) {		
		String extendedAttribute = preffix + format(attribute);
				
		if (Preferences.get(extendedAttribute) == null) {
			Preferences.set(extendedAttribute, value);
		}
	}
	
	static private String format(String str) {
		return str.toLowerCase().replace(' ', '_');
	}
}