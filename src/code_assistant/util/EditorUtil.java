package code_assistant.util;

import static code_assistant.util.Constants.*;

import java.util.ArrayDeque;
import java.util.Deque;

import processing.app.syntax.Brackets;
import processing.app.ui.Editor;

public final class EditorUtil {
	static private Editor editor;

	private EditorUtil() {
	}

	static public void init(Editor _editor) {
		editor = _editor;
	}

	static public int getLineIndentation(String lineText) {
		char[] chars = lineText.toCharArray();
		int index = 0;

		while (index < chars.length && Character.isWhitespace(chars[index])) {
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

//	static public String indentText(String text, int indent) {
//		String[] lines = text.split(NL);
//		StringBuffer sb = new StringBuffer();
//
//		for (String line : lines) {
//			line = line.stripLeading();
//			sb.append(addSpaces(indent).concat(line).concat(NL));
//		}
//		return sb.toString();
//	}

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

	static public int getBlockDepth(int line) {
		if (line < 0 || line > editor.getLineCount() - 1)
			return 0;

		int depthUp = 0;
		int depthDown = 0;
		int lineIndex = line;

		boolean isTheFirstBlock = true;

		while (lineIndex >= 0) {
			String lineText = editor.getLineText(lineIndex);

			if (lineText.matches(BLOCK_OPENING)) {
				depthUp++;
			}

			else if (lineText.matches(BLOCK_CLOSING)) {
				depthUp--;
				isTheFirstBlock = false;
			}

			lineIndex--;
		}

		lineIndex = line;
		boolean isTheLastBlock = true;

		if (editor.getLineText(lineIndex).matches(BLOCK_OPENING)) {
			depthDown = 1;
		}

		while (lineIndex < editor.getLineCount()) {
			String lineText = editor.getLineText(lineIndex);

			if (lineText.matches(BLOCK_CLOSING))
				depthDown++;

			else if (lineText.matches(BLOCK_OPENING)) {
				depthDown--;
				isTheLastBlock = false;
			}

			lineIndex++;
		}
		
		isTheFirstBlock &= (depthUp == 1 && depthDown == 0);
		isTheLastBlock &= (depthDown == 1 && depthUp == 0);
				
		if (isTheFirstBlock && isTheLastBlock)
			return 0;
		if (isTheFirstBlock || isTheLastBlock)
			return 1;
		
		return Math.max(0, Math.min(depthUp, depthDown));
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
	
	// HACK
	static public int getMatchingBraceLineAlt(int lineIndex) {
		if (lineIndex < 0) {
			return -1;
		}

		int blockDepth = 1;
		boolean first = true;

		while (lineIndex >= 0) {
			String lineText = editor.getLineText(lineIndex);

			if (lineText.matches(BLOCK_CLOSING)) {
				blockDepth++;
				lineIndex--;

			} else if (lineText.matches(BLOCK_OPENING) && !first) {
				blockDepth--;

				if (blockDepth == 0)
					return lineIndex;

				lineIndex--;

			} else {
				lineIndex--;
			}
			first = false;
		}
		return -1;
	}
	
	static public boolean checkBracketsBalance(String text, String leftBrackets, String rightBrackets) {
        // Using ArrayDeque is faster than using Stack class
        Deque<Character> stack = new ArrayDeque<>();

        for (char ch : text.toCharArray()) {

        	if (leftBrackets.contains(String.valueOf(ch))) {
                stack.push(ch);
                continue;
            }
            if (rightBrackets.contains(String.valueOf(ch))) {
                if (stack.isEmpty())
                    return false;

                var top = stack.pop();
                if (leftBrackets.indexOf(top) != rightBrackets.indexOf(ch))
                    return false;
            }
        }
        return stack.isEmpty();
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

	static public char prevChar() {
		return prevChar(editor.getText(), editor.getCaretOffset() - 1);
	}

	static public char prevChar(int index) {
		return prevChar(editor.getText(), index);
	}

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
}
