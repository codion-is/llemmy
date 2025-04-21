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
package is.codion.demo.llemmy.ollama;

import is.codion.demos.llemmy.app.ui.LlemmyAppPanel;

import dev.langchain4j.model.ollama.OllamaChatModel;

import java.util.List;

import static is.codion.demos.llemmy.app.ui.LlemmyAppPanel.selectModelName;

public final class Runner {

	public static final String ORCA_MINI = "orca-mini";
	public static final List<String> MODELS = List.of(
					ORCA_MINI,
					"llama3",
					"llama2",
					"mistral",
					"codellama",
					"phi",
					"tinyllama",
					"ollama-test");

	private Runner() {}

	public static void main(String[] args) {
		LlemmyAppPanel.start(() -> List.of(OllamaChatModel.builder()
						.baseUrl("http://localhost:11434")
						.modelName(selectModelName(MODELS, ORCA_MINI))
						.build()));
	}
}
