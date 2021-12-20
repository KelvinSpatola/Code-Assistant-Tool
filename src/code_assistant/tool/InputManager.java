package code_assistant.tool;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import processing.app.Platform;
import processing.app.syntax.PdeInputHandler;
import processing.app.ui.Editor;

public class InputManager extends PdeInputHandler {
	protected List<KeyHandler> keyHandlers = new ArrayList<>();

	// CONSTRUCTOR
	public InputManager(Editor editor, ActionTrigger... triggers) {
		super(editor);

		for (ActionTrigger trigger : triggers) {
			for (Map.Entry<String, Action> entry : trigger.getActions().entrySet()) {

				String keyBinding = entry.getKey();
				Action action = entry.getValue();
				String name = (String) action.getValue(Action.NAME);

				if (name == null) {
					addKeyBinding(keyBinding, action);

				} else {
					KeyStroke ks = parseKeyStroke(keyBinding);
					editor.getTextArea().getInputMap().put(ks, name);
					editor.getTextArea().getActionMap().put(name, action);
				}
			}
		}
	}

	public void addKeyHandler(KeyHandler... handlers) {
		for (KeyHandler handler : handlers) {
			keyHandlers.add(handler);
		}
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
			if ((keyChar == KeyEvent.VK_COMMA) || (keyChar == KeyEvent.VK_SPACE)) {
				e.consume();
				return true;
			}
		}

		for (KeyHandler handler : keyHandlers) {
			if (handler.handleTyped(e)) {
				e.consume();
				return true;
			}
		}
		return false;
	}
}