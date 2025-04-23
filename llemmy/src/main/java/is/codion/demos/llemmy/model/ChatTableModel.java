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

import is.codion.demos.llemmy.domain.Llemmy.Chat;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

import static dev.langchain4j.data.message.ChatMessageType.USER;

// tag::chat_table_model[]
/**
 * Manages the chat log history.
 */
public final class ChatTableModel extends SwingEntityTableModel {

	/**
	 * Instantiates a new {@link ChatTableModel} instance
	 * @param languageModels the language models
	 * @param connectionProvider the connection provider
	 */
	public ChatTableModel(List<ChatLanguageModel> languageModels, EntityConnectionProvider connectionProvider) {
		super(new ChatEditModel(languageModels, connectionProvider));
		ChatEditModel editModel = (ChatEditModel) editModel();
		// Include only chat logs from our session
		queryModel().condition().get(Chat.SESSION).set().equalTo(editModel.session());
		// We implement soft delete (see ChatEditModel), so include
		// only chat log history records not marked as deleted
		queryModel().condition().get(Chat.DELETED).set().equalTo(false);
		// Hardcode the history sorting to the latest at top
		sort().descending(Chat.TIMESTAMP);
		// Display the message in the prompt when a history record is selected
		selection().item().addConsumer(this::onSelection);
	}

	private void onSelection(Entity chat) {
		ChatEditModel model = (ChatEditModel) editModel();
		if (chat == null) {
			model.prompt().clear();
		}
		else if (chat.get(Chat.MESSAGE_TYPE) == USER) {
			model.prompt().set(chat.get(Chat.MESSAGE));
		}
	}
}
// end::chat_table_model[]