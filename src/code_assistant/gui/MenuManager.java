package code_assistant.gui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import code_assistant.tool.ActionTrigger;
import code_assistant.util.Constants;
import processing.app.Platform;
import processing.app.syntax.DefaultInputHandler;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;

public class MenuManager {
    protected Map<String, Action> actions;

    public MenuManager(ActionTrigger... triggers) {
        actions = new HashMap<>();

        for (ActionTrigger trigger : triggers) {
            actions.putAll(trigger.getActions());
        }
    }

    public void addToolsMenuBar(Editor editor) {
        JMenuBar menubar = editor.getJMenuBar();
        JMenu menu = new JMenu("Code Assistant");

        // DefaultInputs.DUPLICATE_UP
        JMenuItem duplicateUpItem = createItem("Duplicate lines up", "CA+UP", true);
        menu.add(duplicateUpItem);

        // DefaultInputs.DUPLICATE_DOWN
        JMenuItem duplicateDownItem = createItem("Duplicate lines down", "CA+DOWN", true);
        menu.add(duplicateDownItem);

        // DefaultInputs.MOVE_UP
        JMenuItem moveUpItem = createItem("Move lines up", "A+UP", true);
        menu.add(moveUpItem);

        // DefaultInputs.MOVE_DOWN
        JMenuItem moveDownItem = createItem("Move lines down", "A+DOWN", true);
        menu.add(moveDownItem);

        menu.addSeparator(); // ---------------------------------------------

        // DefaultInputs.DELETE_LINE
        JMenuItem deleteLineItem = createItem("Delete line", "C+E", true);
        menu.add(deleteLineItem);

        // DefaultInputs.DELETE_LINE_CONTENT
        JMenuItem deleteLineContentItem = createItem("Delete line content", "CS+E", true);
        menu.add(deleteLineContentItem);

        menu.addSeparator(); // ---------------------------------------------

        // JavaModeInputs.TOGGLE_BLOCK_COMMENT
        JMenuItem commentItem = createItem("Toggle block comment", "C+7", true);
        menu.add(commentItem);

        // JavaModeInputs.FORMAT_SELECTED_TEXT
        JMenuItem formatItem = createItem("Format selected text", "C+T", true);
        menu.add(formatItem);

        // DefaultInputs.TO_UPPER_CASE
        JMenuItem upperCaseItem = createItem("To upper case", "CS+U", true);
        menu.add(upperCaseItem);

        // DefaultInputs.TO_LOWER_CASE
        JMenuItem loweCaseItem = createItem("To lower case", "CS+L", true);
        menu.add(loweCaseItem);

        // JavaModeInputs.EXPAND_SELECTION
        JMenuItem expandIntem = createItem("Expand Selection", "CA+RIGHT", true);
        menu.add(expandIntem);

        menu.addSeparator(); // ---------------------------------------------

        JMenuItem websiteItem = new JMenuItem("Visit GitHub page");
        websiteItem.addActionListener(i -> Platform.openURL(Constants.WEBSITE));
        menu.add(websiteItem);

        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent arg0) {
                formatItem.setEnabled(editor.isSelectionActive());
                commentItem.setEnabled(editor.isSelectionActive());
                upperCaseItem.setEnabled(editor.isSelectionActive());
                loweCaseItem.setEnabled(editor.isSelectionActive());
            }
        });

        /*
         * This menu should be on the left of the Help menu, as the Processing
         * developers want. If this were a Mode, rather than a Tool, it would be
         * restricted to complying with this standard. So it's better to respect that
         * even taking into account that the developers left no way to restrict tools
         * from having a distinct menu. Anyway, this is good as it allows users to use
         * tools in different modes. If this project were rather a Mode, it would be
         * impossible to enjoy it in different modes (e.g. Python mode)
         */
        menubar.add(menu, menubar.getMenuCount() - 1);
        menubar.updateUI();
    }

    public void addToolsPopupMenu(Editor editor) {
        JPopupMenu popup = editor.getTextArea().getRightClickPopup();
        JMenu submenu = new JMenu("Code Assistant");

        popup.addSeparator(); // ---------------------------------------------

        // JavaModeInputs.FORMAT_SELECTED_TEXT
        JMenuItem formatItem = createItem("Format selected text", "C+T", false);
        submenu.add(formatItem);

        // JavaModeInputs.TOGGLE_BLOCK_COMMENT
        JMenuItem commentItem = createItem("Toggle block comment", "C+7", false);
        submenu.add(commentItem);

        // DefaultInputs.TO_UPPER_CASE
        JMenuItem upperCaseItem = createItem("To upper case", "CS+U", false);
        submenu.add(upperCaseItem);

        // DefaultInputs.TO_LOWER_CASE
        JMenuItem loweCaseItem = createItem("To lower case", "CS+L", false);
        submenu.add(loweCaseItem);

        submenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent arg0) {
                formatItem.setEnabled(editor.isSelectionActive());
                commentItem.setEnabled(editor.isSelectionActive());
                upperCaseItem.setEnabled(editor.isSelectionActive());
                loweCaseItem.setEnabled(editor.isSelectionActive());
            }
        });

        submenu.setFont(popup.getFont().deriveFont(Font.BOLD));
        popup.add(submenu);
    }

    private JMenuItem createItem(String title, String actionKey, boolean enableAccelerator) {
        JMenuItem item = new MenuItem(title);
        item.addActionListener(i -> actions.get(actionKey).actionPerformed(null));
        if (enableAccelerator)
            item.setAccelerator(DefaultInputHandler.parseKeyStroke(actionKey));
        return item;
    }

    private class MenuItem extends JMenuItem {
        MenuItem(String title) {
            super(title);
        }

        @Override
        public void setAccelerator(KeyStroke keyStroke) {
            super.setAccelerator(keyStroke);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "none");
        }
    }
}
