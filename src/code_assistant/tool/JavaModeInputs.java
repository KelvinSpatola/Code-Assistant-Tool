package code_assistant.tool;

import static code_assistant.util.Constants.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.Language;
import processing.app.Preferences;
import processing.app.syntax.Brackets;
import processing.app.ui.Editor;

public class JavaModeInputs implements ActionTrigger, KeyPressedListener {
	static final String COMMENT_TEXT = "^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)";
	static final String STRING_TEXT = "^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}";
	static final String SPLIT_STRING_TEXT = "^\\h*\\+\\s*(?:\\\".*){2}";

	protected Map<String, Action> actions = new HashMap<>();
	protected Editor editor;

	public JavaModeInputs(Editor editor) {
		this.editor = editor;
		EditorUtil.init(editor);

		actions.put("ENTER", HANDLE_ENTER);
		actions.put("CA+RIGHT", EXPAND_SELECTION);
		actions.put("C+T", FORMAT_SELECTED_TEXT);
	}

	@Override
	public Map<String, Action> getActions() {
		return actions;
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

				int startBrace = EditorUtil.getMatchingBraceLine(true);

				// open brace not found
				if (startBrace == -1) {
					editor.stopCompoundEdit();
					return false;
				}

				int indent = EditorUtil.getLineIndentation(startBrace);

				editor.setSelection(editor.getLineStartOffset(line), editor.getCaretOffset());
				editor.setSelectedText(EditorUtil.addSpaces(indent));

				editor.stopCompoundEdit();
				return true;
			}
		}
		return false;
	}

	/*
	 * ******** ACTIONS ********
	 */

	private final Action FORMAT_SELECTED_TEXT = new AbstractAction("format-selected-text") {
		@Override
		public void actionPerformed(ActionEvent e) {
			formatSelectedText();
		}
	};

	private final Action EXPAND_SELECTION = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			expandSelection();
		}
	};

	private final Action HANDLE_ENTER = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleEnter();
		}
	};

	/*
	 * ******** METHODS ********
	 */

	private void handleEnter() {
		int caretPos = EditorUtil.caretPositionInsideLine();
		int caretLine = editor.getTextArea().getCaretLine();
		String lineText = editor.getLineText(caretLine);

		if (lineText.matches(STRING_TEXT)) {
			int stringStart = lineText.indexOf("\"");
			int stringStop = lineText.lastIndexOf("\"") + 1;

			if (caretPos > stringStart && caretPos < stringStop) {
				splitString(caretLine);
				return;
			}
		}

		if (lineText.matches(COMMENT_TEXT)) {
			if (!lineText.contains("/*")) {
				int line = caretLine - 1;

				while (line >= 0) {
					if (!editor.getLineText(line).matches(COMMENT_TEXT))
						break;
					line--;
				}
				if (!editor.getLineText(line + 1).contains("/*")) {
					insertNewLine();
					return;
				}
			}
			int commentStart = lineText.indexOf("/*");
			int commentStop = (lineText.contains("*/") ? lineText.indexOf("*/") : lineText.length()) + 2;

			if (caretPos > commentStart && caretPos < commentStop) {
				splitComment(caretLine);
				return;
			}
		}

		if (lineText.matches(BLOCK_OPENING)) {
			boolean curlyBracesAreBalanced = EditorUtil.checkBracketsBalance(editor.getText(), "{", "}");

			if (!curlyBracesAreBalanced && caretPos >= lineText.indexOf("{")) {
				createBlockScope(caretLine);
				return;
			}
		}

		// if none of the above, then insert a new line
		insertNewLine();
	}

	private void splitString(int caretLine) {
		int indent = EditorUtil.getLineIndentation(caretLine);
		if (!editor.getLineText(caretLine).matches(SPLIT_STRING_TEXT))
			indent += TAB_SIZE;

		editor.stopCompoundEdit();
		editor.insertText("\"\n" + EditorUtil.addSpaces(indent) + "+ \"");
		editor.stopCompoundEdit();
	}

	private void splitComment(int caretLine) {
		int indent = EditorUtil.getLineIndentation(caretLine);

		editor.startCompoundEdit();
		editor.insertText(NL + EditorUtil.addSpaces(indent - (indent % TAB_SIZE)) + " * ");

		int caretPos = editor.getCaretOffset();
		String nextText = editor.getText().substring(caretPos);

		// Checking if we need to close this comment
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

	private void createBlockScope(int caretLine) {
		int indent = EditorUtil.getLineIndentation(caretLine) + TAB_SIZE;
		
		editor.startCompoundEdit();
		editor.setSelection(editor.getCaretOffset(), editor.getLineStopOffset(caretLine) - 1);

		String cutText = editor.isSelectionActive() ? editor.getSelectedText().trim() : "";		
		editor.setSelectedText("\n" + EditorUtil.addSpaces(indent) + cutText);
		
		int newCaret = editor.getCaretOffset();
		editor.insertText("\n" + EditorUtil.addSpaces(indent - TAB_SIZE) + '}');
		editor.setSelection(newCaret, newCaret);
		editor.stopCompoundEdit();
	}

	private void insertNewLine() {
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
				// System.out.println("1");
				int selectionStart = editor.getSelectionStart();

				if (selectionStart - TAB_SIZE >= 0) {
					// System.out.println("2");
					editor.setSelection(selectionStart - TAB_SIZE, selectionStart);

					// if these are spaces that we can delete
					if (editor.getSelectedText().equals(TAB)) {
						// System.out.println("3");
						editor.setSelectedText("");
					} else {
						// System.out.println("4");
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

	private void expandSelection() {
		final char OPEN_BRACE = '{';
		final char CLOSE_BRACE = '}';

		int start = editor.getSelectionStart();
		int end = editor.getSelectionStop();

		Selection s = new Selection(editor);

		int startLine = s.getStartLine();
		int endLine = s.getEndLine();

		if (editor.isSelectionActive()) {
			String code = editor.getText();

			int lastLineOfSelection = editor.getTextArea().getSelectionStopLine();
			boolean isLastBlock = (editor.getSelectionStop() == editor.getLineStartOffset(lastLineOfSelection));

			if (isLastBlock) {
				end = editor.getLineStopOffset(lastLineOfSelection) - 1;
				editor.setSelection(s.getStart(), end);
				return;

			} else if (code.charAt(start - 1) == OPEN_BRACE && code.charAt(end) == CLOSE_BRACE) {
				editor.setSelection(s.getStart(), s.getEnd());
				return;

			} else if (start == s.getStart() && end == s.getEnd()) {
				startLine--;
				endLine++;

			}
		}

		// go up and search for the corresponding open brace
		int brace = EditorUtil.getMatchingBraceLine(startLine, true);

		// open brace not found
		if (brace == -1) {
			return;
		}

		int lineEnd = editor.getLineStopOffset(brace) - 1;
		start = EditorUtil.getOffsetOfPrevious(OPEN_BRACE, lineEnd) + 1;

		// now go down and search for the corresponding close brace
		brace = EditorUtil.getMatchingBraceLine(endLine, false);

		// close brace not found
		if (brace == -1) {
			return;
		}

		lineEnd = editor.getLineStopOffset(brace) - 1;
		end = EditorUtil.getOffsetOfPrevious(CLOSE_BRACE, lineEnd);

		editor.setSelection(start, end);
	}

	private void formatSelectedText() {
		if (editor.isSelectionActive()) {

			if (editor.getSelectedText().isBlank()) {
				return;
			}

			Selection s = new Selection(editor);

			String selectedText;

			// long string literals are formatted here
			if (Preferences.getBoolean("code_assistant.autoformat.strings")) {
				selectedText = refactorStringLiterals(s.getText());
			} else {
				selectedText = s.getText();
			}

			// and everything else is formatted here
			String formattedText = editor.createFormatter().format(selectedText);

			// but they need to be indented, anyway...
			int brace = EditorUtil.getMatchingBraceLine(s.getStartLine() - 1, true);
			int indent = 0;

			if (brace != -1) {
				indent = EditorUtil.getLineIndentation(brace) + TAB_SIZE;
			}

			formattedText = EditorUtil.indentText(formattedText, indent);

			if (formattedText.equals(selectedText)) {
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

	private String refactorStringLiterals(String text) {
		int maxLength = Preferences.getInteger("code_assistant.autoformat.line_length");

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

	private static void println(Object... objects) {
		for (Object o : objects) {
			System.out.println(o.toString());
		}
	}
}