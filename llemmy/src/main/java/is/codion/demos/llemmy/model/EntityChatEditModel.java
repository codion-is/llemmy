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
package is.codion.demos.llemmy.model;

import is.codion.common.item.Item;
import is.codion.common.observable.Observable;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.demos.llemmy.domain.Llemmy.Chat;
import is.codion.demos.llemmy.ui.EntityChatEditPanel;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTask;
import is.codion.swing.framework.model.SwingEntityEditModel;

import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.data.message.ChatMessageSerializer.messageToJson;
import static is.codion.common.item.Item.item;
import static is.codion.common.state.State.and;
import static java.lang.System.getProperty;
import static java.nio.file.Files.readAllBytes;
import static java.time.Duration.ZERO;
import static java.time.Duration.between;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

/**
 * Manages the state and the business logic for chatting with a language model.
 * @see EntityChatEditPanel
 */
// tag::chat_edit_model[]
public final class EntityChatEditModel extends SwingEntityEditModel {

	// The mime types available for attachments
	public enum MimeType {
		PDF("application/pdf", new FileNameExtensionFilter("PDF", "pdf")),
		JPEG("image/jpeg", new FileNameExtensionFilter("JPEG", "jpg", "jpeg")),
		PNG("image/png", new FileNameExtensionFilter("PNG", "png")),
		PLAIN_TEXT("text/plain", new FileNameExtensionFilter("Text", "txt", "csv"));

		private final String type;
		private final FileFilter fileFilter;

		MimeType(String type, FileFilter fileFilter) {
			this.type = type;
			this.fileFilter = fileFilter;
		}

		public String type() {
			return type;
		}

		public FileFilter fileFilter() {
			return fileFilter;
		}
	}

	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final String USER = getProperty("user.name");
	private static final String SYSTEM = "System";

	// Identifies the current chat session
	private final UUID session = randomUUID();
	// Indicates that the prompt text is empty
	private final State promptEmpty = State.state(true);
	// Indicates that the attachments list model is empty
	private final State attachmentsEmpty = State.state(true);
	// Indicates whether processing is ongoing
	private final State processing = State.state();
	// Indicates whether the last prompt resulted in an error
	private final State error = State.state();
	// Indicates whether prompt data is available and the model is not processing
	private final ObservableState ready =
					and(processing.not(), and(promptEmpty, attachmentsEmpty).not());

	// Holds the time the last prompt was issued to the chat model
	private final Value<LocalDateTime> started = Value.nullable();
	// Holds the elapsed processing time of the current query
	private final Value<Duration> elapsed = Value.nonNull(ZERO);
	// Updates the elapsed time every second during processing
	private final TaskScheduler elapsedUpdater =
					TaskScheduler.builder()
									.task(this::updateElapsed)
									.interval(1, TimeUnit.SECONDS)
									.build();

	// Contains the available chat models
	private final FilterComboBoxModel<Item<ChatModel>> chatModels;
	// Contains the file attachments
	private final FilterListModel<Attachment> attachments =
					FilterListModel.builder()
									.items(Collections.<Attachment>emptyList())
									.build();
	// Contains the prompt text
	private final Value<String> prompt = Value.builder()
					.nonNull("")
					// Update the promptEmpty state each time the value changes
					.consumer(value -> promptEmpty.set(value.trim().isEmpty()))
					.build();

	/**
	 * Instantiates a new {@link EntityChatEditModel} instance
	 * @param chatModels the chat models
	 * @param connectionProvider the connection provider
	 * @throws IllegalArgumentException in case {@code chatModels} is empty
	 */
	public EntityChatEditModel(List<ChatModel> chatModels,
														 EntityConnectionProvider connectionProvider) {
		super(Chat.TYPE, connectionProvider);
		if (chatModels.isEmpty()) {
			throw new IllegalArgumentException("No language model(s) provided");
		}
		// Wrap the language models in Item instances, for a caption to display in the combo box
		this.chatModels = FilterComboBoxModel.builder()
						.items(chatModels.stream()
										.map(model -> item(model, model.provider().name()))
										.toList())
						.selected(chatModels.getFirst())
						.build();
	}

	public UUID session() {
		return session;
	}

	public Value<String> prompt() {
		return prompt;
	}

	public FilterListModel<Attachment> attachments() {
		return attachments;
	}

	public void addAttachment(Path path, MimeType mimeType) {
		attachments.items().add(createAttachment(requireNonNull(path), requireNonNull(mimeType)));
		attachmentsEmpty.set(false);
	}

	public void removeAttachment(Attachment attachment) {
		attachments.items().remove(requireNonNull(attachment));
		attachmentsEmpty.set(attachments.items().count() == 0);
	}

	/**
	 * Updated on the Event Dispatch Thread
	 * @return the ready indicator
	 */
	public ObservableState ready() {
		return ready;
	}

	/**
	 * Updated on the Event Dispatch Thread
	 * @return the processing indicator
	 */
	public ObservableState processing() {
		return processing.observable();
	}

	/**
	 * Updated in a worker thread.
	 * @return the elapsed time
	 */
	public Observable<Duration> elapsed() {
		return elapsed.observable();
	}

	/**
	 * Updated on the Event Dispatch Thread
	 * @return the error indicator
	 */
	public ObservableState error() {
		return error.observable();
	}

	/**
	 * @return the available chat models
	 */
	public FilterComboBoxModel<Item<ChatModel>> chatModels() {
		return chatModels;
	}

	/**
	 * Sends the current prompt along with all attachments.
	 */
	public void send() {
		// Here we perform a couple of async operations, calling the
		// model and inserting the results in a background thread.
		// We assume this method gets called on the EDT.
		UserMessageTask task = new UserMessageTask();
		// UserMessageTask splits the task into a couple of
		// background tasks and some extra methods
		// that must be performed on the EDT.
		send(task);
	}

	@Override
	protected void delete(Collection<Entity> entities, EntityConnection connection) {
		// We override the default delete implementation, in order to implement soft delete
		connection.update(entities.stream()
						.map(this::setDeleted)
						.filter(Entity::modified)
						.toList());
	}

	private Entity setDeleted(Entity entity) {
		entity.set(Chat.DELETED, true);

		return entity;
	}

	private void updateElapsed() {
		elapsed.set(started.optional()
						.map(time -> between(time, LocalDateTime.now()))
						.orElse(ZERO));
	}

	private static void send(UserMessageTask task) {
		// On the Event Dispatch Thread
		task.prepare();
		// The user message is created and inserted
		// into the database in a background thread
		ProgressWorker.builder()
						.task(task)
						// On the Event Dispatch Thread
						.onException(task::fail)
						// Propagate the resulting task
						// to the next async send method
						.onResult(EntityChatEditModel::send)
						.execute();
	}

	private static void send(ChatResponseTask task) {
		// On the Event Dispatch Thread
		task.prepare();
		// The language model is prompted and the result
		// inserted into the database in a background thread
		ProgressWorker.builder()
						.task(task)
						// On the Event Dispatch Thread
						.onResult(task::finish)
						.execute();
	}

	private static Attachment createAttachment(Path path, MimeType mimeType) {
		return switch (mimeType) {
			case PNG, JPEG -> new Attachment(path,
							ImageContent.from(toBase64Bytes(path), mimeType.type()));
			case PLAIN_TEXT -> new Attachment(path,
							TextContent.from(toString(path)));
			case PDF -> new Attachment(path,
							PdfFileContent.from(toBase64Bytes(path), mimeType.type()));
		};
	}

	private static String toBase64Bytes(Path attachment) {
		try {
			return BASE64_ENCODER.encodeToString(readAllBytes(attachment));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String toString(Path path) {
		try {
			return Files.readString(path);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public record Attachment(Path path, Content content) {

		@Override
		public String toString() {
			return path.toString();
		}
	}

	private final class UserMessageTask implements ResultTask<ChatResponseTask> {

		@Override
		public ChatResponseTask execute() {
			UserMessage userMessage = createMessage();

			return new ChatResponseTask(new Result(insert(userMessage), userMessage));
		}

		private UserMessage createMessage() {
			UserMessage.Builder builder = UserMessage.builder().name(USER);
			prompt.optional()
							.filter(not(String::isBlank))
							.ifPresent(text -> builder.addContent(TextContent.from(text)));
			attachments.items().get().forEach(attachment ->
							builder.addContent(attachment.content()));

			return builder.build();
		}

		private Entity insert(UserMessage message) {
			return connection().insertSelect(entities().entity(Chat.TYPE)
							.with(Chat.MESSAGE_TYPE, ChatMessageType.USER)
							.with(Chat.SESSION, session)
							.with(Chat.NAME, USER)
							.with(Chat.TIMESTAMP, LocalDateTime.now())
							.with(Chat.MESSAGE, messageText(message))
							.with(Chat.JSON, messageToJson(message))
							.build());
		}

		private static String messageText(UserMessage message) {
			return message.contents().stream()
							.filter(TextContent.class::isInstance)
							.map(TextContent.class::cast)
							.map(TextContent::text)
							.collect(joining("\n"));
		}

		// Must be called on the Event Dispatch Thread
		// since this affects one or more UI components
		private void prepare() {
			elapsed.clear();
			started.set(LocalDateTime.now());
			processing.set(true);
			elapsedUpdater.start();
		}

		// Must be called on the Event Dispatch Thread
		// since this affects one or more UI components
		private void fail(Exception exception) {
			elapsedUpdater.stop();
			processing.set(false);
			elapsed.clear();
			started.clear();
		}

		private record Result(Entity entity, UserMessage userMessage) {}
	}

	private final class ChatResponseTask implements ResultTask<Entity> {

		private final UserMessageTask.Result result;

		private ChatResponseTask(UserMessageTask.Result result) {
			this.result = result;
		}

		@Override
		public Entity execute() {
			ChatModel chatModel = chatModel();
			LocalDateTime start = LocalDateTime.now();
			try {
				return insert(chatModel.provider().name(),
								chatModel.chat(result.userMessage()),
								Duration.between(start, LocalDateTime.now()));
			}
			catch (Exception e) {
				return insert(e);
			}
		}

		private Entity insert(String name, ChatResponse response, Duration responseTime) {
			TokenUsage tokenUsage = response.metadata().tokenUsage();

			return connection().insertSelect(entities().entity(Chat.TYPE)
							.with(Chat.MESSAGE_TYPE, ChatMessageType.AI)
							.with(Chat.SESSION, session)
							.with(Chat.NAME, name)
							.with(Chat.TIMESTAMP, LocalDateTime.now())
							.with(Chat.MESSAGE, response.aiMessage().text())
							.with(Chat.RESPONSE_TIME, responseTime)
							.with(Chat.JSON, messageToJson(response.aiMessage()))
							.with(Chat.INPUT_TOKENS, tokenUsage.inputTokenCount())
							.with(Chat.OUTPUT_TOKENS, tokenUsage.outputTokenCount())
							.with(Chat.TOTAL_TOKENS, tokenUsage.totalTokenCount())
							.build());
		}

		private Entity insert(Exception exception) {
			return connection().insertSelect(entities().entity(Chat.TYPE)
							.with(Chat.MESSAGE_TYPE, ChatMessageType.SYSTEM)
							.with(Chat.SESSION, session)
							.with(Chat.NAME, SYSTEM)
							.with(Chat.TIMESTAMP, LocalDateTime.now())
							.with(Chat.MESSAGE, exception.getMessage())
							.with(Chat.STACK_TRACE, stackTrace(exception))
							.build());
		}

		private ChatModel chatModel() {
			return chatModels.selection().item().optional()
							.map(Item::value)
							.orElseThrow();
		}

		// Must be called on the Event Dispatch Thread
		// since this affects one or more UI components
		private void prepare() {
			prompt.clear();
			notifyAfterInsert(List.of(result.entity()));
		}

		// Must be called on the Event Dispatch Thread
		// since this affects one or more UI components
		private void finish(Entity entity) {
			elapsedUpdater.stop();
			processing.set(false);
			elapsed.clear();
			started.clear();
			error.set(SYSTEM.equals(entity.get(Chat.NAME)));
			notifyAfterInsert(List.of(entity));
		}
	}

	private static String stackTrace(Exception exception) {
		StringWriter writer = new StringWriter();
		exception.printStackTrace(new PrintWriter(writer));

		return writer.toString();
	}
}
// end::chat_edit_model[]