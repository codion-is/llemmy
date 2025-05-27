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

import is.codion.demos.llemmy.domain.Llemmy.Chat;
import is.codion.demos.llemmy.model.EntityChatEditModel;
import is.codion.demos.llemmy.model.EntityChatTableModel;
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

import static is.codion.swing.common.ui.Utilities.enableComponents;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Comparator.comparing;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.UIManager.getColor;

/**
 * @see EntityChatTableModel
 */
// tag::chat_table_panel[]
public final class EntityChatTablePanel extends EntityTablePanel {

	private final JTextPane chatPane = textPane()
					.editable(false)
					.build();
	private final StyledDocument document = chatPane.getStyledDocument();
	private final Style userStyle = document.addStyle("user", null);
	private final Style systemStyle = document.addStyle("system", null);

	/**
	 * Instantiates a new {@link EntityChatTablePanel}
	 * @param tableModel the {@link EntityChatTableModel} on which to base the panel
	 */
	public EntityChatTablePanel(EntityChatTableModel tableModel) {
		super(tableModel, config -> config
						// Lets skip the query conditions
						.includeConditions(false)
						// but include the filters
						.includeFilters(true)
						// and no south panel
						.includeSouthPanel(false)
						// Lets not allow the Chat.SESSION value
						// to be edited via the table popup menu
						.editable(attributes ->
										attributes.remove(Chat.SESSION)));
		// Refresh the chat each time the visible items or selection changes
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
			// which affects the colors
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
		List<Entity> chats = tableModel().selection().empty().get() ?
						tableModel().items().visible().get() :
						tableModel().selection().items().get();
		chatPane.setText("");
		chats.stream()
						.sorted(comparing(chat -> chat.get(Chat.TIMESTAMP)))
						.forEach(this::addToChatDocument);
	}

	private void addToChatDocument(Entity chat) {
		try {
			document.insertString(document.getLength(), chat.toString() + "\n\n", style(chat));
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private AttributeSet style(Entity chat) {
		return switch (chat.get(Chat.MESSAGE_TYPE)) {
			case USER -> userStyle;
			case SYSTEM -> systemStyle;
			default -> null;
		};
	}

	private void configureTable() {
		FilterTable<Entity, Attribute<?>> table = table();
		EntityChatEditModel editModel = (EntityChatEditModel) tableModel().editModel();
		// Disable the table while the model is processing
		enableComponents(editModel.processing().not(), table);
		// Set some minimum table column widths
		table.columnModel().column(Chat.TIMESTAMP).setMinWidth(160);
		table.columnModel().column(Chat.MESSAGE_TYPE).setMinWidth(80);
		table.columnModel().column(Chat.NAME).setMinWidth(80);
		// Set the default visible columns
		table.columnModel().visible().set(Chat.TIMESTAMP, Chat.MESSAGE_TYPE, Chat.NAME);
		// and the column auto resize mode
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		// We hardcoded the sorting in ChatTableModel
		table.sortingEnabled().set(false);
	}
}
// end::chat_table_panel[]