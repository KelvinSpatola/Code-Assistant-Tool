package code_assistant.util;

import static code_assistant.util.Constants.NL;
import static code_assistant.util.Constants.TAB_SIZE;

import processing.app.ui.Editor;


public final class EditorUtil {
	static private final String BLOCK_OPENING = "^(?!.*?\\/+.*?\\{.*|.*\\/\\*.*|\\h*\\*.*).*?\\{.*";
	static private final String BLOCK_CLOSING = "^(?!.*?\\/+.*?\\}.*|.*\\/\\*.*|\\h*\\*.*).*?\\}.*";
	static private Editor editor;

	private EditorUtil() {
	}
	
	static public void init(Editor _editor) {
		editor = _editor;
	}

	static public int getLineIndentation(String lineText) {
		char[] chars = lineText.toCharArray();
		int index = 0;

		while (Character.isWhitespace(chars[index])) {
			index++;
		}
		return index;
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
	
	static public String indentText(String text, int indent) {
		String[] lines = text.split(NL);
		StringBuffer sb = new StringBuffer();

		for (String line : lines) {
			sb.append(addSpaces(indent).concat(line).concat(NL));
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

	static public int getMatchingBraceLine(boolean goUp) {
		return getMatchingBraceLine(editor.getTextArea().getCaretLine(), goUp);
	}

	static public int getMatchingBraceLine(int lineIndex, boolean goUp) {
		if (lineIndex < 0) {
			return -1;
		}

		int blockDepth = 1;

		if (goUp) {

			if (editor.getLineText(lineIndex).matches(BLOCK_CLOSING)) {
				lineIndex--;
			}

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
		} else { // go down

			if (editor.getLineText(lineIndex).matches(BLOCK_OPENING)) {
				lineIndex++;
			}

			while (lineIndex < editor.getLineCount()) {
				String lineText = editor.getLineText(lineIndex);

				if (lineText.matches(BLOCK_OPENING)) {
					blockDepth++;
					lineIndex++;

				} else if (lineText.matches(BLOCK_CLOSING)) {
					blockDepth--;

					if (blockDepth == 0)
						return lineIndex;

					lineIndex++;

				} else {
					lineIndex++;
				}
			}
		}
		return -1;
	}
	
	static public String addSpaces(int length) {
		if (length <= 0)
			return "";
		return String.format("%1$" + length + "s", "");
	}

	static public int caretPositionInsideLine() {
		int caretOffset = editor.getCaretOffset();
		int lineStartOffset = editor.getLineStartOffset(editor.getTextArea().getCaretLine());

		return caretOffset - lineStartOffset;
	}

	static public int getOffsetOfPrevious(char ch) {
		return getOffsetOfPrevious(ch, editor.getCaretOffset());
	}

	static public int getOffsetOfPrevious(char ch, int offset) {
		char[] code = editor.getText(0, offset + 1).toCharArray();

		while (offset >= 0) {
			if (code[offset] == ch) {
				return offset;
			}
			offset--;
		}
		return -1;
	}
 
	/**
	 * Returns the previous non-white character
	 *
	 * @return the previous non-white character
	 */
	static public char prevChar(String text, int index) {
		char[] code = text.toCharArray();

		while (index >= 0) {
			if (!Character.isWhitespace(code[index])) {
				return code[index];
			}
			index--;
		}
		return Character.UNASSIGNED;
	}
	
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
