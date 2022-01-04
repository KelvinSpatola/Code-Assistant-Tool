package code_assistant.tool;

import static code_assistant.util.Constants.BLOCK_CLOSING;
import static code_assistant.util.Constants.BLOCK_OPENING;
import static code_assistant.util.Constants.NL;
import static code_assistant.util.Constants.TAB;
import static code_assistant.util.Constants.TAB_SIZE;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import code_assistant.completion.TemplatesManager;
import code_assistant.util.EditorUtil;
import code_assistant.util.Selection;
import processing.app.Preferences;
import processing.app.ui.Editor;

public class DefaultInputs implements ActionTrigger {
    protected Map<String, Action> actions = new HashMap<>();
    protected Editor editor;

    public DefaultInputs(Editor editor) {
        this.editor = editor;

        actions.put("AS+UP", DUPLICATE_UP);
        actions.put("AS+DOWN", DUPLICATE_DOWN);
        actions.put("A+UP", MOVE_UP);
        actions.put("A+DOWN", MOVE_DOWN);
        actions.put("TAB", INDENT_TEXT);
        actions.put("S+TAB", OUTDENT_TEXT);
        actions.put("A+ENTER", INSERT_NEW_LINE_BELLOW);
        actions.put("C+E", DELETE_LINE);
        actions.put("CS+E", DELETE_LINE_CONTENT);
        actions.put("CS+U", TO_UPPER_CASE);
        actions.put("CS+L", TO_LOWER_CASE);
    }

    @Override
    public Map<String, Action> getActions() {
        return actions;
    }

    private final Action DELETE_LINE = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            deleteLine(editor.getTextArea().getCaretLine());
        }
    };

    private final Action DELETE_LINE_CONTENT = new AbstractAction("delete-line-content") {
        @Override
        public void actionPerformed(ActionEvent e) {
            deleteLineContent(editor.getTextArea().getCaretLine());
        }
    };

    private final Action DUPLICATE_UP = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            duplicateLines(true);
        }
    };

    private final Action DUPLICATE_DOWN = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            duplicateLines(false);
        }
    };

    private final Action MOVE_UP = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveLines(true);
        }
    };

    private final Action MOVE_DOWN = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            moveLines(false);
        }
    };

    private final Action INSERT_NEW_LINE_BELLOW = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            insertNewLineBellow(editor.getCaretOffset());
        }
    };

    private final Action INDENT_TEXT = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (TemplatesManager.isReadingKeyboardInput())
                return;

            handleTabulation(false);
        }
    };

    private final Action OUTDENT_TEXT = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (TemplatesManager.isReadingKeyboardInput())
                return;

            handleTabulation(true);
        }
    };

    private final Action TO_UPPER_CASE = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            changeCase(true);
        }
    };

    private final Action TO_LOWER_CASE = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            changeCase(false);
        }
    };

    /*
     * ******** METHODS ********
     */

    private void duplicateLines(boolean up) {
        Selection s = new Selection(editor);

        if (s.getEndLine() == editor.getLineCount() - 1) {
            int caret = editor.getLineStopOffset(s.getEndLine());
            editor.setSelection(caret, caret);
            editor.insertText(NL + s.getText());

        } else {
            int caret = s.getEnd() + 1;
            editor.setSelection(caret, caret);
            editor.insertText(s.getText() + NL);
        }

        if (up)
            editor.setSelection(s.getEnd(), s.getStart());
        else
            editor.setSelection(editor.getCaretOffset() - 1, s.getEnd() + 1);
    }

    private void deleteLine(int line) {
        // in case we are in the last line of text (but not when it's also first one)
        if (line == editor.getLineCount() - 1 && line != 0) {
            // subtracting 1 from getLineStartOffset() will delete the line break prior
            // to this line, causing the caret to move to the end of the previous line
            int start = editor.getLineStartOffset(line) - 1;
            int end = editor.getLineStopOffset(line) - 1;

            editor.setSelection(start, end);
            editor.setSelectedText("");

        } else if (editor.getLineCount() > 1) {
            editor.setLineText(line, "");

        } else { // in case we are deleting the only line that remains
            if (editor.getLineText(line).isEmpty()) {
                editor.getToolkit().beep();
                return;
            }
            editor.setText("");
        }
    }

    private void deleteLineContent(int line) {
        int start = editor.getTextArea().getLineStartNonWhiteSpaceOffset(line);
        int end = editor.getLineStopOffset(line) - 1;

        editor.setSelection(start, end);
        editor.setSelectedText("");
    }

    private void moveLines(boolean moveUp) {
        Selection s = new Selection(editor);

        int targetLine = moveUp ? s.getStartLine() - 1 : s.getEndLine() + 1;

        if (targetLine < 0 || targetLine >= editor.getLineCount()) {
            editor.getToolkit().beep();
            return;
        }

        int target_start = editor.getLineStartOffset(targetLine);
        int target_end = editor.getLineStopOffset(targetLine) - 1;

        String selectedText = s.getText();
        String replacedText = editor.getText(target_start, target_end);

        editor.startCompoundEdit();
        editor.setSelection(s.getStart(), s.getEnd());

        int newSelectionStart, newSelectionEnd;

        // SWAP LINES
        if (moveUp) {
            editor.setSelection(target_start, s.getEnd());
            editor.setSelectedText(selectedText + NL + replacedText);

            newSelectionStart = editor.getLineStartOffset(targetLine);
            newSelectionEnd = editor.getLineStopOffset(s.getEndLine() - 1) - 1;

        } else {
            editor.setSelection(s.getStart(), target_end);
            editor.setSelectedText(replacedText + NL + selectedText);

            newSelectionStart = editor.getLineStartOffset(s.getStartLine() + 1);
            newSelectionEnd = editor.getLineStopOffset(targetLine) - 1;
        }

        // UPDATE SELECTION
        editor.setSelection(newSelectionStart, newSelectionEnd);

        // RESOLVE INDENTATION
        if (!Preferences.getBoolean("code_assistant.move_lines.auto_indent")) {
            editor.stopCompoundEdit();
            return;
        }

        s = new Selection(editor);

        int line = s.getStartLine();
        String lineText = editor.getLineText(line);

        int blockIndent = 0;
        int brace = EditorUtil.getMatchingBraceLineAlt(line);

        if (brace != -1) { // we are inside a block here
            if (lineText.matches(BLOCK_OPENING)) {
                brace = EditorUtil.getMatchingBraceLineAlt(line);
                blockIndent = EditorUtil.getLineIndentation(brace) + TAB_SIZE;

            } else if (lineText.matches(BLOCK_CLOSING)) {
                brace = EditorUtil.getMatchingBraceLine(line, true);
                blockIndent = EditorUtil.getLineIndentation(brace);

            } else {
                blockIndent = EditorUtil.getLineIndentation(brace) + TAB_SIZE;
            }
        }

        int selectionIndent = EditorUtil.getLineIndentation(lineText);

        if (selectionIndent < blockIndent)
            editor.handleIndent();

        else if (selectionIndent > blockIndent)
            editor.handleOutdent();

        editor.stopCompoundEdit();
    }

    private void insertNewLineBellow(int offset) {
        int line = editor.getTextArea().getLineOfOffset(offset);
        String lineText = editor.getLineText(line);

        int indent = 0;
        int caretPos = EditorUtil.caretPositionInsideLine();

        if (Preferences.getBoolean("editor.indent")) {

            if (lineText.matches(BLOCK_OPENING)) {
                indent = EditorUtil.getLineIndentation(line);

                if (caretPos > lineText.indexOf('{'))
                    indent += TAB_SIZE;

            } else if (lineText.matches(BLOCK_CLOSING)) {
                indent = EditorUtil.getLineIndentation(line);
                int closeBrace = lineText.indexOf('}');

                if (caretPos <= closeBrace) {
                    offset += (closeBrace - caretPos);
                    editor.setSelection(offset, offset);
                    editor.insertText(TAB);
                    offset += TAB_SIZE;
                }

            } else {
                int startBrace = EditorUtil.getMatchingBraceLine(line, true);

                if (startBrace != -1) // an opening brace was found, we are in a block scope
                    indent = EditorUtil.getLineIndentation(startBrace) + TAB_SIZE;
            }
        }

        editor.startCompoundEdit();
        editor.insertText(NL + EditorUtil.addSpaces(indent));
        editor.setSelection(offset, offset);
        editor.stopCompoundEdit();
    }

    private void handleTabulation(boolean isShiftDown) {
        if (isShiftDown) {
            editor.handleOutdent();

        } else if (editor.isSelectionActive()) {
            editor.handleIndent();

        } else if (Preferences.getBoolean("editor.tabs.expand")) {
            // "editor.tabs.expand" means that each tab is made up of a
            // stipulated number of spaces, and not just a single solid \t
            editor.setSelectedText(TAB);

        } else {
            editor.setSelectedText("\t");
        }
    }

    private void changeCase(boolean toUpperCase) {
        if (editor.isSelectionActive()) {
            int start = editor.getSelectionStart();
            int end = editor.getSelectionStop();

            if (toUpperCase) {
                editor.setSelectedText(editor.getSelectedText().toUpperCase());
            } else {
                editor.setSelectedText(editor.getSelectedText().toLowerCase());
            }
            editor.setSelection(start, end);
        }
    }

}