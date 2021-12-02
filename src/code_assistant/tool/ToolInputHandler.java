package code_assistant.tool;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import processing.app.Platform;
import processing.app.Preferences;
import processing.app.syntax.PdeInputHandler;
import processing.app.ui.Editor;

public class ToolInputHandler extends PdeInputHandler implements ToolConstants {

	// CONSTRUCTOR
	public ToolInputHandler(Editor editor) {
		super(editor);

		ToolTextArea.init(editor);
		ToolEditor.init(editor);
		BracketCloser.init(editor);

		addKeyBinding("C+E", ToolTextArea.DELETE_LINE);
		addKeyBinding("AS+UP", ToolTextArea.DUPLICATE_UP);
		addKeyBinding("AS+DOWN", ToolTextArea.DUPLICATE_DOWN);
		addKeyBinding("A+UP", ToolTextArea.MOVE_UP);
		addKeyBinding("A+DOWN", ToolTextArea.MOVE_DOWN);
		addKeyBinding("CS+E", "delete-line-content", ToolTextArea.DELETE_LINE_CONTENT);

		addKeyBinding("ENTER", ToolEditor.HANDLE_ENTER);
		addKeyBinding("TAB", ToolEditor.INDENT_TEXT);
		addKeyBinding("S+TAB", ToolEditor.OUTDENT_TEXT);
		addKeyBinding("A+ENTER", ToolEditor.INSERT_NEW_LINE_BELLOW_CURRENT_LINE);
		addKeyBinding("CA+RIGHT", ToolEditor.SELECT_BLOCK);
		addKeyBinding("C+T", "format-selected-text", ToolEditor.FORMAT_SELECTED_TEXT);
		
	}

	public void addKeyBinding(String keyBinding, String actionName, AbstractAction action) {
		KeyStroke ks = parseKeyStroke(keyBinding);
		editor.getTextArea().getInputMap().put(ks, actionName);
		editor.getTextArea().getActionMap().put(actionName, action);
	}

	@Override
	protected boolean isMnemonic(KeyEvent e) {
		if (!Platform.isMacOS()) {
			if (e.isAltDown() && Character.isLetter(e.getKeyChar())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		if (e.isMetaDown())
			return false;

		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		// VK_ENTER -> 10 | Return key (on Mac OS) -> 13
//			else if (keyCode == KeyEvent.VK_ENTER || keyCode == 13) {
//			// handleNewLine();
//			e.consume();
//
//		} else 

		if (keyChar == '}') {
			return handleCloseBrace();
		}
		return false;
	}

	@Override
	public boolean handleTyped(KeyEvent e) {
		char keyChar = e.getKeyChar();

		if (e.isControlDown()) {
			// on linux, ctrl-comma (prefs) being passed through to the editor
			if (keyChar == KeyEvent.VK_COMMA) {
				e.consume();
				return true;
			}
			if (keyChar == KeyEvent.VK_SPACE) {
				e.consume();
				return true;
			}
		}

		BracketCloser.update(e.getKeyChar());
		handleInputMethodCommit();
		e.consume();

		return false;
	}

	private boolean handleCloseBrace() {
		char[] contents = editor.getText().toCharArray();

		if (Preferences.getBoolean("editor.indent")) {
			if (editor.isSelectionActive())
				editor.setSelectedText("");

			// if this brace is the only thing on the line, outdent
			// index to the character to the left of the caret
			int prevCharIndex = editor.getCaretOffset() - 1;

			// backup from the current caret position to the last newline,
			// checking for anything besides whitespace along the way.
			// if there's something besides whitespace, exit without
			// messing any sort of indenting.
			int index = prevCharIndex;
			boolean finished = false;
			while ((index != -1) && (!finished)) {
				if (contents[index] == 10) {
					finished = true;
					index++;
				} else if (contents[index] != ' ') {
					// don't do anything, this line has other stuff on it
					return false;
				} else {
					index--;
				}
			}
			if (!finished)
				return false; // brace with no start
			int lineStartIndex = index;

			int pairedSpaceCount = EditorUtil.calcBraceIndent(prevCharIndex, contents); // , 1);
			if (pairedSpaceCount == -1)
				return false;

			editor.getTextArea().setSelectionStart(lineStartIndex);
			editor.setSelectedText(EditorUtil.addSpaces(pairedSpaceCount));

			// mark this event as already handled
			// e.consume();
			return true;
		}
		return false;
	}

	private static void println(Object... objects) {
		for (Object o : objects) {
			System.out.println(o.toString());
		}
	}
}