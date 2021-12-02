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
 * @author   ##author##
 * @modified ##date##
 * @version  ##tool.prettyVersion##
 */

package code_assistant.tool;

import processing.app.Base;
import processing.app.tools.Tool; 

public class CodeAssistant implements Tool {
	private Base base;

	public String getMenuTitle() {
		return "##tool.name##";
	}

	public void init(Base base) {
		this.base = base;
	}

	public void run() {
		base.getActiveEditor().getTextArea().setInputHandler(new ToolInputHandler(base.getActiveEditor()));

		System.out.println(" ##tool.name## v. ##tool.prettyVersion## by ##author##.");

		// editor.statusNotice("Kelvin Clark ");
		// Messages.showWarning("PDE++ Tool", "Kelvin Clark Magalhaes Spatola");
	}
}
