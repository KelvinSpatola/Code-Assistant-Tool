package code_assistant.gui;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import code_assistant.tool.ActionTrigger;
import processing.app.syntax.DefaultInputHandler;
import processing.app.ui.Editor;

public class MenuManager {
    protected Map<String, Action> actions;
    protected Editor editor;

    
    public MenuManager(ActionTrigger... triggers) {
        this.editor = editor;

        actions = new HashMap<>();

        for (ActionTrigger trigger : triggers) {
            actions.putAll(trigger.getActions());
        }
    }

    public void addToolsPopupMenu(Editor editor) {
        JPopupMenu popup = editor.getTextArea().getRightClickPopup();
        JMenu submenu = new JMenu("Code Assistant");    
        JMenuItem formatItem, commentItem, upperCaseItem, loweCaseItem, staticItem;
        
        submenu.setFont(new Font("Segoe UI", Font.BOLD, 12));

        popup.addSeparator(); // ---------------------------------------------
        popup.add(submenu);

        // JavaModeInputs.FORMAT_SELECTED_TEXT
        formatItem = new JMenuItem("Format selected text");
        formatItem.addActionListener(i -> actions.get("C+T").actionPerformed(null));
        formatItem.setAccelerator(DefaultInputHandler.parseKeyStroke("C+T"));
        submenu.add(formatItem);

        // JavaModeInputs.EXPAND_SELECTION
        staticItem = new JMenuItem("Expand Selection");
        staticItem.addActionListener(i -> actions.get("CA+RIGHT").actionPerformed(null));
        staticItem.setAccelerator(DefaultInputHandler.parseKeyStroke("CA+RIGHT"));
        submenu.add(staticItem);

        // JavaModeInputs.TOGGLE_BLOCK_COMMENT
        commentItem = new JMenuItem("Toggle block comment");
        commentItem.addActionListener(i -> actions.get("C+7").actionPerformed(null));
        commentItem.setAccelerator(DefaultInputHandler.parseKeyStroke("C+7"));
        submenu.add(commentItem);

        // DefaultInputs.TO_UPPER_CASE
        upperCaseItem = new JMenuItem("To upper case");
        upperCaseItem.addActionListener(i -> actions.get("CS+U").actionPerformed(null));
        upperCaseItem.setAccelerator(DefaultInputHandler.parseKeyStroke("CS+U"));
        submenu.add(upperCaseItem);

        // DefaultInputs.TO_LOWER_CASE
        loweCaseItem = new JMenuItem("To lower case");
        loweCaseItem.addActionListener(i -> actions.get("CS+L").actionPerformed(null));
        loweCaseItem.setAccelerator(DefaultInputHandler.parseKeyStroke("CS+L"));
        submenu.add(loweCaseItem);

        // CodeAssistant -> visit-website
        staticItem = new JMenuItem("Visit GitHub page");
        staticItem.addActionListener(i -> actions.get("F9").actionPerformed(null));
        staticItem.setAccelerator(DefaultInputHandler.parseKeyStroke("F9"));
        submenu.add(staticItem);

        submenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent arg0) {
                formatItem.setEnabled(editor.isSelectionActive());
                commentItem.setEnabled(editor.isSelectionActive());
                upperCaseItem.setEnabled(editor.isSelectionActive());
                loweCaseItem.setEnabled(editor.isSelectionActive());
            }
        });
    }

    public void addToolsMenuBar(JMenuBar menubar) {
//  JMenu codeAssistantMenu = new JMenu("Code Assistant");
//
//  JMenuItem item = Toolkit.newJMenuItem("Print Message", 'P');
//  item.addActionListener(e -> printMsg());
//  codeAssistantMenu.add(item);
//
//  JMenuBar menubar = new JMenuBar();
//  menubar.add(codeAssistantMenu);
//  editor.setJMenuBar(menubar);
//  editor.getJMenuBar().add(codeAssistantMenu);

    }
}
