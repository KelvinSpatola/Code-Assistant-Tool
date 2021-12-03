package code_assistant.util;

import processing.app.ui.Editor;

public final class EditorUtil implements ToolConstants {
	static Editor editor;

	private EditorUtil() {
	}

	static public void init(Editor _editor) {
		editor = _editor;
	}

	static public int getLineIndentation(int line) {
		int start = editor.getLineStartOffset(line);
		int end = editor.getTextArea().getLineStartNonWhiteSpaceOffset(line);
		return end - start;
	}

	static public int getLineIndentationOfOffset(int offset) {
		int line = editor.getTextArea().getLineOfOffset(offset);
		return getLineIndentation(line);
	}

	static public int getSelectionIndentation(int startLine, int endLine) {
		int result = getLineIndentation(startLine);

		for (int line = startLine + 1; line <= endLine; line++) {
			int currIndent = getLineIndentation(line);
			if (currIndent < result)
				result = currIndent;
		}
		return result;
	}

	static public String indentText(String text) {
		String[] lines = text.split(NL);
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < lines.length; i++) {
			String str = addSpaces(TAB_SIZE).concat(lines[i]);
			if (i == lines.length - 1)
				sb.append(str);
			else
				sb.append(addSpaces(4).concat(lines[i]).concat(NL));
		}
		return sb.toString();
	}

	static public String outdentText(String text) {
		String[] lines = text.split(NL);
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < lines.length; i++) {
			String str = lines[i].substring(TAB_SIZE);
			if (i < lines.length - 1)
				sb.append(str.concat(NL));
			else
				sb.append(str);
		}
		return sb.toString();
	}

	static public String addSpaces(int length) {
		if (length == 0)
			return "";
		return String.format("%1$" + length + "s", "");
	}

	static public int caretPositionInsideLine() {
		int caretOffset = editor.getCaretOffset();
		int lineStartOffset = editor.getLineStartOffset(editor.getTextArea().getCaretLine());

		return caretOffset - lineStartOffset;
	}

	static public int getMatchingBraceLine() {
		int lineIndex = editor.getTextArea().getCaretLine() - 1;
		int blockDepth = 1;

		while (lineIndex >= 0) {
			String lineText = editor.getLineText(lineIndex);

			if (lineText.matches(BLOCK_CLOSING)) {
				blockDepth++;
				lineIndex--;

			} else if (lineText.matches(BLOCK_OPENING)) {
				blockDepth--;

				if (blockDepth == 0)
					return lineIndex;

				lineIndex--;

			} else {
				lineIndex--;
			}
		}
		return -1;
	}

	static public int getOffsetOfPrevious(char ch) {
		char[] code = editor.getText().toCharArray();
		int index = editor.getCaretOffset();

		while (index >= 0) {
			if (code[index] == ch) {
				return index;
			}
			index--;
		}
		return -1;
	}

	/**
	 * Returns the previous non-white character
	 * 
	 * @return the previous non-white character
	 */
	static public char prevChar(int index) {
		char[] code = editor.getText().toCharArray();

		while (index >= 0) {
			if (!Character.isWhitespace(code[index])) {
				return code[index];
			}
			index--;
		}
		return Character.UNASSIGNED;
	}

	static public char prevChar() {
		return prevChar(editor.getCaretOffset() - 1);
	}
}
