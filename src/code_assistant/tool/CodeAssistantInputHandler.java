package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import processing.app.Platform;
import processing.app.syntax.PdeInputHandler;
import processing.app.ui.Editor;

public class CodeAssistantInputHandler extends PdeInputHandler {
	protected List<KeyHandler> keyHandlers = new ArrayList<>();

	// CONSTRUCTOR
	public CodeAssistantInputHandler(Editor editor, KeyHandler... handlers) {
		super(editor);

		DefaultInputs.init(editor);
		addKeyBinding("AS+UP", DefaultInputs.DUPLICATE_UP);
		addKeyBinding("AS+DOWN", DefaultInputs.DUPLICATE_DOWN);
		addKeyBinding("A+UP", DefaultInputs.MOVE_UP);
		addKeyBinding("A+DOWN", DefaultInputs.MOVE_DOWN);
		addKeyBinding("TAB", DefaultInputs.INDENT_TEXT);
		addKeyBinding("S+TAB", DefaultInputs.OUTDENT_TEXT);
		addKeyBinding("A+ENTER", DefaultInputs.INSERT_NEW_LINE_BELLOW);
		addKeyBinding("C+E", DefaultInputs.DELETE_LINE);
		addKeyBinding(editor, "CS+E", "delete-line-content", DefaultInputs.DELETE_LINE_CONTENT);
		
		for (int i = 0; i < handlers.length; i++) {
			keyHandlers.add(handlers[i]);
		}
		
		for (Map.Entry<String, AbstractAction> actionMap : KeyHandler.getActions().entrySet()) {
			String keyBinding = actionMap.getKey();
			AbstractAction action = actionMap.getValue();
			addKeyBinding(keyBinding, action);
		}
	}

	static public void addKeyBinding(Editor editor, String keyBinding, String actionName, AbstractAction action) {
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

		for (KeyHandler handler : keyHandlers) {
			if (handler.handlePressed(e)) {
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