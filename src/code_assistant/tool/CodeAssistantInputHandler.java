package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import processing.app.Platform;
import processing.app.syntax.PdeInputHandler;
import processing.app.ui.Editor;

public class CodeAssistantInputHandler extends PdeInputHandler {
	protected List<KeyHandler> keyHandlers = new ArrayList<>();
	

	// CONSTRUCTOR
	public CodeAssistantInputHandler(Editor editor, KeyHandler ... handlers) {
		super(editor);
		
		for (KeyHandler handler : handlers) {
			keyHandlers.add(handler);
		}

		ToolEditor.init(editor);

		addKeyBinding("AS+UP", ToolEditor.DUPLICATE_UP);
		addKeyBinding("AS+DOWN", ToolEditor.DUPLICATE_DOWN);
		addKeyBinding("A+UP", ToolEditor.MOVE_UP);
		addKeyBinding("A+DOWN", ToolEditor.MOVE_DOWN);
		addKeyBinding("TAB", ToolEditor.INDENT_TEXT);
		addKeyBinding("S+TAB", ToolEditor.OUTDENT_TEXT);
		addKeyBinding("A+ENTER", ToolEditor.INSERT_NEW_LINE_BELLOW);
		addKeyBinding("C+E", ToolEditor.DELETE_LINE);
		addKeyBinding("CS+E", "delete-line-content", ToolEditor.DELETE_LINE_CONTENT);

		addKeyBinding("ENTER", JavaModeInputs.HANDLE_ENTER);
		addKeyBinding("CA+RIGHT", JavaModeInputs.SELECT_BLOCK);
		addKeyBinding("C+T", "format-selected-text", JavaModeInputs.FORMAT_SELECTED_TEXT);
	}

	public void addKeyBinding(String keyBinding, String actionName, AbstractAction action) {
		KeyStroke ks = parseKeyStroke(keyBinding);
		editor.getTextArea().getInputMap().put(ks, actionName);
		editor.getTextArea().getActionMap().put(actionName, action);
	}

	@Override
	protected boolean isMnemonic(KeyEvent e) {
		if (!Platform.isMacOS()) {
			if (e.isAltDown() && Character.isLetter(e.getKeyChar())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean handlePressed(KeyEvent e) {
		if (e.isMetaDown())
			return false;

		for (KeyHandler keyHandler : keyHandlers) {
			if (keyHandler.handlePressed(e)) {
				handleInputMethodCommit();
				e.consume();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean handleTyped(KeyEvent e) {
		char keyChar = e.getKeyChar();

		if (e.isControlDown()) {
			// on linux, ctrl-comma (prefs) being passed through to the editor
			if (keyChar == KeyEvent.VK_COMMA) {
				e.consume();
				return true;
			}
			if (keyChar == KeyEvent.VK_SPACE) {
				e.consume();
				return true;
			}
		}
		return false;
	}
}