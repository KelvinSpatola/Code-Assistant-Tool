package code_assistant.tool;

import static code_assistant.util.Constants.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;

import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.Language;
import processing.app.Preferences;
import processing.app.ui.Editor;


public class JavaModeInputs implements KeyHandler {
	static final String COMMENT_TEXT = "^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)";
	static final String STRING_TEXT = "^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}";
	static final String SPLIT_STRING_TEXT = "^\\h*\\+\\s*(?:\\\".*){2}";
	
	static private Editor editor;

	public JavaModeInputs(Editor _editor) {
		editor = _editor;

		actions.put("ENTER", HANDLE_ENTER);
		actions.put("CA+RIGHT", SELECT_BLOCK);

		CodeAssistantInputHandler.addKeyBinding(editor, "C+T", "format-selected-text",
				JavaModeInputs.FORMAT_SELECTED_TEXT);
	}

	static public final AbstractAction FORMAT_SELECTED_TEXT = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			formatSelectedText();
		}
	};

	static public final AbstractAction SELECT_BLOCK = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			selectBlockOfCode();
		}
	};

	static public final AbstractAction HANDLE_ENTER = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			handleEnter();
		}
	};

	/*
	 * ******** METHODS ********
	 */

	static private void handleEnter() {
		int caretLine = editor.getTextArea().getCaretLine();
		String lineText = editor.getLineText(caretLine);

		boolean matches_a_string = lineText.matches(STRING_TEXT);
		boolean matches_a_comment = lineText.matches(COMMENT_TEXT);

		// if it's neither a string nor a comment
		if (!matches_a_string && !matches_a_comment) {

//			if (lineText.matches("^.*?\\{\\s*\\}?\\h*$")) {
//				editor.startCompoundEdit();
//				editor.insertText("\n" + Util.addSpaces(Util.getLineIndentation(caretLine) + TAB));
//				int newCaret = editor.getCaretOffset();
//
//				editor.insertText("\n" + Util.addSpaces(Util.getLineIndentation(caretLine)));
//				editor.setSelection(newCaret, newCaret);
//				editor.stopCompoundEdit();
//			} else {
//				insertNewLineBellowCurrentLine(caretLine);
//			}

			handleNewLine();
			return;
		}

		int stringStart = lineText.indexOf("\"");
		int stringStop = lineText.lastIndexOf("\"") + 1;
		int commentStart = lineText.indexOf("/*");
		int commentStop = (lineText.contains("*/") ? lineText.indexOf("*/") : lineText.length()) + 2;

		int caretPos = EditorUtil.caretPositionInsideLine(editor);
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
			handleNewLine();
	}

	static private void splitString(int caretLine) {
		int indent = EditorUtil.getLineIndentation(editor, caretLine);
		if (!editor.getLineText(caretLine).matches(SPLIT_STRING_TEXT))
			indent += TAB_SIZE;

		editor.stopCompoundEdit();
		editor.insertText("\"\n" + EditorUtil.addSpaces(indent) + "+ \"");
		editor.stopCompoundEdit();
	}

	static private void splitComment(int caretLine) {
		int indent = EditorUtil.getLineIndentation(editor, caretLine);

		editor.startCompoundEdit();
		editor.insertText(NL + EditorUtil.addSpaces(indent - (indent % TAB_SIZE)) + " * ");

		int caretPos = editor.getCaretOffset();
		String nextText = editor.getText().substring(caretPos);

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

	static private void createBlockScope() {

	}

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
			int spaceCount = EditorUtil.getLineIndentationOfOffset(editor, caretPos - 1);

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
					spaceCount = EditorUtil.getLineIndentationOfOffset(editor, index);
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

	static private void selectBlockOfCode() {
		final char OPEN_BRACE = '{';
		final char CLOSE_BRACE = '}';

		int start = editor.getSelectionStart();
		int end = editor.getSelectionStop();

		Selection s = new Selection(editor);

		int startLine = s.getStartLine();
		int endLine = s.getEndLine();

		if (editor.isSelectionActive()) {
			String code = editor.getText();

			if (code.charAt(start - 1) == OPEN_BRACE && code.charAt(end) == CLOSE_BRACE) {
				startLine--;
				endLine++;
			}
		}

		// go up and search for the corresponding open brace
		int matchingLine = EditorUtil.getMatchingBraceLine(editor, startLine, true);

		// open brace not found
		if (matchingLine == -1) {
			return;
		}

		int lineEnd = editor.getLineStopOffset(matchingLine) - 1;
		start = EditorUtil.getOffsetOfPrevious(editor, OPEN_BRACE, lineEnd) + 1;

		// now go down and search for the corresponding close brace
		matchingLine = EditorUtil.getMatchingBraceLine(editor, endLine, false);

		// close brace not found
		if (matchingLine == -1) {
			return;
		}

		lineEnd = editor.getLineStopOffset(matchingLine) - 1;
		end = EditorUtil.getOffsetOfPrevious(editor, CLOSE_BRACE, lineEnd);

		editor.setSelection(start, end);
	}

	static private void formatSelectedText() {
		if (editor.isSelectionActive()) {

			if (editor.getSelectedText().isBlank()) {
				return;
			}

			Selection s = new Selection(editor);

			String selectedText;
			
			 // long string literals are formatted here
			if (Preferences.getBoolean("code_assistant.auto_format.strings")) {
				selectedText = refactorStringLiterals(s.getText());
			} else {
				selectedText = s.getText();
			}
			
			// and everything else is formatted here
			String formattedText = editor.createFormatter().format(selectedText);

			// but they need to be indented, anyway...
			int brace = EditorUtil.getMatchingBraceLine(editor, s.getStartLine() - 1, true);
			int indent = 0;

			if (brace != -1) {
				indent = EditorUtil.getLineIndentation(editor, brace) + TAB_SIZE;
			}

			formattedText = EditorUtil.indentText(formattedText, indent);

			if (selectedText.equals(formattedText.stripTrailing())) {
				editor.statusNotice(Language.text("editor.status.autoformat.no_changes"));

			} else {
				int start = s.getStart();
				int end = s.getEnd() + 1;

				editor.startCompoundEdit();

				editor.setSelection(start, end);
				editor.setSelectedText(formattedText);

				end = start + formattedText.length() - 1;
				editor.setSelection(start, end);

				editor.stopCompoundEdit();

				editor.getSketch().setModified(true);
				editor.statusNotice(Language.text("editor.status.autoformat.finished"));
			}

		} else {
			int caretPos = editor.getCaretOffset();
			int scrollPos = editor.getScrollPosition();

			editor.handleSelectAll();
			formatSelectedText();
			editor.setSelection(caretPos, caretPos);

			if (scrollPos != editor.getScrollPosition()) {
				editor.getTextArea().setVerticalScrollPosition(scrollPos);
			}
		}
	}

//	static int getIndentation(String lineText) {
//		char[] chars = lineText.toCharArray();
//		int index = 0;
//
//		while (Character.isWhitespace(chars[index])) {
//			index++;
//		}
//		return index;
//	}

	static private String refactorStringLiterals(String text) {
		int maxLength = Preferences.getInteger("code_assistant.auto_format.line_length");

		List<String> lines = new ArrayList<>(Arrays.asList(text.split(NL)));
		int depth = 0;
		int indent = 0;

		for (int i = 0; i < lines.size(); i++) {
			String lineText = lines.get(i);

			if (lineText.matches(STRING_TEXT) && lineText.length() > maxLength) {

				if (depth == 0) {
					indent = EditorUtil.getLineIndentation(lineText);
				}
				
				String preffix = EditorUtil.addSpaces(indent) + TAB + "+ \"";

				String currLine = lineText.substring(0, maxLength - 1) + "\"";
				String nextLine = preffix + lineText.substring(maxLength - 1);

				lines.set(i, currLine);
				lines.add(i + 1, nextLine);
				depth++;

			} else {
				lines.set(i, lineText);
				depth = 0;
				indent = 0;
			}
		}

		StringBuilder result = new StringBuilder();

		for (String line : lines) {
			result.append(line + NL);
		}

		return result.toString();
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		if (e.getKeyChar() == '}') {
			if (Preferences.getBoolean("editor.indent")) {
				editor.startCompoundEdit();

				// erase any selection content
				if (editor.isSelectionActive()) {
					editor.setSelectedText("");
				}

				int line = editor.getTextArea().getCaretLine();

				// don't do anything, this line has other stuff on it
				if (!editor.getLineText(line).isBlank()) {
					editor.stopCompoundEdit();
					return false;
				}

				int startBrace = EditorUtil.getMatchingBraceLine(editor, true);

				// open brace not found
				if (startBrace == -1) {
					editor.stopCompoundEdit();
					return false;
				}

				int indent = EditorUtil.getLineIndentation(editor, startBrace);

				editor.setSelection(editor.getLineStartOffset(line), editor.getCaretOffset());
				editor.setSelectedText(EditorUtil.addSpaces(indent));

				editor.stopCompoundEdit();
				return true;
			}
		}
		return false;
	}

	private static void println(Object... objects) {
		for (Object o : objects) {
			System.out.println(o.toString());
		}
	}
}