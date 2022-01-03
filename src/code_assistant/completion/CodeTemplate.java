package code_assistant.completion;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import processing.app.Preferences;

public class CodeTemplate {
    static private final Set<String> placeholders = new HashSet<>();
    static private final char LF = '\n';
    private String[] sourceLines;
    private StringBuilder buffer;
    private String sourceText;
    private int indent;

    private List<CaretPosition> caretPositions;
    private int positionIndex;
    private boolean isLastCandidate;

    private int startingPosition;
    private int leftBoundary, rightBoundary;

    static {
        placeholders.add("$"); // end caret
        placeholders.add("#"); // paramters
    }

    // CONSTRUCTOR
    public CodeTemplate(String source) {
        buffer = new StringBuilder();
        caretPositions = new ArrayList<>();
        processSourceText(source);
    }

    public String getCode() {
        return getCode(indent);
    }

    public String getCode(int indent) {
        if (indent != this.indent && indent >= 0) {
            setIndentation(indent);
        }
        return buffer.toString();
    }

    public final CodeTemplate setIndentation(int indent) {
        this.indent = indent;

        String spaces = new String(new char[indent]).replace('\0', ' ');
        buffer = new StringBuilder();

        for (String line : sourceLines) {
            buffer.append(spaces).append(line).append(LF);
        }
        buffer.deleteCharAt(buffer.length() - 1);

        return this;
    }

    public int getStartCaretPosition(int currentOffset) {
        positionIndex = 0;
        isLastCandidate = false;

        for (CaretPosition c : caretPositions) {
            c.reset();
        }

        int caret = caretPositions.get(positionIndex).currentOffset;

        startingPosition = currentOffset - buffer.length();

        leftBoundary = startingPosition + caret + (indent * calcLine(caret));
        rightBoundary = leftBoundary + 1;

        return leftBoundary;
    }

    public int getNextPosition() {
        int caret = 0, delta = 0;
        positionIndex++;

        for (int i = 0; i < positionIndex; i++) {
            delta += caretPositions.get(i).delta();
        }

        if (positionIndex < caretPositions.size()) {
            caret = caretPositions.get(positionIndex).startOffset;
            leftBoundary = startingPosition + caret + (indent * calcLine(caret)) + delta;

        } else {
            int last = caretPositions.size() - 1;
            caret = caretPositions.get(last).startOffset;
            leftBoundary = startingPosition + caret + (indent * calcLine(caret)) + delta + 1;
            isLastCandidate = true;
        }

        rightBoundary = leftBoundary + 1;
        return leftBoundary;
    }

    public void readInput(KeyEvent e) {
        int key = e.getKeyChar();

        // won't do anything if this is a not printable character (except backspace and
        // delete)
        if (key == 8 || key >= 32 && key <= 127) { // 8 -> VK_BACK_SPACE

            if (key == KeyEvent.VK_BACK_SPACE || key == KeyEvent.VK_DELETE) {
                caretPositions.get(positionIndex).currentOffset--;
                rightBoundary--;
                return;
            }

            if (Preferences.getBoolean("code_assistant.bracket_closing.enabled")) {
                if (isOpeningBracket(e.getKeyChar())) { // ( [ { \" \'
                    caretPositions.get(positionIndex).currentOffset += 2;
                    rightBoundary += 2;
                    return;
                }
            }

            caretPositions.get(positionIndex).currentOffset++;
            rightBoundary++;
        }
    }

    boolean skip;

    public boolean contains(int caret) {
//		println("caret: " + caret + " - rightBoundary: " + rightBoundary);
//		skip = caret == rightBoundary;
//		return ((caret >= leftBoundary && caret < rightBoundary) || skip);
//		^^ same as:
        return (caret >= leftBoundary && caret <= rightBoundary);
    }

    public boolean isLastCandidate() {
        return isLastCandidate;
    }

    protected void processSourceText(String source) {
        int index = 0, indexSubstring = 0;

        while (index < source.length()) {
            char ch = source.charAt(index);

            if (placeholders.contains(String.valueOf(ch))) {
                caretPositions.add(new CaretPosition(index - indexSubstring));
                indexSubstring++;
            }
            index++;
        }

        sourceText = removePlaceholders(source);
        sourceLines = sourceText.split("\n");
        setIndentation(0);
    }

    protected String removePlaceholders(String source) {
        for (String ph : placeholders) {
            source = source.replace(ph, "");
        }
        return source;
    }

    protected void addPlaceholder(String tag) { // do we really need this?
        placeholders.add(tag);
    }

    protected boolean isOpeningBracket(char ch) {
        String tokens = "([{\"\'";
        return tokens.contains(String.valueOf(ch));
    }

    private int calcLine(int offset) {
        int line = 1, index = 0;

        while (index < offset) {
            if (sourceText.charAt(index) == LF) {
                line++;
            }
            index++;
        }
        return line;
    }

    static private void println(Object... what) {
        for (Object s : what) {
            System.out.println(s.toString());
        }
    }

    static class CaretPosition {
        int currentOffset, startOffset;
        boolean isStopPosition; // TODO: gotta work this idea

        CaretPosition(int currentOffset) {
            this.currentOffset = currentOffset;
            startOffset = currentOffset;
        }

        int delta() {
            return currentOffset - startOffset;
        }

        void reset() {
            currentOffset = startOffset;
            isStopPosition = false;
        }
    }
}
