package code_assistant.tool;

import processing.app.ui.Editor;

public final class ToolUtilities implements ToolConstants {
	static Editor editor;

	private ToolUtilities() {
	}

	public static void init(Editor _editor) {
		editor = _editor;
	}

	public static int getLineIndentation(int line) {
		int start = editor.getLineStartOffset(line);
		int end = editor.getTextArea().getLineStartNonWhiteSpaceOffset(line);
		return end - start;
	}

	public static int getLineIndentationOfOffset(int offset) {
		int line = editor.getTextArea().getLineOfOffset(offset);
		return getLineIndentation(line);
	};

	public static int getSelectionIndentation(int startLine, int endLine) {
		int result = getLineIndentation(startLine);

		for (int line = startLine + 1; line <= endLine; line++) {
			int currIndent = getLineIndentation(line);
			if (currIndent < result)
				result = currIndent;
		}
		return result;
	}

	public static String indentText(String text) {
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

	public static String outdentText(String text) {
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

	public static String addSpaces(int length) {
		if (length == 0)
			return "";
		return String.format("%1$" + length + "s", "");
	}

	public static int caretPositionInsideLine() {
		int caretOffset = editor.getCaretOffset();
		int lineStartOffset = editor.getLineStartOffset(editor.getTextArea().getCaretLine());

		return caretOffset - lineStartOffset;
	}

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
}
