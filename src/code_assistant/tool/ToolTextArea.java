package code_assistant.tool;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import processing.app.ui.Editor;

public class ToolTextArea implements ToolConstants {
	private static Editor editor;

	public static void init(Editor _editor) {
		editor = _editor;
	}

	public static final AbstractAction DELETE_LINE = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			deleteLine(editor.getTextArea().getCaretLine());
		}
	};

	public static final AbstractAction DELETE_LINE_CONTENT = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			deleteLineContent(editor.getTextArea().getCaretLine());
		}
	};

	public static final AbstractAction DUPLICATE_UP = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			duplicateLines(true);

		}
	};

	public static final AbstractAction DUPLICATE_DOWN = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			duplicateLines(false);
		}
	};

	public static final AbstractAction MOVE_UP = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			moveLines(true);
		}
	};

	public static final AbstractAction MOVE_DOWN = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			moveLines(false);
		}
	};

	/*
	 * ******** METHODS ********
	 */

	/*
	 * TODO: corrigir bug quando se duplica uma linha e o caret esta na posicao zero
	 * da linha!!!
	 */
	private static void duplicateLines(boolean up) {
		Selection s = new Selection(editor);

		if (s.getEndLine() == editor.getLineCount() - 1) {
			int caret = editor.getLineStopOffset(s.getEndLine());
			editor.setSelection(caret, caret);
			editor.insertText(NL + s.getText());

		} else {
			int caret = s.getEnd() + 1;
			editor.setSelection(caret, caret);
			editor.insertText(s.getText() + NL);
		}

		if (up)
			editor.setSelection(s.getEnd(), s.getStart());
		else
			editor.setSelection(editor.getCaretOffset() - 1, s.getEnd() + 1);
	}

	private static void deleteLine(int line) {
		// in case we are in the last line of text (but not when it's also first one)
		if (line == editor.getLineCount() - 1 && line != 0) {
			// subtracting 1 from getLineStartOffset() will delete the line break prior
			// to this line, causing the caret to move to the end of the previous line
			int start = editor.getLineStartOffset(line) - 1;
			int end = editor.getLineStopOffset(line) - 1;

			editor.setSelection(start, end);
			editor.setSelectedText("");

		} else if (editor.getLineCount() > 1) {
			editor.setLineText(line, "");

		} else { // in case we are deleting the only line that remains
			if (editor.getLineText(line).isEmpty()) {
				editor.getToolkit().beep();
				return;
			}
			editor.setText("");
		}
	}

	private static void deleteLineContent(int line) {
		int start = editor.getTextArea().getLineStartNonWhiteSpaceOffset(line);
		int end = editor.getLineStopOffset(line) - 1;

		editor.setSelection(start, end);
		editor.setSelectedText("");
	}

	private static void moveLines(boolean moveUp) {
		Selection s = new Selection(editor);

		int targetLine = moveUp ? s.getStartLine() - 1 : s.getEndLine() + 1;

		if (targetLine < 0 || targetLine >= editor.getLineCount()) {
			editor.getToolkit().beep();
			return;
		}

		int target_start = editor.getLineStartOffset(targetLine);
		int target_end = editor.getLineStopOffset(targetLine) - 1;

		String selectedText = s.getText();
		String replacedText = editor.getText(target_start, target_end);

		editor.startCompoundEdit();
		{
			editor.setSelection(s.getStart(), s.getEnd());

			int newSelectionStart, newSelectionEnd;

			// swap lines
			if (moveUp) {
				editor.setSelection(target_start, s.getEnd());
				editor.setSelectedText(selectedText + NL + replacedText);

				newSelectionStart = editor.getLineStartOffset(targetLine);
				newSelectionEnd = editor.getLineStopOffset(s.getEndLine() - 1) - 1;
			} else {
				editor.setSelection(s.getStart(), target_end);
				editor.setSelectedText(replacedText + NL + selectedText);

				newSelectionStart = editor.getLineStartOffset(s.getStartLine() + 1);
				newSelectionEnd = editor.getLineStopOffset(targetLine) - 1;
			}

			// update selection
			editor.setSelection(newSelectionStart, newSelectionEnd);
		}
		editor.stopCompoundEdit();
	}
}