package code_assistant.tool;

import java.awt.event.KeyEvent;

public interface KeyHandler {
	boolean handlePressed(KeyEvent e);
	boolean handleTyped(KeyEvent e);
}
