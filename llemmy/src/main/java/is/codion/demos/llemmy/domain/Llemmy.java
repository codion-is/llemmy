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
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Converter;

import dev.langchain4j.data.message.ChatMessageType;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.framework.domain.entity.OrderBy.descending;

// tag::llemmy[]
public final class Llemmy extends DomainModel {

	// Identifies this domain model
	public static final DomainType DOMAIN = domainType(Llemmy.class);

	public Llemmy() {
		super(DOMAIN);
		chat();
	}
	// end::llemmy[]

	// tag::chat_api[]
	public interface Chat {
		// Identifies this entity type
		EntityType TYPE = DOMAIN.entityType("llemmy.chat");

		// Primary key column
		Column<Integer> ID = TYPE.integerColumn("id");
		// Maps automatically to a native UUID H2 column
		Column<UUID> SESSION = TYPE.column("session", UUID.class);
		Column<String> NAME = TYPE.stringColumn("name");
		// Column based on an Enum
		Column<ChatMessageType> MESSAGE_TYPE = TYPE.column("message_type", ChatMessageType.class);
		Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
		// Derived from TIMESTAMP, not a table column
		Attribute<LocalTime> TIME = TYPE.localTimeAttribute("time");
		Column<String> MESSAGE = TYPE.stringColumn("message");
		Column<String> STACK_TRACE = TYPE.stringColumn("stack_trace");
		// Duration used as custom column type
		Column<Duration> RESPONSE_TIME = TYPE.column("response_time", Duration.class);
		Column<Integer> INPUT_TOKENS = TYPE.integerColumn("input_tokens");
		Column<Integer> OUTPUT_TOKENS = TYPE.integerColumn("output_tokens");
		Column<Integer> TOTAL_TOKENS = TYPE.integerColumn("total_tokens");
		Column<String> JSON = TYPE.stringColumn("json");
		// For implementing soft-delete
		Column<Boolean> DELETED = TYPE.booleanColumn("deleted");
	}
	// end::chat_api[]

	// tag::chat_impl[]
	private void chat() {
		add(Chat.TYPE.define(
										Chat.ID.define()
														.primaryKey()
														.generator(identity()),
										Chat.SESSION.define()
														.column(),
										Chat.TIMESTAMP.define()
														.column()
														.nullable(false)
														.dateTimePattern("yyyy-MM-dd HH:mm:ss")
														.caption("Time"),
										Chat.TIME.define()
														.derived()
														.from(Chat.TIMESTAMP)
														.value(source -> source.optional(Chat.TIMESTAMP)
																		.map(LocalDateTime::toLocalTime)
																		.orElse(null))
														.dateTimePattern("HH:mm:ss"),
										Chat.NAME.define()
														.column()
														.nullable(false)
														.caption("Name"),
										Chat.MESSAGE_TYPE.define()
														.column()
														// Specify that the enum is represented by an underlying
														// String column and provide a converter for converting
														// between the enum and the underlying column value
														.converter(String.class, new MessageTypeConverter())
														.caption("Type"),
										Chat.MESSAGE.define()
														.column()
														.caption("Message"),
										Chat.STACK_TRACE.define()
														.column()
														.caption("Stack trace"),
										Chat.RESPONSE_TIME.define()
														.column()
														.converter(Integer.class, new DurationConverter())
														.caption("Duration"),
										Chat.INPUT_TOKENS.define()
														.column()
														.caption("Input tokens"),
										Chat.OUTPUT_TOKENS.define()
														.column()
														.caption("Output tokens"),
										Chat.TOTAL_TOKENS.define()
														.column()
														.caption("Total tokens"),
										Chat.JSON.define()
														.column()
														.caption("JSON"),
										Chat.DELETED.define()
														.column()
														.nullable(false)
														.caption("Deleted")
														.defaultValue(false)
														.withDefault(true))
						.formatter(EntityFormatter.builder()
										// 12:38:12 @ OPEN_AI: Hello! How can I assist you today?
										.value(Chat.TIME)
										.text(" @ ")
										.value(Chat.NAME)
										.text(": ")
										.value(Chat.MESSAGE)
										.build())
						.orderBy(descending(Chat.TIMESTAMP))
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

	private static class DurationConverter implements Converter<Duration, Integer> {

		@Override
		public Integer toColumnValue(Duration duration, Statement statement) throws SQLException {
			return (int) duration.toMillis();
		}

		@Override
		public Duration fromColumnValue(Integer millis) throws SQLException {
			return Duration.ofMillis(millis);
		}
	}
	// end::chat_impl[]
}
