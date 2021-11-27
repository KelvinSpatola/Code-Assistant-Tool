package code_assistant.tool;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.AbstractAction;

import processing.app.Language;
import processing.app.ui.Editor;

public class ToolEditor implements RegistrableActions, ToolConstants {
	private static Editor editor;

	public static void init(Editor _editor) {
		editor = _editor;
		ToolUtilities.init(editor);

		actions.put("handle-enter", HANDLE_ENTER);
		actions.put("select-block", SELECT_BLOCK);
		actions.put("format-selected-text", FORMAT_SELECTED_TEXT);
	}

	public static Map.Entry<String, AbstractAction> getAction(String actionName) {
		return RegistrableActions.getAction(actionName);
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
				insertNewLineBellowCurrentLine(caretLine);
				return;
			}

			int stringStart = lineText.indexOf("\"");
			int stringStop = lineText.lastIndexOf("\"") + 1;
			int commentStart = lineText.indexOf("/*");
			int commentStop = (lineText.contains("*/") ? lineText.indexOf("*/") : lineText.length()) + 2;

			int caretPos = ToolUtilities.caretPositionInsideLine();
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
				insertNewLineBellowCurrentLine(caretLine);
		}
	};

	/*
	 * ******** METHODS ********
	 */

	private static int blockDepth = 0;

	private static void selectBlockOfCode() {
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

	private static void splitString(int caretLine) {
		int indent = ToolUtilities.getLineIndentation(caretLine);
		if (!editor.getLineText(caretLine).matches(SPLIT_STRING_TEXT))
			indent += TAB_SIZE;

		editor.stopCompoundEdit();
		editor.insertText("\"\n" + ToolUtilities.addSpaces(indent) + "+ \"");
		editor.stopCompoundEdit();
	}

	private static void splitComment(int caretLine) {
		int indent = ToolUtilities.getLineIndentation(caretLine);

		editor.startCompoundEdit();
		editor.insertText(NL + ToolUtilities.addSpaces(indent - (indent % TAB_SIZE)) + " * ");

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
			editor.insertText(NL + ToolUtilities.addSpaces(indent - (indent % TAB_SIZE)) + " */");
			editor.getTextArea().setCaretPosition(caretPos);
		}
		editor.stopCompoundEdit();
	}

	private static void insertNewLineBellowCurrentLine(int caretLine) {
		int indent = ToolUtilities.getLineIndentation(caretLine);
		String lineText = editor.getLineText(caretLine);

		if (lineText.contains("{") && (ToolUtilities.caretPositionInsideLine() > lineText.indexOf("{")))
			indent += TAB_SIZE;

		int caretPos = editor.getCaretOffset();

		editor.startCompoundEdit();
		editor.insertText(NL + (indent > 0 ? ToolUtilities.addSpaces(indent) : ""));
		editor.getTextArea().setCaretPosition(caretPos);
		editor.stopCompoundEdit();
	}

	private static void formatSelectedText() {
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
}