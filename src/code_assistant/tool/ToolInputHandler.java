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
//		addKeyBinding("C+E", ToolTextArea.getAction("delete-line").getValue());
//		addKeyBinding("AS+UP", ToolTextArea.getAction("duplicate-lines-up").getValue());
//		addKeyBinding("AS+DOWN", ToolTextArea.getAction("duplicate-lines-down").getValue());
//		addKeyBinding("A+UP", ToolTextArea.getAction("move-lines-up").getValue());
//		addKeyBinding("A+DOWN", ToolTextArea.getAction("move-lines-down").getValue());
		addAction(ToolTextArea.getAction("delete-line"), "control E");
		addAction(ToolTextArea.getAction("delete-line-content"), "control shift E");
		addAction(ToolTextArea.getAction("duplicate-lines-up"), "alt shift UP");
		addAction(ToolTextArea.getAction("duplicate-lines-down"), "alt shift DOWN");
		addAction(ToolTextArea.getAction("move-lines-up"), "alt UP");
		addAction(ToolTextArea.getAction("move-lines-down"), "alt DOWN");

		ToolEditor.init(editor);
		// addKeyBinding("CA+ENTER", ToolEditor.getAction("handle-enter").getValue());
//		addKeyBinding("CA+RIGHT", ToolEditor.getAction("select-block").getValue());
		// addKeyBinding("C+T",
		// ToolEditor.getAction("format-selected-text").getValue());
		addAction(ToolEditor.getAction("handle-enter"), "alt ENTER");
		addAction(ToolEditor.getAction("select-block"), "control alt RIGHT");
		addAction(ToolEditor.getAction("format-selected-text"), "control T");

		// BracketCloser.init(editor);
		// registerAction(BracketCloser.getAction("insert-closing-brace"), "ENTER");

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
	protected boolean isMnemonic(KeyEvent event) {
		if (!Platform.isMacOS()) {
			if (event.isAltDown() && !event.isControlDown() && event.getKeyChar() != KeyEvent.VK_UNDEFINED) {
				// This is probably a menu mnemonic, don't pass it through.
				// If it's an alt-NNNN sequence, those only work on the keypad
				// and pass through UNDEFINED as the keyChar.
//		    	  return true;

				// System.out.println("MNEMONIC");
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
			handleNewLine();
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

				int pairedSpaceCount = ToolUtilities.calcBraceIndent(prevCharIndex, contents); // , 1);
				if (pairedSpaceCount == -1)
					return false;

				editor.getTextArea().setSelectionStart(lineStartIndex);
				editor.setSelectedText(ToolUtilities.addSpaces(pairedSpaceCount));

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

	private void handleNewLine() {
		char[] code = editor.getText().toCharArray();

		if (Preferences.getBoolean("editor.indent")) {

			int caretPos = editor.getCaretOffset();

			// this is the previous character
			// (i.e. when you hit return, it'll be the last character
			// just before where the newline will be inserted)
			// int prevPos = caretPos - 1;

			// if the previous thing is a brace (whether prev line or
			// up farther) then the correct indent is the number of spaces
			// on that line + 'indent'.
			// if the previous line is not a brace, then just use the
			// identical indentation to the previous line

			// calculate the amount of indent on the previous line
			// this will be used *only if the prev line is not an indent*
			int spaceCount = ToolUtilities.getLineIndentationOfOffset(caretPos - 1);

			// Let's check if the last character is an open brace, then indent.
			int index = caretPos - 1;
			while ((index >= 0) && Character.isWhitespace(code[index])) {
				index--;
			}
			if (index != -1) {
				// still won't catch a case where prev stuff is a comment
				if (code[index] == '{') {
					// intermediate lines be damned,
					// use the indent for this line instead
					spaceCount = ToolUtilities.getLineIndentationOfOffset(index);
					spaceCount += TAB_SIZE;
				}
			}

			// now before inserting this many spaces, walk forward from
			// the caret position and count the number of spaces,
			// so that the number of spaces aren't duplicated again
			index = caretPos;
			int extraSpaceCount = 0;
			while ((index < code.length) && (code[index] == ' ')) {
				extraSpaceCount++;
				index++;
			}
			int braceCount = 0;
			while ((index < code.length) && (code[index] != '\n')) {
				if (code[index] == '}') {
					braceCount++;
				}
				index++;
			}

			// Hitting return on a line with spaces *after* the caret
			// can cause trouble. For 0099, was ignoring the case, but this is
			// annoying, so in 0122 we're trying to fix that.
			spaceCount -= extraSpaceCount;

			if (spaceCount < 0) {
				editor.getTextArea().setSelectionEnd(editor.getSelectionStop() - spaceCount);
				editor.setSelectedText(NL);
				editor.getTextArea().setCaretPosition(editor.getCaretOffset() + extraSpaceCount + spaceCount);
			} else {
				String insertion = NL + ToolUtilities.addSpaces(spaceCount);
				editor.setSelectedText(insertion);
				editor.getTextArea().setCaretPosition(editor.getCaretOffset() + extraSpaceCount);
			}

			// not gonna bother handling more than one brace
			if (braceCount > 0) {
				System.out.println("1");
				int selectionStart = editor.getSelectionStart();

				if (selectionStart - TAB_SIZE >= 0) {
					System.out.println("2");
					editor.setSelection(selectionStart - TAB_SIZE, selectionStart);

					// if these are spaces that we can delete
					if (editor.getSelectedText().equals(TAB)) {
						System.out.println("3");
						editor.setSelectedText("");
					} else {
						System.out.println("4");
						editor.setSelection(selectionStart, selectionStart);
					}
				}
			}
		} else {
			// Enter/Return was being consumed by somehow even if false
			// was returned, so this is a band-aid to simply fire the event again.
			editor.setSelectedText(NL);
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

		// BracketCloser.update(e.getKeyChar());
		handleInputMethodCommit();
		e.consume();

		return false;
	}

}