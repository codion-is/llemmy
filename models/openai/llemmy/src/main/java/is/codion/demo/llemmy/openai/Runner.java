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

import is.codion.common.model.CancelException;
import is.codion.demos.llemmy.LlemmyApp;
import is.codion.swing.common.ui.dialog.Dialogs;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static is.codion.swing.common.ui.component.Components.stringField;

// tag::runner[]
public final class Runner {

	private static final List<String> MODELS =
					Stream.of(OpenAiChatModelName.values())
									.map(Objects::toString)
									.toList();

	private Runner() {}

	public static void main(String[] args) {
		LlemmyApp.start(() -> List.of(OpenAiChatModel.builder()
						.apiKey(Dialogs.input()
										.component(stringField()
														.columns(25))
										.title("OpenAI API Key")
										.show())
						.modelName(Dialogs.select()
										.comboBox(MODELS)
										.defaultSelection(GPT_4_O_MINI.toString())
										.title("Select model")
										.select()
										.orElseThrow(CancelException::new))
						.build()));
	}
}
// end::runner[]