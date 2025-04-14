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

import is.codion.demos.llemmy.domain.Llemmy.ChatLog;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

/**
 * Manages the chat log history.
 */
public final class ChatLogTableModel extends SwingEntityTableModel {

	public ChatLogTableModel(List<ChatLanguageModel> languageModels, EntityConnectionProvider connectionProvider) {
		super(new ChatLogEditModel(languageModels, connectionProvider));
		ChatLogEditModel editModel = (ChatLogEditModel) editModel();
		// Include only chat logs from our session
		queryModel().condition().get(ChatLog.SESSION).set().equalTo(editModel.session());
		// We implement soft delete (see ChatLogEditModel), so include
		// only chat log history records not marked as deleted
		queryModel().condition().get(ChatLog.DELETED).set().equalTo(false);
		// Hardcode the history sorting to the latest at top
		sort().descending(ChatLog.TIMESTAMP);
		// Display the message in the prompt when a history record is selected
		selection().item().addConsumer(this::onSelection);
	}

	private void onSelection(Entity chatLog) {
		ChatLogEditModel editModel = (ChatLogEditModel) editModel();
		editModel.prompt().set(chatLog == null ? null : chatLog.get(ChatLog.MESSAGE));
	}
}
