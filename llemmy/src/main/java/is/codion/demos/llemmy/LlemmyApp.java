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
package is.codion.demos.llemmy;

import is.codion.common.db.database.Database;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.demos.llemmy.domain.Llemmy;
import is.codion.demos.llemmy.domain.Llemmy.Chat;
import is.codion.demos.llemmy.model.EntityChatModel;
import is.codion.demos.llemmy.ui.EntityChatEditPanel;
import is.codion.demos.llemmy.ui.EntityChatPanel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.plugin.flatlaf.intellij.themes.dracula.Dracula;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.common.user.User.user;
import static is.codion.swing.framework.ui.EntityTablePanel.ColumnSelection.MENU;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.LEADING;

/**
 * The main application panel, use {@link #start(Supplier)} to launch Llemmy.
 * {@snippet :
 * List<String> models = List.of("orca-mini", "llama2", "llama3");
 *
 * String selectedModel  = Dialogs.comboBoxSelectionDialog(models)
 *     .defaultSelection("orca-mini")
 *     .title("Select model")
 *     .select()
 *     .orElseThrow(CancelException::new);
 *
 * LlemmyApp.start(() -> List.of(OllamaChatModel.builder()
 *     .baseUrl("http://localhost:12345")
 *     .modelName(selectedModel)
 *     .build()));
 *}
 * @see #start(Supplier)
 */
// tag::app_panel[]
public final class LlemmyApp extends EntityApplicationPanel<LlemmyApp.LlemmyAppModel> {

	private LlemmyApp(LlemmyAppModel applicationModel) {
		super(applicationModel,
						// See LlemmyAppModel at the bottom of this class
						List.of(new EntityChatPanel(applicationModel.chatModel())), List.of(),
						// We replace the default application layout factory, which
						// produces a layout arranging the main panels in a tabbed pane,
						// but here we only have a single panel, no need for tabs
						applicationPanel -> () ->
										// Simply return our single panel, initialized
										applicationPanel.entityPanel(Chat.TYPE).initialize());
	}

	/**
	 * Override the default View menu, to exclude the Look & Feel selection
	 * menu item, since {@link EntityChatEditPanel} contains a Look & Feel combo box.
	 * @return the {@link Controls} on which to base the main view menu
	 */
	@Override
	protected Optional<Controls> createViewMenuControls() {
		return Optional.of(Controls.builder()
						.caption(FrameworkMessages.view())
						.mnemonic(FrameworkMessages.viewMnemonic())
						// Just include the Always on top control
						.control(createAlwaysOnTopControl())
						.build());
	}

	/**
	 * Override the default Help menu, to replace the default
	 * Help menu item with a control toggling the help state,
	 * showing/hiding the help panel.
	 * @return the {@link Controls} on which to base the main Help menu
	 */
	@Override
	protected Optional<Controls> createHelpMenuControls() {
		State help = ((EntityChatPanel) entityPanel(Chat.TYPE)).help();

		return Optional.of(Controls.builder()
						.caption("Help")
						.mnemonic('H')
						// A Control for toggling the help state
						// presented as a check box in the menu
						.control(Control.builder()
										.toggle(help)
										.caption("Help"))
						.separator()
						// Include the default log and about controls, separated
						.control(createLogControls())
						.separator()
						.control(createAboutControl())
						.build());
	}

	/**
	 * Manually instantiate a new {@link LocalEntityConnectionProvider} instance,
	 * since we are always running with a in-memory H2 database
	 * @param user the user
	 * @return a new {@link EntityConnectionProvider} instance
	 */
	private static EntityConnectionProvider createConnectionProvider(User user) {
		return LocalEntityConnectionProvider.builder()
						// Returns a Database based on the
						// 'codion.db.url' system property
						.database(Database.instance())
						// Inject the domain model
						.domain(new Llemmy())
						// Supply the user
						.user(user)
						.build();
	}

	public static void start(Supplier<List<ChatModel>> chatModels) {
		requireNonNull(chatModels, "chatModels is null");
		// Configure the jdbc URL ('codion.db.url')
		Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
		// and the database initialization script
		Database.DATABASE_INIT_SCRIPTS.set("classpath:create_schema.sql");
		// Configure FlatLaf related things, the inspector is not necessary
		// but very helpful when debugging UI related stuff
		FlatInspector.install("ctrl shift alt X");
		// Configure a decent font
		FlatInterFont.install();
		FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
		FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
		FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
		Locale.setDefault(Locale.of("en", "EN"));
		FilterTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(LEADING);
		// Display table column selection in a menu, instead of a dialog
		EntityTablePanel.Config.COLUMN_SELECTION.set(MENU);
		EntityApplicationPanel.builder(LlemmyAppModel.class, LlemmyApp.class)
						.applicationName(LlemmyAppModel.APPLICATION_NAME)
						.applicationVersion(LlemmyAppModel.APPLICATION_VERSION)
						.frameTitle(LlemmyAppModel.APPLICATION_NAME + " " + LlemmyAppModel.APPLICATION_VERSION)
						// The H2Database super-user
						.user(user("sa"))
						// We provide a factory for the EntityConnectionProvider,
						// since we just manually instantiate a Local one,
						// instead of relying on the ServiceLoader
						.connectionProvider(LlemmyApp::createConnectionProvider)
						// We must supply the language models when instatiating
						// the application model, so here we provide a factory,
						// which receives the EntityConnectionProvider from above
						.applicationModel(connectionProvider ->
										new LlemmyAppModel(chatModels.get(), connectionProvider))
						// We provide a factory for the panel instantiation,
						// which receives the LlemmyAppModel from above,
						// allowing us to keep the constructor private
						.applicationPanel(LlemmyApp::new)
						// No need, startup should be pretty quick
						.startupDialog(false)
						.defaultLookAndFeel(Dracula.class)
						.start();
	}

	static final class LlemmyAppModel extends SwingEntityApplicationModel {

		private static final String APPLICATION_NAME = "Llemmy";
		private static final Version APPLICATION_VERSION =
						Version.parse(LlemmyAppModel.class, "/version.properties");

		private LlemmyAppModel(List<ChatModel> chatModels,
													 EntityConnectionProvider connectionProvider) {
			super(connectionProvider,
							List.of(new EntityChatModel(chatModels, connectionProvider)),
							APPLICATION_VERSION);
		}

		private EntityChatModel chatModel() {
			return (EntityChatModel) entityModels().get(Chat.TYPE);
		}
	}
}
// end::app_panel[]