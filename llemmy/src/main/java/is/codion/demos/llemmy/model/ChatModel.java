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
package is.codion.demos.llemmy.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

// tag::chat_model[]
public final class ChatModel extends SwingEntityModel {

	/**
	 * Instantiates a new {@link ChatModel} instance
	 * @param languageModels the language models
	 * @param connectionProvider the connection provider
	 */
	public ChatModel(List<ChatLanguageModel> languageModels, EntityConnectionProvider connectionProvider) {
		super(new ChatTableModel(languageModels, connectionProvider));
	}
}
// end::chat_model[]