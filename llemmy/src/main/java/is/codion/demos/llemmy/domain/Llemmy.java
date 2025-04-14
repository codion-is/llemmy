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
package is.codion.demos.llemmy.domain;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Converter;

import dev.langchain4j.data.message.ChatMessageType;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.OrderBy.descending;

// tag::llemmy[]
public final class Llemmy extends DomainModel {

	public static final DomainType DOMAIN = domainType(Llemmy.class);

	public Llemmy() {
		super(DOMAIN);
		chatLog();
	}
	// end::llemmy[]

	// tag::chat_log_api[]
	public interface ChatLog {
		EntityType TYPE = DOMAIN.entityType("llemmy.chat_log");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<UUID> SESSION = TYPE.column("session", UUID.class);
		Column<String> NAME = TYPE.stringColumn("name");
		Column<ChatMessageType> MESSAGE_TYPE = TYPE.column("message_type", ChatMessageType.class);
		Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
		Attribute<LocalTime> TIME = TYPE.localTimeAttribute("time");
		Column<String> MESSAGE = TYPE.stringColumn("message");
		Column<String> STACK_TRACE = TYPE.stringColumn("stack_trace");
		Column<Integer> INPUT_TOKENS = TYPE.integerColumn("input_tokens");
		Column<Integer> OUTPUT_TOKENS = TYPE.integerColumn("output_tokens");
		Column<Integer> TOTAL_TOKENS = TYPE.integerColumn("total_tokens");
		Column<String> JSON = TYPE.stringColumn("json");
		Column<Boolean> DELETED = TYPE.booleanColumn("deleted");
	}
	// end::chat_log_api[]

	// tag::chat_log_impl[]
	private void chatLog() {
		add(ChatLog.TYPE.define(
										ChatLog.ID.define()
														.primaryKey(),
										ChatLog.SESSION.define()
														.column(),
										ChatLog.TIMESTAMP.define()
														.column()
														.nullable(false)
														.dateTimePattern("yyyy-MM-dd HH:mm:ss")
														.caption("Time"),
										ChatLog.TIME.define()
														.derived(from -> from.optional(ChatLog.TIMESTAMP)
																		.map(LocalDateTime::toLocalTime)
																		.orElse(null), ChatLog.TIMESTAMP)
														.dateTimePattern("HH:mm:ss"),
										ChatLog.NAME.define()
														.column()
														.nullable(false)
														.caption("Name"),
										ChatLog.MESSAGE_TYPE.define()
														.column()
														.columnClass(String.class, new MessageTypeConverter())
														.caption("Type"),
										ChatLog.MESSAGE.define()
														.column()
														.caption("Message"),
										ChatLog.STACK_TRACE.define()
														.column()
														.caption("Stack trace"),
										ChatLog.INPUT_TOKENS.define()
														.column()
														.caption("Input tokens"),
										ChatLog.OUTPUT_TOKENS.define()
														.column()
														.caption("Output tokens"),
										ChatLog.TOTAL_TOKENS.define()
														.column()
														.caption("Total tokens"),
										ChatLog.JSON.define()
														.column()
														.caption("JSON"),
										ChatLog.DELETED.define()
														.column()
														.nullable(false)
														.caption("Deleted")
														.defaultValue(false)
														.columnHasDefaultValue(true))
						.keyGenerator(identity())
						.stringFactory(StringFactory.builder()
										.value(ChatLog.TIME)
										.text(" @ ")
										.value(ChatLog.NAME)
										.text(": ")
										.value(ChatLog.MESSAGE)
										.build())
						.orderBy(descending(ChatLog.TIMESTAMP))
						.caption("Chat Log")
						.build());
	}

	private static final class MessageTypeConverter implements Converter<ChatMessageType, String> {

		@Override
		public String toColumnValue(ChatMessageType chatMessageType, Statement statement) throws SQLException {
			return chatMessageType.toString();
		}

		@Override
		public ChatMessageType fromColumnValue(String columnValue) throws SQLException {
			return ChatMessageType.valueOf(columnValue);
		}
	}
	// end::chat_log_impl[]
}
