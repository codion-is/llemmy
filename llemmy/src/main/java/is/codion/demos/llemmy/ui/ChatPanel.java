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
package is.codion.demos.llemmy.ui;

import is.codion.common.state.State;
import is.codion.demos.llemmy.LlemmyApp;
import is.codion.demos.llemmy.model.ChatEditModel;
import is.codion.demos.llemmy.model.ChatModel;
import is.codion.demos.llemmy.model.ChatTableModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static is.codion.swing.common.ui.component.Components.scrollPane;
import static is.codion.swing.common.ui.component.Components.textArea;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.stream.Collectors.joining;
import static javax.swing.BorderFactory.createTitledBorder;

// tag::chat_panel[]
/**
 * Combines the {@link ChatEditPanel} for the chat prompt interface
 * and {@link ChatTablePanel} for the chat text and history.
 * @see ChatModel
 */
public final class ChatPanel extends EntityPanel {

	private final HelpPanel helpPanel = new HelpPanel();
	private final State help = State.builder()
					.consumer(this::onHelpChanged)
					.build();

	/**
	 * Instantiates a new {@link ChatPanel}
	 * @param model the {@link ChatModel} on which to base the panel
	 */
	public ChatPanel(ChatModel model) {
		super(model,
						new ChatEditPanel((ChatEditModel) model.editModel()),
						new ChatTablePanel((ChatTableModel) model.tableModel()),
						config -> config
										// Skip the default CRUD operation controls
										.includeControls(false)
										// No base panel needed for the edit panel since we
										// want it to fill the whole width of the parent panel
										.editBasePanel(editPanel -> editPanel));
		setupKeyEvents();
	}

	/**
	 * @return the {@link State} controlling whether the help panel is visible
	 */
	public State help() {
		return help;
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// Here we update the UI of components that may
		// not be visible during Look & Feel selection
		Utilities.updateUI(helpPanel);
	}

	private void setupKeyEvents() {
		// Set up some global key events for this panel.
		// Note that calling addKeyEvent() assures that the key event is
		// added to this base panel and to the edit panel as well,
		// since that may be displayed in another window.
		ChatEditModel editModel = (ChatEditModel) editModel();
		ChatEditPanel editPanel = (ChatEditPanel) editPanel();
		ChatTablePanel tablePanel = (ChatTablePanel) tablePanel();
		// Set the base parameters, the modifier and the condition
		KeyEvents.Builder keyEvent = KeyEvents.builder()
						.modifiers(ALT_DOWN_MASK)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		// Set the keycode and action and add the key event
		addKeyEvent(keyEvent.keyCode(VK_0)
						.action(command(editPanel::requestModelFocus)));
		addKeyEvent(keyEvent.keyCode(VK_1)
						.action(command(editPanel::requestPromptFocus)));
		addKeyEvent(keyEvent.keyCode(VK_2)
						.action(command(editPanel::requestAttachmentFocus)));
		addKeyEvent(keyEvent.keyCode(VK_3)
						.action(command(tablePanel::requestChatFocus)));
		addKeyEvent(keyEvent.keyCode(VK_4)
						.action(command(tablePanel::requestHistoryFocus)));
		addKeyEvent(keyEvent.keyCode(VK_5)
						.action(command(editPanel::requestLookAndFeelFocus)));
		addKeyEvent(keyEvent.keyCode(VK_6)
						.action(command(helpPanel.shortcuts::requestFocus)));
		addKeyEvent(keyEvent.keyCode(VK_UP)
						.modifiers(CTRL_DOWN_MASK)
						.action(Control.builder()
										// Use the built in method to decrement the selected table model indexes
										.command(tablePanel.tableModel().selection().indexes()::decrement)
										// Only enabled while the model is not processing
										.enabled(editModel.processing().not())
										.build()));
		addKeyEvent(keyEvent.keyCode(VK_DOWN)
						.action(Control.builder()
										// Use the built in method to increment the selected table model indexes
										.command(tablePanel.tableModel().selection().indexes()::increment)
										// Only enabled while the model is not processing
										.enabled(editModel.processing().not())
										.build()));
	}

	private void onHelpChanged(boolean visible) {
		if (visible) {
			add(helpPanel, BorderLayout.EAST);
		}
		else {
			remove(helpPanel);
		}
		revalidate();
		repaint();
	}

	private static final class HelpPanel extends JPanel {

		private final JTextArea shortcuts = textArea()
						.value(helpText())
						.font(monospaceFont())
						.editable(false)
						.build();

		private HelpPanel() {
			super(borderLayout());
			setBorder(createTitledBorder("Shortcuts"));
			add(scrollPane(shortcuts).build(), BorderLayout.CENTER);
		}

		@Override
		public void updateUI() {
			super.updateUI();
			Utilities.updateUI(shortcuts);
		}

		private static String helpText() {
			try (InputStream inputStream = LlemmyApp.class.getResourceAsStream("shortcuts.txt")) {
				return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
								.lines()
								.collect(joining("\n"));
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to load shortcuts.txt", e);
			}
		}

		private static Font monospaceFont() {
			Font font = UIManager.getFont("TextArea.font");

			return new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
		}
	}
}
// end::chat_panel[]