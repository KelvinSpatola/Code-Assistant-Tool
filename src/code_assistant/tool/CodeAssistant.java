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

import processing.app.Base;
import processing.app.tools.Tool;
import processing.app.ui.Editor; 

public class CodeAssistant implements Tool {
	private final String TOOL_NAME = "Code Assistant";
	private Base base;

	public String getMenuTitle() {
		return TOOL_NAME;
	}

	public void init(Base base) {
		this.base = base;
	}

	public void run() {
		Editor editor = base.getActiveEditor();
		editor.getTextArea().setInputHandler(new CodeAssistantInputHandler(editor));

		System.out.println(TOOL_NAME + " v. ##tool.prettyVersion## by Kelvin Spatola.");

		// editor.statusNotice("Kelvin Clark ");
		// Messages.showWarning("PDE++ Tool", "Kelvin Clark Magalhaes Spatola");
	}
}