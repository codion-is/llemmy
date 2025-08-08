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

import is.codion.common.item.Item;
import is.codion.demos.llemmy.model.EntityChatEditModel;
import is.codion.demos.llemmy.model.EntityChatEditModel.Attachment;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.framework.ui.EntityEditPanel;

import dev.langchain4j.model.chat.ChatModel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.time.Duration;
import java.util.List;

import static is.codion.demos.llemmy.model.EntityChatEditModel.MimeType;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * Manages the UI for chatting with a large language model.
 * @see EntityChatEditModel
 */
// tag::chat_edit_panel[]
public final class EntityChatEditPanel extends EntityEditPanel {

	private final EntityChatEditModel model;

	private final JComboBox<Item<ChatModel>> chatModelComboBox;
	private final JPanel chatModelPanel;
	private final JTextArea promptTextArea;
	private final JScrollPane promptScrollPane;
	private final JList<Attachment> attachmentsList;
	private final JScrollPane attachmentsScrollPane;
	private final JProgressBar progressBar;
	private final JButton clearButton;
	private final JButton sendButton;
	private final JComboBox<Item<LookAndFeelEnabler>> lookAndFeelComboBox =
					LookAndFeelComboBox.builder().build();

	public EntityChatEditPanel(EntityChatEditModel model) {
		super(model);
		this.model = model;
		this.chatModelComboBox = createChatModelComboBox();
		this.chatModelPanel = borderLayoutPanel()
						.center(chatModelComboBox)
						.build();
		Control sendControl = createSendControl();
		this.promptTextArea = createPromptTextArea(sendControl);
		this.promptScrollPane = scrollPane()
						.view(promptTextArea)
						.build();
		this.attachmentsList = createAttachmentsList();
		this.attachmentsScrollPane = scrollPane()
						.view(attachmentsList)
						.build();
		this.progressBar = createProgressBar();
		this.clearButton = button()
						.control(createClearControl())
						.build();
		this.sendButton = button()
						.control(sendControl)
						.build();
		model.processing().addConsumer(this::onProcessingChanged);
		model.elapsed().addConsumer(this::onElapsedChanged);
		focus().initial().set(promptTextArea);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// Here we update the UI of components that may
		// not be visible during Look & Feel selection
		Utilities.updateUI(progressBar, chatModelComboBox);
	}

	void requestModelFocus() {
		chatModelComboBox.requestFocus();
	}

	void requestPromptFocus() {
		promptTextArea.requestFocus();
	}

	void requestAttachmentFocus() {
		attachmentsList.requestFocus();
	}

	void requestLookAndFeelFocus() {
		lookAndFeelComboBox.requestFocus();
	}

	@Override
	protected void initializeUI() {
		setLayout(borderLayout());
		add(splitPane()
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.resizeWeight(0.75)
						.leftComponent(borderLayoutPanel()
										.north(createModelPanel())
										.center(borderLayoutPanel()
														.border(createTitledBorder("Prompt"))
														.center(promptScrollPane)))
						.rightComponent(borderLayoutPanel()
										.north(createLookAndFeelPanel())
										.center(borderLayoutPanel()
														.border(createTitledBorder("Attachments"))
														.center(attachmentsScrollPane)))
						.build(), BorderLayout.CENTER);
	}

	private JPanel createModelPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder("Model"))
						.center(chatModelPanel)
						.east(gridLayoutPanel(1, 2)
										.addAll(clearButton, sendButton))
						.build();
	}

	private JPanel createLookAndFeelPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder("Look & Feel"))
						.center(lookAndFeelComboBox)
						.build();
	}

	private Control createClearControl() {
		return Control.builder()
						.command(model.prompt()::clear)
						.caption("Clear")
						.mnemonic('C')
						// Only enabled when the model is ready
						.enabled(model.ready())
						.build();
	}

	private Control createSendControl() {
		return Control.builder()
						.command(model::send)
						.caption("Send")
						.mnemonic('S')
						// Only enabled when the model is ready
						.enabled(model.ready())
						.build();
	}

	private JComboBox<Item<ChatModel>> createChatModelComboBox() {
		return comboBox()
						.model(model.chatModels())
						// Only enabled when the model is not processing
						.enabled(model.processing().not())
						.preferredWidth(200)
						.build();
	}

	private JTextArea createPromptTextArea(Control sendControl) {
		return textArea()
						.link(model.prompt())
						.rowsColumns(5, 40)
						.lineWrap(true)
						.wrapStyleWord(true)
						// Only enabled when the model is not processing
						.enabled(model.processing().not())
						.validIndicator(model.error().not())
						// Ctrl-Enter sends the prompt
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.modifiers(CTRL_DOWN_MASK)
										.action(sendControl))
						.build();
	}

	private JList<Attachment> createAttachmentsList() {
		return Components.list()
						.model(model.attachments())
						// The List value is based on the items in
						// the list, as opposed to the selected items.
						.items()
						// Only enabled when the model is not processing
						.enabled(model.processing().not())
						// Insert to add attachment
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_INSERT)
										.action(command(this::addAttachment)))
						// Delete to remove the selected attachment
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_DELETE)
										.action(command(this::removeAttachment)))
						.build();
	}

	private void addAttachment() {
		// Select the file mime type
		Dialogs.select()
						.list(List.of(MimeType.values()))
						// Set the modal dialog owner
						.owner(attachmentsList)
						.select()
						// Restricts the selection to a single item
						.single()
						// Returns an empty Optional in case the user cancels
						.ifPresent(mimeType ->
										// Select the file to attach
										Dialogs.select()
														.files()
														// Filter files by the selected mime type
														.filter(mimeType.fileFilter())
														// Select one or more files
														.selectFiles()
														// Add the attachments
														.forEach(file ->
																		model.addAttachment(file.toPath(), mimeType)));
	}

	private void removeAttachment() {
		model.attachments().selection().items().get().forEach(model::removeAttachment);
	}

	private JProgressBar createProgressBar() {
		return progressBar()
						.indeterminate(true)
						.stringPainted(true)
						.string("")
						.build();
	}

	private void onProcessingChanged(boolean processing) {
		chatModelPanel.removeAll();
		chatModelPanel.add(processing ? progressBar : chatModelComboBox, BorderLayout.CENTER);
		progressBar.requestFocus();
		chatModelPanel.revalidate();
		chatModelPanel.repaint();
	}

	private void onElapsedChanged(Duration elapsed) {
		// Use invokeLater() since this gets called in a background thread
		invokeLater(() -> progressBar.setString(
						format("%02d:%02d", elapsed.toMinutes(), elapsed.toSecondsPart())));
	}
}
// end::chat_edit_panel[]