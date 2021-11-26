package code_assistant.tool;

import processing.app.ui.Editor;

public class Selection {
	private String text = "";
	private int start, end, startLine, endLine;

	public Selection(Editor editor) {
		processing.app.syntax.JEditTextArea textarea = editor.getTextArea();

		startLine = textarea.getSelectionStartLine();
		endLine = textarea.getSelectionStopLine();

		if (editor.getSelectionStop() == editor.getLineStartOffset(endLine)) {
			endLine--;
		}

		start = editor.getLineStartOffset(startLine);
		end = editor.getLineStopOffset(endLine) - 1;
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
}
