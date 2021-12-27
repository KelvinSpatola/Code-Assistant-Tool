/**
 * you can put a one sentence description of your tool here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author   Kelvin Spatola
 * @modified ##date##
 * @version  ##tool.prettyVersion##
 */

package code_assistant.tool;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import code_assistant.completion.CodeTemplatesManager;
import code_assistant.util.Constants;
import code_assistant.util.ToolPreferences;
import processing.app.Base;
import processing.app.Platform;
import processing.app.tools.Tool;
import processing.app.ui.Editor;

public class CodeAssistant implements Tool, ActionTrigger {
	static public final String TOOL_NAME = "Code Assistant";
	private Base base;
	private boolean isRunning = false;

	@Override
	public String getMenuTitle() {
		return TOOL_NAME;
	}

	@Override
	public void init(Base base) {
		this.base = base;
		ToolPreferences.init();
	}

	@Override
	public void run() {
		Editor editor = base.getActiveEditor();

		final DefaultInputs defaultInputs = new DefaultInputs(editor);
		final JavaModeInputs javaModeInputs = new JavaModeInputs(editor);
		final BracketCloser bracketCloser = new BracketCloser(editor);
		final CodeTemplatesManager completion = new CodeTemplatesManager(editor);


		InputManager inputHandler = new InputManager(editor,
				defaultInputs,
				javaModeInputs,
				this);

		inputHandler.addKeyHandler(javaModeInputs, bracketCloser, completion);
		editor.getTextArea().setInputHandler(inputHandler);

		System.out.println(TOOL_NAME + " v. ##tool.prettyVersion## by Kelvin Spatola.");

		if (!isRunning) {
			isRunning = true;
			editor.statusNotice(TOOL_NAME + " is running.");

		} else {
			editor.statusNotice(TOOL_NAME + " is already active.");
		}
	}

	@Override
	public Map<String, Action> getActions() {
		Map<String, Action> actions = new HashMap<>();

		actions.put("F9", new AbstractAction("visit-website") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Platform.openURL(Constants.WEBSITE);
			}
		});

		return actions;
	}
}