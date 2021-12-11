package code_assistant.util;

import processing.app.Preferences;

public final class ToolPreferences {

	private ToolPreferences() {}

	static public void init() {
		// set default attributes
		set("code_assistant.auto_format.strings", "true");
		set("code_assistant.auto_format.comments", "true");
		set("code_assistant.auto_format.line_length", "80");
	}

	static public void set(String attribute, String value) {
		if (Preferences.get(attribute) == null) {
			Preferences.set(attribute, value);
		}
	}
}