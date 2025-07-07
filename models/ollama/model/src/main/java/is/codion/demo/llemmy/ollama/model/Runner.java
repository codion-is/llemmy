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
package is.codion.demo.llemmy.ollama.model;

import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import org.testcontainers.containers.GenericContainer;

import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static is.codion.demo.llemmy.ollama.Runner.*;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.Utilities.setClipboard;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeel;
import static java.lang.String.format;
import static javax.swing.SwingConstants.CENTER;

// tag::runner[]
public final class Runner {

	private Runner() {}

	public static void main(String[] args) {
		findLookAndFeel(FlatDarkLaf.class).ifPresent(LookAndFeelEnabler::enable);
		try (var ollama = new GenericContainer<>("langchain4j/ollama-" + model() + ":latest")
						.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(new HostConfig()
										.withPortBindings(new PortBinding(bindPort(PORT), new ExposedPort(PORT)))))) {
			ollama.start();
			var baseUrlField = stringField()
							.value(format("http://%s:%d", ollama.getHost(), PORT))
							.selectAllOnFocusGained(true)
							.columns(25)
							.editable(false)
							.horizontalAlignment(CENTER)
							.build();
			Dialogs.action()
							.component(borderLayoutPanel()
											.border(emptyBorder())
											.northComponent(label()
															.text(ollama.getDockerImageName())
															.horizontalAlignment(CENTER)
															.build())
											.centerComponent(baseUrlField)
											.build())
							.title("Ollama")
							.defaultAction(Control.builder()
											.command(() -> setClipboard(baseUrlField.getText()))
											.caption("Copy")
											.mnemonic('C')
											.build())
							.escapeAction(Control.builder()
											.command(() -> parentWindow(baseUrlField).dispose())
											.caption("Stop")
											.mnemonic('S')
											.build())
							.show();
			ollama.stop();
		}
	}

	private static String model() {
		return Dialogs.input()
						.component(comboBox()
										.model(FilterComboBoxModel.builder()
										.items(MODELS)
										.build())
										.value(ORCA_MINI)
										.preferredWidth(250))
						.title("Select model")
						.show();
	}
}
// end::runner[]