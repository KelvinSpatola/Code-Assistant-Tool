package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import processing.app.Preferences;
import processing.app.ui.Editor;

public class CodeCompletion implements KeyHandler {
	protected Editor editor;

	static Map<String, Macros> snippets = new HashMap<>();

	static {
		snippets.put("sout", new Macros("System.out.println();", 2));
		snippets.put("setup", new Macros("void setup() {\n\t\n}", 2));
		snippets.put("draw", new Macros("void draw() {\n\t\n}", 2));
		snippets.put("mpress", new Macros("void mousePressed() {\n\t\n}", 2));
		snippets.put("kpress", new Macros("void keyPressed() {\n\t\n}", 2));
		snippets.put("fori", new Macros("for (int i = 0; i < 10; i++) {\n\t\n}", 2));
		snippets.put("switch", new Macros("switch () {\ncase 1:\n\t\n\tbreak;\n}", 10));
	}

	static class Macros {
		String code;

		public String getCode() {
			return code;
		}

		public int getCaretPos() {
			return caretPos;
		}

		int caretPos;

		Macros(String code, int caretPos) {
			this.code = code;
			this.caretPos = caretPos;
		}

	}

	public CodeCompletion(Editor editor) {
		this.editor = editor;
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		int key = e.getKeyCode();

		if (e.isControlDown() && key == KeyEvent.VK_SPACE) {

			if (Preferences.getBoolean("editor.caret.block") == false) {
				Preferences.setBoolean("editor.caret.block", true);
				
				Method createTextArea;
				try {
					createTextArea = editor.getClass().getDeclaredMethod("createTextArea");
					createTextArea.setAccessible(true);
					editor = (Editor) createTextArea.invoke(editor);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			System.out.println(Preferences.getBoolean("editor.caret.block"));

			String trigger = checkTrigger();

			if (snippets.containsKey(trigger)) {

				int triggerEnd = editor.getCaretOffset();
				int triggerStart = triggerEnd - trigger.length();
				editor.setSelection(triggerStart, triggerEnd);

				editor.setSelectedText(snippets.get(trigger).getCode());
				int caret = editor.getCaretOffset() - snippets.get(trigger).getCaretPos();
				editor.setSelection(caret, caret);
			}
		}
		return false;
	}

	@Override
	public boolean handleTyped(KeyEvent e) {
		return false;
	}

	private String checkTrigger() {
		int line = editor.getTextArea().getCaretLine();
		String lineText = editor.getLineText(line);

		StringBuilder sb = new StringBuilder();
		int index = getPositionInsideLine() - 1;

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

	private int getPositionInsideLine() {
		int caretOffset = editor.getCaretOffset();
		int lineStartOffset = editor.getLineStartOffset(editor.getTextArea().getCaretLine());
		return caretOffset - lineStartOffset;
	}

	private void println(Object... what) {
		for (Object s : what) {
			System.out.println(s.toString());
		}
	}

}
