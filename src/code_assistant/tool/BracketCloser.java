package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.HashMap;

import processing.app.Preferences;
import processing.app.ui.Editor;

public class BracketCloser implements KeyHandler {
	private Editor editor;

	// needed to remove double brackets (when typing too fast)
	static char lastChar;

	// define which characters should get closed
	static char[] openingChar = { '(', '[', '{', '"', '\'', '<' };
	static char[] closingChar = { ')', ']', '}', '"', '\'', '>' };

	// TODO: talvez deveria torna-los <String, String> de forma conseguir utilizar
	// padroes mais avancados como /* e */ ???
	static private final Map<Character, Character> tokens = new HashMap<Character, Character>();

	private boolean enabled;

	static {
		tokens.put('(', ')');
		tokens.put('[', ']');
		tokens.put('{', '}');
		tokens.put('<', '>');

		tokens.put('"', '"');
		tokens.put('\'', '\'');
		tokens.put('*', '*');
	}

	// CONSTRUCTOR
	public BracketCloser(Editor editor) {
		this.editor = editor;
		enabled = Preferences.getBoolean("code_assistant.bracket_closing.auto_close");
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		char keyChar = e.getKeyChar();

		if (!isEnabled() || !tokens.containsKey(keyChar)) {
			return false;
		}

		// if selection is active we must wrap a pair of tokens around the selection
		if (editor.isSelectionActive()) {
			wrapSelection(keyChar);
			
		} else {
			addClosingToken(keyChar);
		}

		return true;
	}

	@Override // from the KeyHandler interface
	public boolean handleTyped(KeyEvent e) {
		char keyChar = e.getKeyChar();
		return (isEnabled() && tokens.containsKey(keyChar) && tokens.containsValue(keyChar));
	}

	private void wrapSelection(char token) {
		StringBuilder selectedText = new StringBuilder(editor.getSelectedText());

		if (Preferences.getBoolean("code_assistant.bracket_closing.expand")) {
			selectedText.insert(0, token).append(tokens.get(token)).toString();

		} else {
			char firstChar = selectedText.charAt(0);
			char lastChar = selectedText.charAt(selectedText.length() - 1);

			boolean isAlreadyWrapped = false;

			if (tokens.containsKey(firstChar) && tokens.containsValue(lastChar) && lastChar == tokens.get(firstChar))
				isAlreadyWrapped = true;

			if (isAlreadyWrapped) {
				// if the selected text is already wrapped with this token, then toggle it off
				if (token == firstChar) {
					selectedText.delete(0, 1);
					selectedText.delete(selectedText.length() - 1, selectedText.length());
				} else {
					selectedText.setCharAt(0, token);
					selectedText.setCharAt(selectedText.length() - 1, tokens.get(token));
				}

			} else {
				selectedText.insert(0, token).append(tokens.get(token)).toString();
			}
		}

		int start = editor.getSelectionStart();
		int end = start + selectedText.length();

		editor.startCompoundEdit();
		editor.setSelectedText(selectedText.toString());
		editor.setSelection(start, end);
		editor.stopCompoundEdit();
	}
	
	private void addClosingToken(char token) { 
		int line = editor.getTextArea().getCaretLine();
		String lineText = editor.getLineText(line);
		
		int caretPos = editor.getCaretOffset() - editor.getLineStartOffset(line);
		
		char prevChar = lineText.charAt(caretPos-1);
		char nextChar = lineText.charAt(caretPos);
		
		println("prevChar: " + prevChar + " - nextChar: " + nextChar);
		
		StringBuilder result = new StringBuilder();
		result.append(token).append(tokens.get(token));
		
		editor.insertText(result.toString());

		int newCaret = editor.getCaretOffset() - 1;
		editor.setSelection(newCaret, newCaret);
	}


//	@Override
//	public boolean handlePressed(KeyEvent e) {
//		int keyChar = e.getKeyChar();
//		
//		// loop through array of opening brackets to trigger completion
//		for (int i = 0; i < openingChar.length; i++) {
//			// if nothing is selected just add closing bracket, else wrap brackets around
//			// selection
//			if (!editor.isSelectionActive()) {
//				if (keyChar == openingChar[i])
//					addClosingChar(i);
//				else if (keyChar == closingChar[i] && lastChar == openingChar[i])
//					removeClosingChar(i);
//			} else if (keyChar == openingChar[i] && editor.isSelectionActive())
//				addClosingChar(i, editor.getSelectionStart(), editor.getSelectionStop());
//		}
//		return false;
//	}

	// add closing bracket and set caret inside brackets
	private void addClosingChar(int positionOfChar) {
		editor.insertText(Character.toString(closingChar[positionOfChar]));

		int cursorPos = editor.getCaretOffset();
		editor.setSelection(cursorPos - 1, cursorPos - 1);
		lastChar = openingChar[positionOfChar];
	}

	// prevents something like ()) when typing too fast
	// TODO: corrigir bug quando este metodo e chamado para apagar um 'closing char'
	// e acaba movendo o scroll do editor.

	private void removeClosingChar(int positionOfChar) {
		// return if character is ' or "
		if (closingChar[positionOfChar] == '\'' || closingChar[positionOfChar] == '"')
			return;

		String sketchContent = editor.getText();
		int cursorPos = editor.getCaretOffset();

		String newContent1 = sketchContent.substring(0, cursorPos);
		String newContent2 = sketchContent.substring(cursorPos + 1, sketchContent.length());

		editor.setText(newContent1 + newContent2);
		editor.setSelection(cursorPos, cursorPos);

		lastChar = closingChar[positionOfChar];
	}

	protected boolean isEnabled() {
		return enabled;
	}

	private void println(Object... what) {
		for (Object s : what) {
			System.out.println(s.toString());
		}
	}
}
