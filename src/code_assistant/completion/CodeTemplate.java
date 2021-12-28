package code_assistant.completion;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import processing.app.syntax.JEditTextArea;
import processing.app.ui.Editor;

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

	static {
		placeholders.add("$");
		placeholders.add("#");
	}

	// CONSTRUCTOR
	public CodeTemplate(String source) {
		buffer = new StringBuilder();
		caretPositions = new ArrayList<>();
		processSourceText(source);
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

	private String removePlaceholders(String source) {
		for (String ph : placeholders) {
			source = source.replace(ph, "");
		}
		return source;
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

	public String getCode() {
		return getCode(indent);
	}

	public String getCode(int indent) {
		if (indent != this.indent && indent >= 0) {
			setIndentation(indent);
		}
		return buffer.toString();
	}

	int startingPosition;
	public int leftBoundary, rightBoundary;

	public int getStartCaretPosition(int currentOffset) {
		positionIndex = 0;
		isLastCandidate = false;

		for (CaretPosition c : caretPositions) {
			c.reset();
		}

		int caret = caretPositions.get(positionIndex).currentOffset;

		startingPosition = currentOffset - buffer.length();
		// println("startingPosition: " + startingPosition);

		leftBoundary = startingPosition + caret + (indent * calcLine(caret));
		rightBoundary = leftBoundary + 1;

		// println("leftBoundary: " + leftBoundary + " - rightBoundary: " +
		// rightBoundary);
		return leftBoundary;
	}

	protected void addPlaceholder(String tag) {
		placeholders.add(tag);
	}

	public boolean isLastCandidate() {
		return isLastCandidate;
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

			println("last");
		}

		rightBoundary = leftBoundary + 1;
		return leftBoundary;
	}

	public void readInput(KeyEvent e, Editor editor) {
		int key = e.getKeyChar();

		// won't do anything if this is a not printable character
		if (key == KeyEvent.VK_BACK_SPACE || key >= 32 && key < 127) {
			if (key == KeyEvent.VK_BACK_SPACE) {
				caretPositions.get(positionIndex).currentOffset--;
				rightBoundary--;

			} else {
				caretPositions.get(positionIndex).currentOffset++;
				rightBoundary++;
			}
			
			textarea = editor.getTextArea();
			gfx = textarea.getPainter().getGraphics();

			int line = textarea.getCaretLine();
			int selectStart = leftBoundary - textarea.getLineStartOffset(line);
			int selectEnd = rightBoundary - textarea.getLineStartOffset(line);
			
			int x = textarea._offsetToX(line, selectStart);
			int y = textarea.lineToY(line);
			int w = textarea._offsetToX(line, selectEnd - 1) - x;
			int h = textarea.getPainter().getLineHeight() + 5;

			//textarea.getPainter().setBackground(new Color(120, 120, 0));
			
			gfx.setColor(new Color(0, 255, 255));
			gfx.setClip(x, y, w, h);
//			textarea.paintImmediately(x, y, w, h);
			
			//textarea.getPainter().invalidateLine(line);
			//textarea.getPainter().setLineHighlightEnabled(false);
			//textarea.getPainter().repaint(x, y, w, h);
			//editor.getTextArea().paint(gfx);
			
			gfx.drawRect(x, y, w, h);
			gfx.fillRect(x, y, w, h);
		}

	}
	
	JEditTextArea textarea;
	Graphics gfx;

	public boolean contains(int caret) {
		return (caret >= leftBoundary && caret < rightBoundary);
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

	class CaretPosition {
		int currentOffset, startOffset;
		boolean isStopPosition;

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
