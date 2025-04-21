/*
 * This file is part of Codion Llemmy Demo.
 *
 * Codion Llemmy Demo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion Llemmy Demo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion Llemmy Demo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demo.llemmy.openai;

import is.codion.demos.llemmy.ui.LlemmyAppPanel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static is.codion.swing.common.ui.component.Components.comboBox;
import static is.codion.swing.common.ui.dialog.Dialogs.inputDialog;

public final class Runner {

	private static final String API_KEY = System.getenv("OPENAI_API_KEY");

	private static final List<OpenAiChatModelName> MODEL_NAMES =
					List.of(OpenAiChatModelName.values());

	private Runner() {}

	public static void main(String[] args) {
		LlemmyAppPanel.start(() -> List.of(OpenAiChatModel.builder()
						.apiKey(API_KEY)
						.modelName(selectModel())
						.build()));
	}

	private static OpenAiChatModelName selectModel() {
		return inputDialog(comboBox(FilterComboBoxModel.builder(MODEL_NAMES).build())
						.value(GPT_4_O_MINI)
						.preferredWidth(250)
						.buildValue())
						.title("Choose model")
						.show();
	}
}
