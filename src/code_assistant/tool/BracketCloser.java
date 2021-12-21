package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.HashMap;

import processing.app.Preferences;
import processing.app.ui.Editor;

public class BracketCloser implements KeyHandler {
	private static final Map<Character, Character> tokens = new HashMap<Character, Character>();
	private final String CLOSING_BRACKETS = ")]}>";
	private char nextToken;
	private boolean enabled;

	protected Editor editor;

	static {
		tokens.put('(', ')');
		tokens.put('[', ']');
		tokens.put('{', '}');
		tokens.put('<', '>');

		tokens.put('"', '"');
		tokens.put('\'', '\'');
	}

	// CONSTRUCTOR
	public BracketCloser(Editor editor) {
		this.editor = editor;
		enabled = Preferences.getBoolean("code_assistant.bracket_closing.enabled");
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		char keyChar = e.getKeyChar();

		if (!tokens.containsKey(keyChar) && !tokens.containsValue(keyChar)) {
			// this keyChar is not our business, so let's get the hell outta here
			return false;
		}

		// closing - enabled
		if (keyChar == nextToken) {
			skipNextToken(keyChar);
			return false;
		}

		// closing - enabled or disabled
		if (isClosingBracket(keyChar)) {
			editor.insertText(String.valueOf(keyChar));
		}

		// opening - enabled
		if (isEnabled() && tokens.containsKey(keyChar)) {
			// if selection is active we must wrap a pair of tokens around the selection
			if (editor.isSelectionActive())
				wrapSelection(keyChar);

			else // otherwise, add a closing token
				addClosingToken(keyChar);
		}

		return false;
	}

	@Override // from the KeyHandler interface
	public boolean handleTyped(KeyEvent e) {
		char keyChar = e.getKeyChar();
		return (isClosingBracket(keyChar) || (isEnabled() && tokens.containsKey(keyChar)));
	}

	private void addClosingToken(char token) {
		nextToken = tokens.get(token);

		StringBuilder result = new StringBuilder();
		result.append(token).append(nextToken);

		editor.insertText(result.toString());

		// step back one char so that it is in the middle of the tokens
		int newCaret = editor.getCaretOffset() - 1;
		editor.setSelection(newCaret, newCaret);
	}

	private void skipNextToken(char token) {
		int caret = editor.getCaretOffset();

		if (editor.getText().charAt(caret) == nextToken) {
			editor.setSelection(caret + 1, caret + 1);
			nextToken = Character.UNASSIGNED;

		} else if (isClosingBracket(token)) { 
			editor.insertText(String.valueOf(token));
			
		} else { // if it's either \' \" or *
			addClosingToken(token);
		}
	}

	private void wrapSelection(char token) {
		StringBuilder selectedText = new StringBuilder(editor.getSelectedText());

		if (Preferences.getBoolean("code_assistant.bracket_closing.replace_token")) {
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

	protected boolean isClosingBracket(char ch) {
		return CLOSING_BRACKETS.contains(String.valueOf(ch));
	}
}
