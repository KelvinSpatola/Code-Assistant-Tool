package code_assistant.util;

import processing.app.Preferences;

public final class ToolPreferences {

	private ToolPreferences() {}

	static public void init() {
		// set default attributes
		set("code_assistant.autoformat.strings", "true");
		set("code_assistant.autoformat.comments", "true");
		set("code_assistant.autoformat.line_length", "80");

		set("code_assistant.move_lines.auto_indent", "true");
	}

	static public void set(String attribute, String value) {
		if (Preferences.get(attribute) == null) {
			Preferences.set(attribute, value);
		}
	}
}