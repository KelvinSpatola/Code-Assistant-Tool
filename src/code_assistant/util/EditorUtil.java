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
	
//	static public int getContextIndentation(int offset) {
//		return -1;
//	}

	/**
	 * Walk back from 'index' until the brace that seems to be the beginning of the
	 * current block, and return the number of spaces found on that line.
	 */
	static public int calcBraceIndent(int index, char[] contents) {
		// now that we know things are ok to be indented, walk
		// backwards to the last { to see how far its line is indented.
		// this isn't perfect cuz it'll pick up commented areas,
		// but that's not really a big deal and can be fixed when
		// this is all given a more complete (proper) solution.
		int braceDepth = 1;
		boolean finished = false;
		while ((index != -1) && (!finished)) {
			if (contents[index] == '}') {
				// aww crap, this means we're one deeper
				// and will have to find one more extra {
				braceDepth++;
				// if (braceDepth == 0) {
				// finished = true;
				// }
				index--;
			} else if (contents[index] == '{') {
				braceDepth--;
				if (braceDepth == 0) {
					finished = true;
				}
				index--;
			} else {
				index--;
			}
		}
		// never found a proper brace, be safe and don't do anything
		if (!finished)
			return -1;

		// check how many spaces on the line with the matching open brace
		// return calcSpaceCount(index, contents);
		return getLineIndentationOfOffset(index);
	}

	/**
	 * Returns the previous non-white character
	 * 
	 * @return the previous non-white character
	 */
	static public char prevChar(int index) {		
		char[] code = editor.getText().toCharArray();
		
		while (index >= 0) {    
			if(!Character.isWhitespace(code[index])) {
				return code[index];
			}
			index--;
		}
		return Character.UNASSIGNED;
	}
	
	static public char prevChar() {		
		return prevChar(editor.getCaretOffset() - 1);
	}

	/**
	 * Returns the next non-white character
	 * 
	 * @return the next non-white character
	 */
//	static public char nextChar() {		
//		char[] code = editor.getText().toCharArray();
//		int index = editor.getCaretOffset() + 1;
//		
//		while (index < code.length) {    
//			if(!Character.isWhitespace(code[index])) {
//				return code[index];
//			}
//			index++;
//		}
//		return Character.UNASSIGNED;
//	}
}
