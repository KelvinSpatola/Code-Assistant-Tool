package code_assistant.completion;

import static code_assistant.util.Constants.DATA_FOLDER;
import static code_assistant.util.Constants.NL;
import static code_assistant.util.Constants.TAB_SIZE;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import code_assistant.tool.KeyHandler;
import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.ui.Editor;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

public class CodeTemplatesManager implements KeyHandler {
	static private boolean isReadingKeyboardInput;
	private CodeTemplate currentTemplate;
	private Editor editor;

	static private Map<String, CodeTemplate> templates = new HashMap<>();
	static {
		templates.put("sout", new CodeTemplate("System.out.println($);"));
		templates.put("if", new CodeTemplate("if ($) {\n    $\n}"));
		templates.put("ifelse", new CodeTemplate("if ($) {\n    $\n} else {\n    $\n}"));
		templates.put("switch", new CodeTemplate("switch ($) {\ncase $:\n    $\n    break;\n}"));
		templates.put("for", new CodeTemplate("for (int i = 0; i < $; i++) {\n    $\n}"));
		templates.put("while", new CodeTemplate("while ($) {\n    $\n}"));
		templates.put("do", new CodeTemplate("do {\n    $\n} while ($);"));
		templates.put("try", new CodeTemplate("try {\n    $\n} catch (Exception e) {\n    e.printStackTrace();\n}"));
	}

	// CONSTRUCTOR
	public CodeTemplatesManager(Editor editor) {
		this.editor = editor;
		EditorUtil.init(editor);

		File jsonFile = new File(DATA_FOLDER, "templates.json");

		if (jsonFile.exists()) {
			addTemplatesFromFile(jsonFile, templates);

		} else {
			// create one
		}

	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) {

			String trigger = checkTrigger();

			if (templates.containsKey(trigger)) {
				int triggerEnd = editor.getCaretOffset();
				int triggerStart = triggerEnd - trigger.length();

				Selection s = new Selection(editor);

				editor.setSelection(s.getStart(), triggerEnd);

				int line = editor.getTextArea().getCaretLine();
				int indent = EditorUtil.getLineIndentation(line);

				currentTemplate = templates.get(trigger);
				String code = currentTemplate.getCode(indent);

				editor.setSelectedText(code);

				int caret = currentTemplate.getStartCaretPosition(editor.getCaretOffset());
				editor.setSelection(caret, caret);

				currentTemplate.isReadingInput(true);
				isReadingKeyboardInput = true;
			}

		} else if (e.getKeyCode() == KeyEvent.VK_TAB) {
			handleNextPosition();
		}
		
		return false;
	}
	
	private void handleNextPosition() {
		if (CodeTemplatesManager.isReadingKeyboardInput() && currentTemplate.isReadingInput()) {
			int caret = currentTemplate.getNextPosition();
			editor.setSelection(caret, caret);

		} else {
			currentTemplate = null;
			isReadingKeyboardInput = false;
		}
	}

	@Override
	public boolean handleTyped(KeyEvent e) {
		return false;
	}

	private String checkTrigger() {
		int line = editor.getTextArea().getCaretLine();
		String lineText = editor.getLineText(line);

		StringBuilder sb = new StringBuilder();
		int index = EditorUtil.caretPositionInsideLine() - 1;

		while (index >= 0) {
			char ch = lineText.charAt(index);

			if (Character.isWhitespace(ch)) {
				break;
			}
			sb.append(ch);
			index--;
		}
		return sb.reverse().toString();
	}

	static public boolean isReadingKeyboardInput() {
		return isReadingKeyboardInput;
	}	

	private void addTemplatesFromFile(File file, Map<String, CodeTemplate> templates) {
		JSONObject jsonFile = PApplet.loadJSONObject(file);
		JSONArray user_templates = jsonFile.getJSONArray("User-Templates");

		for (int i = 0; i < user_templates.size(); i++) {
			JSONObject template = user_templates.getJSONObject(i);
			JSONArray lines = template.getJSONArray("code");

			StringBuilder source = new StringBuilder();
			for (int j = 0; j < lines.size(); j++) {
				source.append(lines.getString(j)).append(NL);
			}
			source.deleteCharAt(source.length() - 1);

			String key = template.getString("key");
			templates.put(key, new CodeTemplate(source.toString()));
		}
	}

	static private void println(Object... what) {
		for (Object s : what) {
			System.out.println(s.toString());
		}
	}

}
