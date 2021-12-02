package code_assistant.util;

import processing.app.ui.Editor;

public class Selection {
	private String text = "";
	private int start, end, startLine, endLine;

	public Selection(Editor editor) {
		processing.app.syntax.JEditTextArea textarea = editor.getTextArea();

		startLine = textarea.getSelectionStartLine();
		endLine = textarea.getSelectionStopLine();

		// in case this selection ends with the caret at the beginning of the last line,
		// not selecting any text
		if (editor.isSelectionActive() && editor.getLineStartOffset(endLine) == editor.getSelectionStop()) {
			endLine--;
		}

		start = editor.getLineStartOffset(startLine);
		end = Math.max(start, editor.getLineStopOffset(endLine) - 1);

		text = editor.getText(start, end);
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getEndLine() {
		return endLine;
	}

	public String getText() {
		return text;
	}

	public boolean isEmpty() {
		return text.isEmpty();
	}
}
