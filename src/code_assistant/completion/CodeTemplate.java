package code_assistant.completion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodeTemplate {
	static private final Set<String> placeholders = new HashSet<>();
	protected String[] sourceLines;
	protected StringBuilder buffer;
	protected int caretLine;
	
	private int indent;
	private String sourceText;
	private List<Integer> caretPositions;
	private final char LF = '\n';

	static {
		placeholders.add("$");
	}

	// CONSTRUCTOR
	public CodeTemplate(String source) {
		buffer = new StringBuilder();
		caretPositions = new ArrayList<>();
		processSourceText(source);
	}

	protected void processSourceText(String source) {
		int index = 0;

		while (index < source.length()) {
			char ch = source.charAt(index);

			if (placeholders.contains(String.valueOf(ch))) {
				caretPositions.add(index);
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

	public int getCaretPosition(int currentOffset) {
		int caret = caretPositions.get(0);
		return currentOffset - buffer.length() + caret + (indent * calcLine(caret));
	}
	
	protected void addPlaceholder(String tag) {
		placeholders.add(tag);
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
}
