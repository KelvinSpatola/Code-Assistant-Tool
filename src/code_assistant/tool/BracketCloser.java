package code_assistant.tool;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.AbstractAction;

import processing.app.ui.Editor;

public class BracketCloser implements RegistrableActions {
	private static Editor editor;

	// needed to remove double brackets (when typing too fast)
	static char lastChar;

	// define which characters should get closed
	static char[] openingChar = { '(', '[', '{', '"', '\'', '<' };
	static char[] closingChar = { ')', ']', '}', '"', '\'', '>' };

	public static void init(Editor _editor) {
		editor = _editor;

		actions.put("insert-closing-brace", HANDLE_ENTER);
	}

	public static Map.Entry<String, AbstractAction> getAction(String actionName) {
		return RegistrableActions.getAction(actionName);
	}

	public static final AbstractAction HANDLE_ENTER = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			editor.insertText(Character.toString('}'));
		}
	};

	public static void update(char key) {
		// loop through array of opening brackets to trigger completion
		for (int i = 0; i < openingChar.length; i++) {
			// if nothing is selected just add closing bracket, else wrap brackets around
			// selection
			if (!editor.isSelectionActive()) {
				if (key == closingChar[i] && lastChar == openingChar[i])
					removeClosingChar(i);
				else if (key == openingChar[i])
					addClosingChar(i);
			} else if (key == openingChar[i] && editor.isSelectionActive())
				addClosingChar(i, editor.getSelectionStart(), editor.getSelectionStop());
		}
		System.out.println(lastChar);
	}

	// add closing bracket and set caret inside brackets
	private static void addClosingChar(int positionOfChar) {
		editor.insertText(Character.toString(closingChar[positionOfChar]));

		int cursorPos = editor.getCaretOffset();
		editor.setSelection(cursorPos - 1, cursorPos - 1);
		lastChar = openingChar[positionOfChar];
	}

	// if something is selected wrap closing brackets around selection
	private static void addClosingChar(int positionOfChar, int startSelection, int endSelection) {
		editor.setSelection(endSelection, endSelection);
		editor.insertText(Character.toString(closingChar[positionOfChar]));
		editor.setSelection(startSelection, startSelection);
		lastChar = openingChar[positionOfChar];
	}

	// prevents something like ()) when typing too fast
	// TODO: corrigir bug quando este metodo e chamado para apagar um 'closing char'
	// e acaba movendo o scroll do editor.

	public static void removeClosingChar(int positionOfChar) {
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
