package code_assistant.completion;

import static code_assistant.util.Constants.DATA_FOLDER;
import static code_assistant.util.Constants.NL;
import static code_assistant.util.Constants.TAB_SIZE;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import code_assistant.tool.KeyHandler;
import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.Language;
import processing.app.syntax.JEditTextArea;
import processing.app.ui.Editor;
import processing.app.ui.EditorStatus;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

public class CodeTemplatesManager implements KeyHandler, CaretListener/*, Runnable*/ {
	static private CodeTemplate currentTemplate;
	static private int globalCaret;
	private Editor editor;

	static private Map<String, CodeTemplate> templates = new HashMap<>();
	static {
		templates.put("sout", new CodeTemplate("System.out.println($);"));
		templates.put("if", new CodeTemplate("if ($) {\n    $\n}"));
		templates.put("ifelse", new CodeTemplate("if ($) {\n    $\n} else {\n    \n}"));
		templates.put("switch", new CodeTemplate("switch ($) {\ncase $:\n    $\n    break;\n}"));
		templates.put("for", new CodeTemplate("for (int i = $; i < $; i++) {\n    $\n}"));
		templates.put("while", new CodeTemplate("while ($) {\n    $\n}"));
		templates.put("do", new CodeTemplate("do {\n    $\n} while ($);"));
		templates.put("try", new CodeTemplate("try {\n    $\n} catch (Exception e) {\n    e.printStackTrace();\n}"));
	}

	// CONSTRUCTOR
	public CodeTemplatesManager(Editor editor) {
		this.editor = editor;
		EditorUtil.init(editor);

		editor.getTextArea().addCaretListener(this);

		File jsonFile = new File(DATA_FOLDER, "templates.json");

		if (jsonFile.exists()) {
			addTemplatesFromFile(jsonFile, templates);

		} else {
			// create one
		}
//		textarea = editor.getTextArea();
//		gfx = textarea.getGraphics();

//		Thread thread = new Thread(this);
//		thread.setDaemon(true);
//		thread.start();
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (e.isShiftDown()) {

		}

		if (e.isControlDown() && keyCode == KeyEvent.VK_SPACE) {

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
			}

		} else if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_TAB) {
			// handleNextPosition();
			if (isReadingKeyboardInput()) {

				if (currentTemplate.isLastCandidate()) {
					currentTemplate = null;
					System.out.println("DONE");
					return false;
				}

				int caret = currentTemplate.getNextPosition();
				editor.setSelection(caret, caret);
				System.out.println("NEXT");

			} else {
				currentTemplate = null;
				System.out.println("isReading: false 1");
			}

		} else {

			if (isReadingKeyboardInput()) {

				if (currentTemplate.isLastCandidate()) {
					currentTemplate = null;
					System.out.println("DONE");
					return false;
				}

				currentTemplate.readInput(e, editor);

			} else {
				currentTemplate = null;
			}

			System.out.println("isReading: " + isReadingKeyboardInput() + " 2");
		}

//		if(isReadingKeyboardInput()) {
//			editor.statusMessage("EDITING PARAMETERS", EditorStatus.WARNING);			
//		} 

		return false;
	}

	static public boolean isReadingKeyboardInput() {
		if (currentTemplate == null)
			return false;

		return currentTemplate.contains(globalCaret);
	}

//	JEditTextArea textarea;
//	Graphics gfx;

	@Override
	public void caretUpdate(CaretEvent e) {
		globalCaret = e.getDot();
		// System.out.print(globalCaret + " - ");

	/*	int line = textarea.getCaretLine();
		int caretPos = editor.getCaretOffset() - textarea.getLineStartOffset(line);
		
		int x = 50;
		int y = textarea.lineToY(line);
		int w = textarea._offsetToX(line, caretPos) - x;
		int h = textarea.getPainter().getLineHeight();

		//textarea.getPainter().setBackground(new Color(120, 120, 0));
		//textarea.getPainter().invalidateLine(textarea.getCaretLine());
		
		gfx.setColor(new Color(255, 255, 0));
		//gfx.setClip(x, y, w, h);
		gfx.drawRect(x, y, w, h);
		//editor.getTextArea().paint(gfx); */
	}

//	private void handleNextPosition() {
//	
//	}

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
