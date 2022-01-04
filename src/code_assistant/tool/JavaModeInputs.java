package code_assistant.tool;

import static code_assistant.util.Constants.BLOCK_CLOSING;
import static code_assistant.util.Constants.BLOCK_OPENING;
import static code_assistant.util.Constants.NL;
import static code_assistant.util.Constants.TAB;
import static code_assistant.util.Constants.TAB_SIZE;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import code_assistant.completion.CodeTemplatesManager;
import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.Language;
import processing.app.Preferences;
import processing.app.ui.Editor;

public class JavaModeInputs implements ActionTrigger, KeyHandler {
    protected static final String COMMENT_TEXT = "^(?!.*\\\".*\\/\\*.*\\\")(?:.*\\/\\*.*|\\h*\\*.*)";
    protected static final String STRING_TEXT = "^(?!(.*?(\\*|\\/+).*?\\\".*\\\")).*(?:\\\".*){2}";
    protected static final String SPLIT_STRING_TEXT = "^\\h*\\+\\s*(?:\\\".*){2}";
    protected static final String OPEN_COMMENT = "/*";
    protected static final String CLOSE_COMMENT = "*/";
    protected static final char OPEN_BRACE = '{';
    protected static final char CLOSE_BRACE = '}';

    protected Map<String, Action> actions = new HashMap<>();
    protected Editor editor;

    public JavaModeInputs(Editor editor) {
        this.editor = editor;
        EditorUtil.init(editor);

        actions.put("ENTER", HANDLE_ENTER);
        actions.put("CA+RIGHT", EXPAND_SELECTION);
        actions.put("C+T", FORMAT_SELECTED_TEXT);
        actions.put("C+7", TOGGLE_BLOCK_COMMENT);
    }

    @Override // from the ActionTrigger interface
    public Map<String, Action> getActions() {
        return actions;
    }

    /*
     * ******** KEY HANDLER ********
     */

    @Override // from the KeyHandler interface
    public boolean handlePressed(KeyEvent e) {
        if (e.getKeyChar() == CLOSE_BRACE) {
            editor.startCompoundEdit();

            // erase any selection content
            if (editor.isSelectionActive()) {
                editor.setSelectedText("");
            }

            int indent = 0;

            if (Preferences.getBoolean("editor.indent")) {
                int line = editor.getTextArea().getCaretLine();

                if (editor.getLineText(line).isBlank()) {
                    int startBrace = EditorUtil.getMatchingBraceLine(line, true);

                    if (startBrace != -1)
                        indent = EditorUtil.getLineIndentation(startBrace);

                    editor.setSelection(editor.getLineStartOffset(line), editor.getCaretOffset());
                }
            }

            String result = EditorUtil.addSpaces(indent);

            // if the user chooses to disable the bracket closing feature in the
            // Preferences.txt file, we should then insert a closing brace here.
            // Otherwise this is handled by the BracketCloser class.
            if (!Preferences.getBoolean("code_assistant.bracket_closing.enabled"))
                result += CLOSE_BRACE;

            editor.setSelectedText(result);
            editor.stopCompoundEdit();
        }

        return false;
    }

    @Override // from the KeyHandler interface
    public boolean handleTyped(KeyEvent e) {
        return (e.getKeyChar() == CLOSE_BRACE);
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
            if (CodeTemplatesManager.isReadingKeyboardInput())
                return;

            handleEnter();
        }
    };

    private final Action TOGGLE_BLOCK_COMMENT = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            toggleBlockComment();
        }
    };

    /*
     * ******** METHODS ********
     */

    private void handleEnter() {
        int caret = editor.getCaretOffset();

        if (!editor.isSelectionActive()) {

            int positionInLine = EditorUtil.getPositionInsideLineWithOffset(caret);
            int caretLine = editor.getTextArea().getCaretLine();
            String lineText = editor.getLineText(caretLine);

            if (lineText.matches(STRING_TEXT)) {
                int stringStart = lineText.indexOf("\"");
                int stringStop = lineText.lastIndexOf("\"") + 1;

                if (positionInLine > stringStart && positionInLine < stringStop) {
                    splitString(caretLine);
                    return;
                }
            }

            if (lineText.matches(COMMENT_TEXT)) {
                if (!lineText.contains(OPEN_COMMENT)) {
                    int line = caretLine - 1;

                    while (line >= 0) {
                        if (!editor.getLineText(line).matches(COMMENT_TEXT))
                            break;
                        line--;
                    }
                    if (!editor.getLineText(line + 1).contains(OPEN_COMMENT)) {
                        insertNewLine(caret);
                        return;
                    }
                }
                int commentStart = lineText.indexOf(OPEN_COMMENT);
                int commentStop = (lineText.contains(CLOSE_COMMENT) ? lineText.indexOf(CLOSE_COMMENT)
                        : lineText.length()) + 2;

                if (positionInLine > commentStart && positionInLine < commentStop) {
                    splitComment(caretLine);
                    return;
                }
            }

            if (lineText.matches(BLOCK_OPENING)) {
                if (Preferences.getBoolean("code_assistant.bracket_closing.enabled")) {

                    boolean bracketsAreBalanced = EditorUtil.checkBracketsBalance(editor.getText(), "{", "}");
                    boolean hasClosingBrace = lineText.matches(BLOCK_CLOSING);
                    int openBrace = lineText.indexOf(OPEN_BRACE);
                    int closeBrace = lineText.indexOf(CLOSE_BRACE);

                    if ((!bracketsAreBalanced && positionInLine > openBrace) || (bracketsAreBalanced && hasClosingBrace
                            && positionInLine > openBrace && positionInLine <= closeBrace)) {

                        createBlockScope(caret);
                        return;
                    }
                }
            }
        }
        // if none of the above, then insert a new line
        insertNewLine(caret);
    }

    private void splitString(int caretLine) {
        int indent = 0;
        if (Preferences.getBoolean("editor.indent")) {
            indent = EditorUtil.getLineIndentation(caretLine);

            if (!editor.getLineText(caretLine).matches(SPLIT_STRING_TEXT))
                indent += TAB_SIZE;
        }

        editor.stopCompoundEdit();
        editor.insertText("\"\n" + EditorUtil.addSpaces(indent) + "+ \"");
        editor.stopCompoundEdit();
    }

    private void splitComment(int caretLine) {
        int indent = 0;
        if (Preferences.getBoolean("editor.indent")) {
            indent = EditorUtil.getLineIndentation(caretLine);
        }

        editor.startCompoundEdit();
        editor.insertText(NL + EditorUtil.addSpaces(indent - (indent % TAB_SIZE)) + " * ");

        int caretPos = editor.getCaretOffset();
        String nextText = editor.getText().substring(caretPos);

        // Checking if we need to close this comment
        int openingToken = nextText.indexOf(OPEN_COMMENT);
        int closingToken = nextText.indexOf(CLOSE_COMMENT);
        boolean commentIsOpen = (closingToken == -1) || (closingToken > openingToken && openingToken != -1);

        if (commentIsOpen) {
            editor.getTextArea().setCaretPosition(editor.getLineStopOffset(++caretLine) - 1);
            editor.insertText(NL + EditorUtil.addSpaces(indent - (indent % TAB_SIZE)) + " */");
            editor.getTextArea().setCaretPosition(caretPos);
        }
        editor.stopCompoundEdit();
    }

    private void createBlockScope(int offset) {
        int line = editor.getTextArea().getLineOfOffset(offset);

        int indent = 0;
        if (Preferences.getBoolean("editor.indent")) {
            indent = EditorUtil.getLineIndentation(line) + TAB_SIZE;
        }

        editor.startCompoundEdit();
        editor.setSelection(offset, editor.getLineStopOffset(line) - 1);

        String cutText = editor.isSelectionActive() ? editor.getSelectedText().trim() : "";

        if (cutText.matches(BLOCK_CLOSING)) {
            cutText = cutText.replace(CLOSE_BRACE, '\0').trim();
        }

        editor.setSelectedText(NL + EditorUtil.addSpaces(indent) + cutText);

        int newOffset = editor.getCaretOffset();
        editor.insertText(NL + EditorUtil.addSpaces(indent - TAB_SIZE) + CLOSE_BRACE);
        editor.setSelection(newOffset, newOffset);
        editor.stopCompoundEdit();
    }

    private void insertNewLine(int offset) {
        int indent = 0;
        editor.startCompoundEdit();

        // erase any selection content
        if (editor.isSelectionActive()) {
            offset = editor.getSelectionStart() + 1;
            editor.setSelectedText("");
            editor.setSelectedText(NL);

            editor.setSelection(offset, offset);
            editor.stopCompoundEdit();

        }
        if (Preferences.getBoolean("editor.indent")) {
            int line = editor.getTextArea().getLineOfOffset(offset);
            String lineText = editor.getLineText(line);

            int startBrace = EditorUtil.getMatchingBraceLine(line, true);

            if (startBrace != -1) {
                indent = EditorUtil.getLineIndentation(startBrace);

                if (!lineText.matches(BLOCK_CLOSING))
                    indent += TAB_SIZE;

                int positionInLine = EditorUtil.getPositionInsideLineWithOffset(offset);

                if (lineText.matches(BLOCK_OPENING) && positionInLine <= lineText.indexOf(OPEN_BRACE))
                    indent -= TAB_SIZE;
            }
            editor.setSelection(offset, editor.getLineStopOffset(line) - 1);
        }
        String cutText = editor.isSelectionActive() ? editor.getSelectedText().trim() : "";
        editor.setSelectedText(NL + EditorUtil.addSpaces(indent) + cutText);

        int newOffset = offset + indent + 1;
        editor.setSelection(newOffset, newOffset);
        editor.stopCompoundEdit();
    }

    private void expandSelection() {
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

            }
            if (code.charAt(start - 1) == OPEN_BRACE && code.charAt(end) == CLOSE_BRACE) {
                editor.setSelection(s.getStart(), s.getEnd());
                return;

            }
            if (start == s.getStart() && end == s.getEnd()) {
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
            boolean isSourceIntact = true;

            // long string literals are formatted here
            if (Preferences.getBoolean("code_assistant.autoformat.strings")) {
                selectedText = refactorStringLiterals(s.getText());
                isSourceIntact = selectedText.stripTrailing().equals(s.getText());
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

            if (formattedText.equals(selectedText) && isSourceIntact) {
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

    private void toggleBlockComment() {
        if (editor.isSelectionActive()) {
            String selectedText = editor.getSelectedText();
            StringBuilder result = new StringBuilder();

            if (selectedText.startsWith(OPEN_COMMENT) && selectedText.endsWith(CLOSE_COMMENT)) {
                result.append(selectedText);
                result.delete(0, 3);
                result.delete(result.length() - 3, result.length());

            } else {
                result.append(OPEN_COMMENT).append(" " + selectedText + " ").append(CLOSE_COMMENT);
            }

            int selectionStart = editor.getSelectionStart();
            int selectionEnd = selectionStart + result.length();

            editor.startCompoundEdit();
            editor.setSelectedText(result.toString());
            editor.setSelection(selectionStart, selectionEnd);
            editor.stopCompoundEdit();
        }
    }
}