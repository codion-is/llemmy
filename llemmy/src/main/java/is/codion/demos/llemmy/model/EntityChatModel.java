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

import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

// tag::chat_model[]
public final class EntityChatModel extends SwingEntityModel {

	/**
	 * Instantiates a new {@link EntityChatModel} instance
	 * @param chatModels the chat models
	 * @param connectionProvider the connection provider
	 */
	public EntityChatModel(List<ChatModel> chatModels, EntityConnectionProvider connectionProvider) {
		super(new EntityChatTableModel(chatModels, connectionProvider));
	}
}
// end::chat_model[]