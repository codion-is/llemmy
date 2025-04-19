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

import is.codion.demos.llemmy.domain.Llemmy.ChatLog;
import is.codion.demos.llemmy.model.ChatLogEditModel;
import is.codion.demos.llemmy.model.ChatLogTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Comparator.comparing;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.UIManager.getColor;

/**
 * @see ChatLogTableModel
 */
public final class ChatLogTablePanel extends EntityTablePanel {

	private final JTextPane chatPane = textPane()
					.editable(false)
					.build();
	private final StyledDocument document = chatPane.getStyledDocument();
	private final Style userStyle = document.addStyle("user", null);
	private final Style systemStyle = document.addStyle("system", null);

	public ChatLogTablePanel(ChatLogTableModel tableModel) {
		super(tableModel, config -> config
						.includeConditions(false)
						.includeFilters(true)
						.includeSouthPanel(false)
						.editable(attributes ->
										attributes.remove(ChatLog.SESSION)));
		tableModel.items().visible().addListener(this::refreshChat);
		tableModel.selection().items().addListener(this::refreshChat);
		configureTable();
		configureStyles();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (chatPane != null) {
			// In case the Look & Feel changed
			// which changes the colors
			refreshChat();
		}
	}

	@Override
	protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
		setLayout(borderLayout());
		add(splitPane()
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.resizeWeight(0.75)
						.leftComponent(borderLayoutPanel()
										.border(createTitledBorder("Chat"))
										.centerComponent(scrollPane(chatPane).build())
										.build())
						.rightComponent(borderLayoutPanel()
										.border(createTitledBorder("History"))
										.centerComponent(tableComponent)
										.build())
						.build(), BorderLayout.CENTER);
	}

	void requestChatFocus() {
		chatPane.requestFocus();
	}

	void requestHistoryFocus() {
		table().requestFocus();
	}

	private void configureStyles() {
		configureUserStyle();
		StyleConstants.setForeground(systemStyle, Color.WHITE);
		StyleConstants.setBackground(systemStyle, Color.RED);
	}

	private void configureUserStyle() {
		// Keep this separate since these colors are Look & Feel dependent
		// and need to be updated each time it changes
		StyleConstants.setForeground(userStyle, getColor("TextPane.background"));
		StyleConstants.setBackground(userStyle, getColor("TextPane.foreground"));
	}

	private void refreshChat() {
		// In case the Look & Feel has changed
		configureUserStyle();
		// We display all the chat history if the selection is empty,
		// otherwise only the selected history
		List<Entity> chatLogs = tableModel().selection().empty().get() ?
						tableModel().items().visible().get() :
						tableModel().selection().items().get();
		chatPane.setText("");
		chatLogs.stream()
						.sorted(comparing(chatLog -> chatLog.get(ChatLog.TIMESTAMP)))
						.forEach(this::addToChatDocument);
	}

	private void addToChatDocument(Entity chatLog) {
		try {
			document.insertString(document.getLength(), chatLog.toString() + "\n\n", style(chatLog));
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private AttributeSet style(Entity chatLog) {
		return switch (chatLog.get(ChatLog.MESSAGE_TYPE)) {
			case USER -> userStyle;
			case SYSTEM -> systemStyle;
			default -> null;
		};
	}

	private void configureTable() {
		FilterTable<Entity, Attribute<?>> table = table();
		ChatLogEditModel editModel = (ChatLogEditModel) tableModel().editModel();
		// Disable the table while the model is processing
		linkToEnabledState(editModel.processing().not(), table);
		// Set some minimum table column widths
		table.columnModel().column(ChatLog.TIMESTAMP).setMinWidth(170);
		table.columnModel().column(ChatLog.MESSAGE_TYPE).setMinWidth(100);
		table.columnModel().column(ChatLog.NAME).setMinWidth(120);
		// Set the default visible columns
		table.columnModel().visible().set(ChatLog.TIMESTAMP, ChatLog.MESSAGE_TYPE, ChatLog.NAME);
		// and the column auto resize mode
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		// We hardcoded the sorting in ChatLogTableModel
		table.sortingEnabled().set(false);
	}
}
