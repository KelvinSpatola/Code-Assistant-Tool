package code_assistant.tool;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import processing.app.Language;
import processing.app.Preferences;
import processing.app.ui.Editor;

public class ToolEditor implements ToolConstants {
	private static Editor editor;

	public static void init(Editor _editor) {
		editor = _editor;
		EditorUtil.init(editor);
	}

	public static final AbstractAction FORMAT_SELECTED_TEXT = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			formatSelectedText();
		}
	};
	

	public static final AbstractAction SELECT_BLOCK = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			selectBlockOfCode();
		}
	};

	
	public static final AbstractAction INSERT_NEW_LINE_BELLOW_CURRENT_LINE = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			insertNewLineBellowCurrentLine(editor.getTextArea().getCaretLine());
		}
	};
	
	public static final AbstractAction HANDLE_ENTER = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {

			int caretLine = editor.getTextArea().getCaretLine();
			String lineText = editor.getLineText(caretLine);

			boolean matches_a_string = lineText.matches(STRING_TEXT);
			boolean matches_a_comment = lineText.matches(COMMENT_TEXT);

			// if it's neither a string nor a comment
			if (!matches_a_string && !matches_a_comment) {

//				if (lineText.matches("^.*?\\{\\s*\\}?\\h*$")) {
//					editor.startCompoundEdit();
//					editor.insertText("\n" + Util.addSpaces(Util.getLineIndentation(caretLine) + TAB));
//					int newCaret = editor.getCaretOffset();
//
//					editor.insertText("\n" + Util.addSpaces(Util.getLineIndentation(caretLine)));
//					editor.setSelection(newCaret, newCaret);
//					editor.stopCompoundEdit();
//				} else {
//					insertNewLineBellowCurrentLine(caretLine);
//				}
				//insertNewLineBellowCurrentLine(caretLine);  <------
				
				handleNewLine();
				return;
			}

			int stringStart = lineText.indexOf("\"");
			int stringStop = lineText.lastIndexOf("\"") + 1;
			int commentStart = lineText.indexOf("/*");
			int commentStop = (lineText.contains("*/") ? lineText.indexOf("*/") : lineText.length()) + 2;

			int caretPos = EditorUtil.caretPositionInsideLine();
			boolean isString = matches_a_string && (caretPos > stringStart && caretPos < stringStop);
			boolean isComment = matches_a_comment && (caretPos > commentStart && caretPos < commentStop);

			/*
			 * We gotta check if this line starts with a "*" that doesn't come from a
			 * comment. The only way to do this is by checking if the previous line of text
			 * is a comment line.
			 */
			if (isComment && !lineText.contains("/*")) {
				String prevLine = editor.getLineText(caretLine - 1);

				if (!prevLine.matches(COMMENT_TEXT) || prevLine.contains("*/")) {
					isComment = false;
				}
			}

			// finally ...

			if (isString)
				splitString(caretLine);

			else if (isComment)
				splitComment(caretLine);

			else
				//insertNewLineBellowCurrentLine(caretLine);
				handleNewLine();
		}
	};

	/*
	 * ******** METHODS ********
	 */
	
	static private void handleNewLine() {
		char[] code = editor.getText().toCharArray();

		if (Preferences.getBoolean("editor.indent")) {

			int caretPos = editor.getCaretOffset();

			// if the previous thing is a brace (whether prev line or
			// up farther) then the correct indent is the number of spaces
			// on that line + 'indent'.
			// if the previous line is not a brace, then just use the
			// identical indentation to the previous line

			// calculate the amount of indent on the previous line
			// this will be used *only if the prev line is not an indent*
			int spaceCount = EditorUtil.getLineIndentationOfOffset(caretPos - 1);

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
					spaceCount = EditorUtil.getLineIndentationOfOffset(index);
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
				String insertion = NL + EditorUtil.addSpaces(spaceCount);
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

	static private void splitString(int caretLine) {
		int indent = EditorUtil.getLineIndentation(caretLine);
		if (!editor.getLineText(caretLine).matches(SPLIT_STRING_TEXT))
			indent += TAB_SIZE;

		editor.stopCompoundEdit();
		editor.insertText("\"\n" + EditorUtil.addSpaces(indent) + "+ \"");
		editor.stopCompoundEdit();
	}

	static private void splitComment(int caretLine) {
		int indent = EditorUtil.getLineIndentation(caretLine);

		editor.startCompoundEdit();
		editor.insertText(NL + EditorUtil.addSpaces(indent - (indent % TAB_SIZE)) + " * ");

		int caretPos = editor.getCaretOffset();
		String nextText = editor.getText().substring(caretPos); // .replace('\n', ' ');

		/*
		 * Checking if we need to close this comment
		 */
		int openingToken = nextText.indexOf("/*");
		int closingToken = nextText.indexOf("*/");
		boolean commentIsOpen = (closingToken == -1) || (closingToken > openingToken && openingToken != -1);

		if (commentIsOpen) {
			editor.getTextArea().setCaretPosition(editor.getLineStopOffset(++caretLine) - 1);
			editor.insertText(NL + EditorUtil.addSpaces(indent - (indent % TAB_SIZE)) + " */");
			editor.getTextArea().setCaretPosition(caretPos);
		}
		editor.stopCompoundEdit();
	}

	static private void insertNewLineBellowCurrentLine(int caretLine) {
		int indent = EditorUtil.getLineIndentation(caretLine);
		String lineText = editor.getLineText(caretLine);

		if (lineText.contains("{") && (EditorUtil.caretPositionInsideLine() > lineText.indexOf("{")))
			indent += TAB_SIZE;

		int caretPos = editor.getCaretOffset();

		editor.startCompoundEdit();
		editor.insertText(NL + (indent > 0 ? EditorUtil.addSpaces(indent) : ""));
		editor.getTextArea().setCaretPosition(caretPos);
		editor.stopCompoundEdit();
	}

	static private void formatSelectedText() {
		if (editor.isSelectionActive()) {

			Selection s = new Selection(editor);
			int selectionStart = s.getStart();
			int selectionEnd = s.getEnd();

			if (s.getEndLine() == editor.getLineCount() - 1) {
				selectionEnd--;
			}

			String code = editor.getText();
			String textBeforeSelection = code.substring(0, selectionStart);
			String textAfterSelection = code.substring(selectionEnd + 1);

			String selectedText = s.getText();
			final String formattedText = editor.createFormatter().format(selectedText);

			if (formattedText.equals(selectedText)) {
				editor.statusNotice(Language.text("editor.status.autoformat.no_changes"));

			} else {
				editor.startCompoundEdit();
				editor.setText(textBeforeSelection + formattedText + textAfterSelection);

				selectionEnd = selectionStart + formattedText.length() - 1;
				editor.setSelection(selectionStart, selectionEnd);
				editor.stopCompoundEdit();

				editor.getSketch().setModified(true);
				editor.statusNotice(Language.text("editor.status.autoformat.finished"));
			}
		} else {
			editor.handleAutoFormat();
		}
	}

	static private int blockDepth = 0;

	static private void selectBlockOfCode() {
		final char OPEN_BRACE = '{';
		final char CLOSE_BRACE = '}';

		int caretPos = editor.getCaretOffset();
		String code = editor.getText();

		int start = caretPos;
		int end = caretPos;

		if (editor.isSelectionActive()) {
			start = editor.getSelectionStart() - 1;
			end = editor.getSelectionStop();

			if (code.charAt(start) == OPEN_BRACE && code.charAt(end) == CLOSE_BRACE) {
				start--;
				end++;
			}
		} else {
			blockDepth = 0;
		}

		short skipOpen = 0;
		short skipClose = 0;

		if (caretPos >= 0 && caretPos < code.length()) {
			boolean foundBrace = false;

			// searching for the opening bracket...
			while (!foundBrace) {
				if (start < 0)
					return;

				char ch = code.charAt(start);

				if (ch == OPEN_BRACE) {
					foundBrace = true;
				} else {
					if (ch == CLOSE_BRACE)
						skipClose++;

					start--;
				}
			}

			foundBrace = false;

			// searching for the closing bracket...
			while (!foundBrace /* && skipClose == 0 */) {
				if (end > code.length() - 1)
					return;

				char ch = code.charAt(start);

				if (ch == CLOSE_BRACE) {
					foundBrace = true;
					blockDepth++;
					// System.out.println("block depth: " + blockDepth);

				} else {
					end++;
				}
			}

			editor.setSelection(++start, end);
		}
	}
}