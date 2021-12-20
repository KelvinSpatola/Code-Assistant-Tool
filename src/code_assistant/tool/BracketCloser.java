package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.HashMap;

import processing.app.Preferences;
import processing.app.ui.Editor;

public class BracketCloser implements KeyHandler {
	private static final Map<Character, Character> tokens = new HashMap<Character, Character>();
	private char previousToken;
	private boolean enabled;

	protected Editor editor;

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
				
		if (keyChar == previousToken) {
			int newCaret = editor.getCaretOffset() + 1;

			editor.setSelection(newCaret, newCaret);
			previousToken = Character.UNASSIGNED;
			return true;
			
		} else if (keyChar == ')' || keyChar == ']' || keyChar == '}' || keyChar == '>') {
			
			editor.insertText(String.valueOf(keyChar));
		}

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
		return (isEnabled() && (tokens.containsKey(keyChar) || keyChar == ')' || keyChar == ']' || keyChar == '}' || keyChar == '>'));
	}
	
	private void addClosingToken(char token) { 
		StringBuilder result = new StringBuilder();
		result.append(token).append(tokens.get(token));
		
		editor.insertText(result.toString());

		int newCaret = editor.getCaretOffset() - 1;
		editor.setSelection(newCaret, newCaret);
		
		previousToken = tokens.get(token);
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

	protected boolean isEnabled() {
		return enabled;
	}

	private void println(Object... what) {
		for (Object s : what) {
			System.out.println(s.toString());
		}
	}
}
