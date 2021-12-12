package code_assistant.tool;

import static code_assistant.util.Constants.*;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.Preferences;
import processing.app.ui.Editor;

public class DefaultInputs implements ActionTrigger {
	protected Map<String, Action> actions = new HashMap<>();
	protected Editor editor;

	public DefaultInputs(Editor editor) {
		this.editor = editor;

		actions.put("AS+UP", DUPLICATE_UP);
		actions.put("AS+DOWN", DUPLICATE_DOWN);
		actions.put("A+UP", MOVE_UP);
		actions.put("A+DOWN", MOVE_DOWN);
		actions.put("TAB", INDENT_TEXT);
		actions.put("S+TAB", OUTDENT_TEXT);
		actions.put("A+ENTER", INSERT_NEW_LINE_BELLOW);
		actions.put("C+E", DELETE_LINE);
		actions.put("CS+E", DELETE_LINE_CONTENT);
	}
	
	@Override
	public Map<String, Action> getActions() {
		return actions;
	}

	private final Action DELETE_LINE = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			deleteLine(editor.getTextArea().getCaretLine());
		}
	};

	private final Action DELETE_LINE_CONTENT = new AbstractAction("delete-line-content") {
		@Override
		public void actionPerformed(ActionEvent e) {
			deleteLineContent(editor.getTextArea().getCaretLine());
		}
	};	

	private final Action DUPLICATE_UP = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			duplicateLines(true);
		}
	};

	private final Action DUPLICATE_DOWN = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			duplicateLines(false);
		}
	};

	private final Action MOVE_UP = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			moveLines(true);
		}
	};

	private final Action MOVE_DOWN = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			moveLines(false);
		}
	};
	
	private final Action INSERT_NEW_LINE_BELLOW = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			insertNewLineBellow(editor.getCaretOffset());
		}
	};

	private final Action INDENT_TEXT = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleTabulation(false);
		}
	};

	private final Action OUTDENT_TEXT = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleTabulation(true);
		}
	};

	/*
	 * ******** METHODS ********
	 */

	private void duplicateLines(boolean up) {
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

	private void deleteLine(int line) {
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

	private void deleteLineContent(int line) {
		int start = editor.getTextArea().getLineStartNonWhiteSpaceOffset(line);
		int end = editor.getLineStopOffset(line) - 1;

		editor.setSelection(start, end);
		editor.setSelectedText("");
	}

	private void moveLines(boolean moveUp) {
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
		
		s = new Selection(editor);
		
		String lineText = editor.getLineText(s.getStartLine());
		
//		if (lineText.matches(BLOCK_OPENING) || lineText.matches(BLOCK_CLOSING)) {
//			System.out.println("brace!!! - " + lineText);		
//			editor.stopCompoundEdit();
//			return;
//		}
		
		int brace = EditorUtil.getMatchingBraceLine(s.getStartLine(), true);
		int blockIndentation = 0;

		if (brace != -1) { // an opening brace was found, we are in a block scope
			blockIndentation = EditorUtil.getLineIndentation(brace) + TAB_SIZE;
		} 
				
		boolean isBrace =  (lineText.matches(BLOCK_OPENING) || lineText.matches(BLOCK_CLOSING));
		
		int selectionIndent = EditorUtil.getLineIndentation(isBrace ? s.getStartLine() - 1: s.getStartLine());
		
		System.out.println("brace line: " + brace
				+ " - blockIndentation: " + blockIndentation 
				+ " - selection indent: " + selectionIndent
				+ " - s.getStartLine(): " + (isBrace ? s.getStartLine() - 1: s.getStartLine()));
		
		if (selectionIndent < blockIndentation) {
			editor.handleIndent();
			
		} else if (selectionIndent > blockIndentation) {
			editor.handleOutdent();
		}

		editor.stopCompoundEdit();
	}

	private void insertNewLineBellow(int offset) {
		int line = editor.getTextArea().getLineOfOffset(offset);
		String lineText = editor.getLineText(line);

		int indent = 0;
		int caretPos = EditorUtil.caretPositionInsideLine();

		if (lineText.matches(BLOCK_OPENING)) {
			indent = EditorUtil.getLineIndentation(line);

			if (caretPos > lineText.indexOf('{'))
				indent += TAB_SIZE;

		} else if (lineText.matches(BLOCK_CLOSING)) {
			indent = EditorUtil.getLineIndentation(line);
			int closeBrace = lineText.indexOf('}');

			if (caretPos <= closeBrace) {
				offset += (closeBrace - caretPos);
				editor.setSelection(offset, offset);
				editor.insertText(TAB);
				offset += TAB_SIZE;
			}

		} else {
			int startBrace = EditorUtil.getMatchingBraceLine(line, true);

			if (startBrace != -1) // an opening brace was found, we are in a block scope
				indent = EditorUtil.getLineIndentation(startBrace) + TAB_SIZE;
		}

		editor.startCompoundEdit();
		editor.insertText(NL + EditorUtil.addSpaces(indent));
		editor.setSelection(offset, offset);
		editor.stopCompoundEdit();
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
}