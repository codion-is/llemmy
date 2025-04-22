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
package is.codion.demos.llemmy.app.model;

import is.codion.common.version.Version;
import is.codion.demos.llemmy.app.ui.LlemmyAppPanel;
import is.codion.demos.llemmy.model.ChatModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

/**
 * @see LlemmyAppPanel
 */
public final class LlemmyAppModel extends SwingEntityApplicationModel {

	public static final String NAME = "Llemmy";
	public static final Version VERSION = Version.parse(LlemmyAppModel.class, "/version.properties");

	public LlemmyAppModel(List<ChatLanguageModel> languageModels, EntityConnectionProvider connectionProvider) {
		super(connectionProvider, List.of(new ChatModel(languageModels, connectionProvider)), VERSION);
	}
}
