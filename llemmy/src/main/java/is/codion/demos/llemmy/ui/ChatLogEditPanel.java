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
import is.codion.demos.llemmy.model.ChatLogEditModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.framework.ui.EntityEditPanel;

import dev.langchain4j.model.chat.ChatLanguageModel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.time.Duration;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.dialog.Dialogs.fileSelectionDialog;
import static is.codion.swing.common.ui.dialog.Dialogs.selectionDialog;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.lang.String.format;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * Manages the UI for chatting with a language model.
 * @see ChatLogEditModel
 */
public final class ChatLogEditPanel extends EntityEditPanel {

	// The mime types available for attachments
	private static final List<String> SUPPORTED_MIME_TYPES = List.of(
					ChatLogEditModel.PDF,
					ChatLogEditModel.IMAGE_JPEG,
					ChatLogEditModel.IMAGE_PNG,
					ChatLogEditModel.TEXT_PLAIN);

	private final ChatLogEditModel editModel;

	private final JPanel languageModelPanel;
	private final JComboBox<Item<ChatLanguageModel>> languageModelComboBox;
	private final JTextArea promptTextArea;
	private final JList<ChatLogEditModel.Attachment> attachmentsList;
	private final JScrollPane attachmentsScrollPane;
	private final JScrollPane promptScrollPane;
	private final JProgressBar progressBar;
	private final JButton clearButton;
	private final JButton sendButton;
	private final JComboBox<Item<LookAndFeelEnabler>> lookAndFeelComboBox =
					LookAndFeelComboBox.builder().build();

	public ChatLogEditPanel(ChatLogEditModel editModel) {
		super(editModel);
		this.editModel = editModel;
		this.languageModelComboBox = createLanguageModelComboBox();
		this.languageModelPanel = borderLayoutPanel()
						.centerComponent(languageModelComboBox)
						.build();
		this.clearButton = button(createClearControl()).build();
		Control sendControl = createSendControl();
		this.sendButton = button(sendControl).build();
		this.promptTextArea = createPromptTextArea(sendControl);
		this.attachmentsList = createAttachmentsList();
		this.attachmentsScrollPane = scrollPane(attachmentsList).build();
		this.promptScrollPane = scrollPane(promptTextArea).build();
		this.progressBar = createProgressBar();
		editModel.processing().addConsumer(this::onProcessingChanged);
		editModel.elapsed().addConsumer(this::onElapsedChanged);
		focus().initial().set(promptTextArea);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// Here we update the UI of components that may
		// not be visible during Look & Feel selection
		Utilities.updateUI(progressBar, languageModelComboBox);
	}

	void requestModelFocus() {
		languageModelComboBox.requestFocus();
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
										.northComponent(createModelPanel())
										.centerComponent(borderLayoutPanel()
														.border(createTitledBorder("Prompt"))
														.centerComponent(promptScrollPane)
														.build())
										.build())
						.rightComponent(borderLayoutPanel()
										.northComponent(createLookAndFeelPanel())
										.centerComponent(borderLayoutPanel()
														.border(createTitledBorder("Attachments"))
														.centerComponent(attachmentsScrollPane)
														.build())
										.build())
						.build(), BorderLayout.CENTER);
	}

	private JPanel createModelPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder("Model"))
						.centerComponent(languageModelPanel)
						.eastComponent(gridLayoutPanel(1, 2)
										.add(clearButton)
										.add(sendButton)
										.build())
						.build();
	}

	private JPanel createLookAndFeelPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder("Look & Feel"))
						.centerComponent(lookAndFeelComboBox)
						.build();
	}

	private Control createClearControl() {
		return Control.builder()
						.command(editModel.prompt()::clear)
						.caption("Clear")
						.mnemonic('C')
						// Only enabled when the model is ready
						.enabled(editModel.ready())
						.build();
	}

	private Control createSendControl() {
		return Control.builder()
						.command(editModel::send)
						.caption("Send")
						.mnemonic('S')
						// Only enabled when the model is ready
						.enabled(editModel.ready())
						.build();
	}

	private JComboBox<Item<ChatLanguageModel>> createLanguageModelComboBox() {
		return comboBox(editModel.languageModels())
						// Only enabled when the model is not processing
						.enabled(editModel.processing().not())
						.preferredWidth(200)
						.build();
	}

	private JTextArea createPromptTextArea(Control sendControl) {
		return textArea(editModel.prompt())
						.rowsColumns(5, 40)
						.lineWrap(true)
						.wrapStyleWord(true)
						// Only enabled when the model is not processing
						.enabled(editModel.processing().not())
						.validIndicator(editModel.error().not())
						// Ctrl-Enter sends the prompt
						.keyEvent(KeyEvents.builder(VK_ENTER)
										.modifiers(CTRL_DOWN_MASK)
										.action(sendControl))
						.build();
	}

	private JList<ChatLogEditModel.Attachment> createAttachmentsList() {
		return Components.list(editModel.attachments())
						// The List value is based on the items in
						// the list, as opposed to the selected items.
						.items()
						// Only enabled when the model is not processing
						.enabled(editModel.processing().not())
						// Insert to add attachment
						.keyEvent(KeyEvents.builder(VK_INSERT)
										.action(command(this::addAttachment)))
						// Delete to remove the selected attachment
						.keyEvent(KeyEvents.builder(VK_DELETE)
										.action(command(this::removeAttachment)))
						.build();
	}

	private void addAttachment() {
		// Select the file mime type
		selectionDialog(SUPPORTED_MIME_TYPES)
						.owner(attachmentsList)
						// Restricts the selection to a single item
						.selectSingle()
						.ifPresent(mimeType ->
										fileSelectionDialog()
														// Filter files by the selected mime type
														.fileFilter(fileFilter(mimeType))
														// Select one or more files
														.selectFiles()
														// Add the attachments
														.forEach(file ->
																		editModel.addAttachment(file.toPath(), mimeType)));
	}

	private void removeAttachment() {
		attachmentsList.getSelectedValuesList().forEach(editModel::removeAttachment);
	}

	private JProgressBar createProgressBar() {
		return progressBar()
						.indeterminate(true)
						.stringPainted(true)
						.string("")
						.build();
	}

	private static FileFilter fileFilter(String mimeType) {
		return switch (mimeType) {
			case ChatLogEditModel.IMAGE_PNG -> new FileNameExtensionFilter("PNG", "png");
			case ChatLogEditModel.IMAGE_JPEG -> new FileNameExtensionFilter("JPEG", "jpg", "jpeg");
			case ChatLogEditModel.TEXT_PLAIN -> new FileNameExtensionFilter("Text", "txt", "csv");
			case ChatLogEditModel.PDF -> new FileNameExtensionFilter("PDF", "pdf");
			default -> throw new IllegalArgumentException("Unsupported mime type: " + mimeType);
		};
	}

	private void onProcessingChanged(boolean processing) {
		invokeLater(() -> {
			languageModelPanel.removeAll();
			languageModelPanel.add(processing ? progressBar : languageModelComboBox, BorderLayout.CENTER);
			progressBar.requestFocus();
			languageModelPanel.revalidate();
			languageModelPanel.repaint();
		});
	}

	private void onElapsedChanged(Duration elapsed) {
		invokeLater(() -> progressBar.setString(
						format("%02d:%02d", elapsed.toMinutes(), elapsed.toSecondsPart())));
	}
}
