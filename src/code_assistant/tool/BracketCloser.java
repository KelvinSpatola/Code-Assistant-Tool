package code_assistant.tool;

import java.awt.event.KeyEvent;

import processing.app.ui.Editor;

public class BracketCloser implements KeyPressedListener {
	private Editor editor;

	// needed to remove double brackets (when typing too fast)
	static char lastChar;

	// define which characters should get closed
	static char[] openingChar = { '(', '[', '{', '"', '\'', '<' };
	static char[] closingChar = { ')', ']', '}', '"', '\'', '>' };

	public BracketCloser(Editor _editor) {
		editor = _editor;
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		int keyChar = e.getKeyChar();

		// loop through array of opening brackets to trigger completion
		for (int i = 0; i < openingChar.length; i++) {
			// if nothing is selected just add closing bracket, else wrap brackets around
			// selection
			if (!editor.isSelectionActive()) {
				if (keyChar == openingChar[i])
					addClosingChar(i);
				else if (keyChar == closingChar[i] && lastChar == openingChar[i])
					removeClosingChar(i);
			} else if (keyChar == openingChar[i] && editor.isSelectionActive())
				addClosingChar(i, editor.getSelectionStart(), editor.getSelectionStop());
		}
		return false;
	}


	// add closing bracket and set caret inside brackets
	private void addClosingChar(int positionOfChar) {
		editor.insertText(Character.toString(closingChar[positionOfChar]));

		int cursorPos = editor.getCaretOffset();
		editor.setSelection(cursorPos - 1, cursorPos - 1);
		lastChar = openingChar[positionOfChar];
	}

	// if something is selected wrap closing brackets around selection
	private void addClosingChar(int positionOfChar, int startSelection, int endSelection) {
		editor.setSelection(endSelection, endSelection);
		editor.insertText(Character.toString(closingChar[positionOfChar]));
		editor.setSelection(startSelection, startSelection);
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
}
