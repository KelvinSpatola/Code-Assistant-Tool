package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import processing.app.Platform;
import processing.app.Preferences;
import processing.app.syntax.PdeInputHandler;
import processing.app.ui.Editor;

public class ToolInputHandler extends PdeInputHandler implements ToolConstants {

	// CONSTRUCTOR
	public ToolInputHandler(Editor editor) {
		// this.editor = editor;
		// editor.getTextArea().addKeyListener(this);
		super(editor);

		ToolTextArea.init(editor);
		addKeyBinding("C+E", ToolTextArea.getAction("delete-line").getValue());
//		addKeyBinding("CS+E", ToolTextArea.getAction("delete-line-content").getValue());
		addKeyBinding("AS+UP", ToolTextArea.getAction("duplicate-lines-up").getValue());
		addKeyBinding("AS+DOWN", ToolTextArea.getAction("duplicate-lines-down").getValue());
		addKeyBinding("A+UP", ToolTextArea.getAction("move-lines-up").getValue());
		addKeyBinding("A+DOWN", ToolTextArea.getAction("move-lines-down").getValue());

//		addAction(ToolTextArea.getAction("delete-line"), "control E");
		addAction(ToolTextArea.getAction("delete-line-content"), "control shift E");
//		addAction(ToolTextArea.getAction("duplicate-lines-up"), "alt shift UP");
//		addAction(ToolTextArea.getAction("duplicate-lines-down"), "alt shift DOWN");
//		addAction(ToolTextArea.getAction("move-lines-up"), "alt UP");
//		addAction(ToolTextArea.getAction("move-lines-down"), "alt DOWN");

		ToolEditor.init(editor);
		addKeyBinding("ENTER", ToolEditor.getAction("handle-enter").getValue());
		addKeyBinding("A+ENTER", ToolEditor.getAction("insert-new-line-bellow-current-line").getValue());
		addKeyBinding("CA+RIGHT", ToolEditor.getAction("select-block").getValue());
//		addKeyBinding("C+T", ToolEditor.getAction("format-selected-text").getValue());

//		addAction(ToolEditor.getAction("handle-enter"), "ENTER");
//		addAction(ToolEditor.getAction("insert-new-line-bellow-current-line"), "alt ENTER");
//		addAction(ToolEditor.getAction("select-block"), "control alt RIGHT");
		addAction(ToolEditor.getAction("format-selected-text"), "control T");

		BracketCloser.init(editor);

//		for (KeyStroke ks : editor.getTextArea().getInputMap().allKeys()) {
//			System.out.println(ks + " " + editor.getTextArea().getInputMap().get(ks));
//		}

	}

	private void addAction(Map.Entry<String, AbstractAction> actionEntry, String keyStroke) {
		KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
		editor.getTextArea().getInputMap().put(ks, actionEntry.getKey());
		editor.getTextArea().getActionMap().put(actionEntry.getKey(), actionEntry.getValue());
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
		int keyCode = e.getKeyCode();
		char keyChar = e.getKeyChar();

		if (e.isMetaDown())
			return false;

		// VK_BACK_SPACE -> 8 | VK_TAB -> 9 | VK_ENTER -> 10
		// [32 - 127] = //
		// !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~
//		if ((keyCode == KeyEvent.VK_BACK_SPACE) || (keyCode == KeyEvent.VK_TAB) || (keyCode == KeyEvent.VK_ENTER)
//				|| ((keyChar >= 32) && (keyChar < 128))) {
//
//			// BracketCloser.update(e.getKeyChar());
//			handleInputMethodCommit();
//		}

		// VK_TAB -> 9
		if (keyCode == KeyEvent.VK_TAB) {
			handleTabulation(e.isShiftDown());
			e.consume();

			// VK_ENTER -> 10 | Return key (on Mac OS) -> 13
		} else if (keyCode == KeyEvent.VK_ENTER || keyCode == 13) {
			// handleNewLine();
			e.consume();

		} else if (keyChar == '}') {
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
				e.consume();
				return true;
			}
		}
		return false;
	}

	private void handleTabulation(boolean isShiftDown) {
		if (isShiftDown) {
			editor.handleOutdent();

		} else if (editor.isSelectionActive()) {
			editor.handleIndent();

		} else if (Preferences.getBoolean("editor.tabs.expand")) {
			// "editor.tabs.expand" means that each tab is made up of a
			// stipulated number of spaces, and not just a single solid \t
			editor.setSelectedText(TAB);

		} else {
			editor.setSelectedText("\t");
		}
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

	public static void println(Object... objects) {
		for (Object o : objects) {
			System.out.println(o.toString());
		}
	}
}