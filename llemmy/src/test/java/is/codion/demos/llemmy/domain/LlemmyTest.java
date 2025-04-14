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

import is.codion.demos.llemmy.domain.Llemmy.ChatLog;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.framework.domain.test.DomainTest;

import dev.langchain4j.data.message.ChatMessageType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

final class LlemmyTest extends DomainTest {

	public LlemmyTest() {
		super(new Llemmy(), LlemmyEntityFactory::new);
	}

	@Test
	void test() {
		test(ChatLog.TYPE);
	}

	private static class LlemmyEntityFactory extends DefaultEntityFactory {

		private LlemmyEntityFactory(EntityConnection connection) {
			super(connection);
		}

		@Override
		protected <T> T value(Attribute<T> attribute) {
			if (attribute.equals(ChatLog.MESSAGE_TYPE)) {
				return (T) ChatMessageType.AI;
			}
			if (attribute.equals(ChatLog.SESSION)) {
				return (T) UUID.randomUUID();
			}
			if (attribute.equals(ChatLog.JSON)) {
				return null;
			}

			return super.value(attribute);
		}
	}
}
