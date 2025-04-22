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
package is.codion.demos.llemmy.app.ui;

import is.codion.common.db.database.Database;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.demos.llemmy.app.model.LlemmyAppModel;
import is.codion.demos.llemmy.domain.Llemmy;
import is.codion.demos.llemmy.domain.Llemmy.Chat;
import is.codion.demos.llemmy.model.ChatModel;
import is.codion.demos.llemmy.ui.ChatPanel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.plugin.flatlaf.intellij.themes.dracula.Dracula;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import dev.langchain4j.model.chat.ChatLanguageModel;

import javax.swing.JComponent;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.common.user.User.user;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.comboBox;
import static is.codion.swing.common.ui.dialog.Dialogs.inputDialog;
import static is.codion.swing.framework.ui.EntityTablePanel.ColumnSelection.MENU;
import static is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES;
import static javax.swing.SwingConstants.LEADING;

/**
 * @see LlemmyAppModel
 */
public final class LlemmyAppPanel extends EntityApplicationPanel<LlemmyAppModel> {

	public LlemmyAppPanel(LlemmyAppModel applicationModel) {
		super(applicationModel,
						List.of(new ChatPanel((ChatModel) applicationModel.entityModels().get(Chat.TYPE))), List.of(),
						LlemmyApplicationLayout::new);
	}

	public static String selectModelName(List<String> modelNames, String defaultName) {
		return inputDialog(comboBox(FilterComboBoxModel.builder(modelNames).build())
						.value(defaultName)
						.preferredWidth(250)
						.buildValue())
						.title("Select model")
						.show();
	}

	@Override
	protected Optional<Controls> createViewMenuControls() {
		return Optional.of(Controls.builder()
						.caption(FrameworkMessages.view())
						.mnemonic(FrameworkMessages.viewMnemonic())
						.control(createAlwaysOnTopControl())
						.build());
	}

	@Override
	protected Optional<Controls> createHelpMenuControls() {
		State helpVisible = ((ChatPanel) entityPanel(Chat.TYPE)).helpVisible();

		return Optional.of(Controls.builder()
						.caption("Help")
						.mnemonic('H')
						.control(Control.builder()
										.toggle(helpVisible)
										.caption("Help")
										.build())
						.separator()
						.control(createLogControls())
						.separator()
						.control(createAboutControl())
						.build());
	}

	private static EntityConnectionProvider createConnectionProvider(User user) {
		return LocalEntityConnectionProvider.builder()
						.database(Database.instance())
						.domain(new Llemmy())
						.user(user)
						.build();
	}

	private static final class LlemmyApplicationLayout implements ApplicationLayout {

		private final EntityApplicationPanel<?> applicationPanel;

		private LlemmyApplicationLayout(EntityApplicationPanel<?> applicationPanel) {
			this.applicationPanel = applicationPanel;
		}

		@Override
		public JComponent layout() {
			return borderLayoutPanel()
							.centerComponent(applicationPanel.entityPanel(Chat.TYPE).initialize())
							.border(emptyBorder())
							.build();
		}
	}

	public static void start(Supplier<List<ChatLanguageModel>> languageModels) {
		FlatInspector.install("ctrl shift alt X");
		FlatInterFont.install();
		FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
		FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
		FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
		Locale.setDefault(Locale.of("en", "EN"));
		FilterTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(LEADING);
		ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(DISPLAY_DEPENDENCIES);
		EntityTablePanel.Config.COLUMN_SELECTION.set(MENU);
		EntityApplicationPanel.builder(LlemmyAppModel.class, LlemmyAppPanel.class)
						.applicationName(LlemmyAppModel.NAME)
						.applicationVersion(LlemmyAppModel.VERSION)
						.frameTitle("Llemmy")
						.user(user("sa"))
						.connectionProvider(LlemmyAppPanel::createConnectionProvider)
						.applicationModel(connectionProvider ->
										new LlemmyAppModel(languageModels.get(), connectionProvider))
						.displayStartupDialog(false)
						.defaultLookAndFeel(Dracula.class)
						.start();
	}
}
