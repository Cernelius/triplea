package games.strategy.triplea.settings;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * Logic for building UI components that "bind" to ClientSettings.
 * For example, if we have a setting that needs a number, we could create an integer text field with this
 * class. This class takes care of the UI code to ensure we render the proper swing component with validation.
 */
class SelectionComponentFactory {
  static SelectionComponent proxySettings() {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);


    final HttpProxy.ProxyChoice proxyChoice =
        HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));

    final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);

    final JRadioButton systemButton =
        new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);

    final JRadioButton userButton =
        new JRadioButton("Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);


    SwingComponents.createButtonGroup(noneButton, systemButton, userButton);


    final JTextField hostText = new JTextField(ClientSetting.PROXY_HOST.value(), 20);
    final JTextField portText = new JTextField(ClientSetting.PROXY_PORT.value(), 6);

    final JPanel radioPanel = JPanelBuilder.builder()
        .verticalBoxLayout()
        .add(noneButton)
        .add(systemButton)
        .add(userButton)
        .add(new JLabel("Proxy Host: "))
        .add(hostText)
        .add(new JLabel("Proxy Port: "))
        .add(portText)
        .build();

    final ActionListener enableUserSettings = e -> {
      if (userButton.isSelected()) {
        hostText.setEnabled(true);
        hostText.setBackground(Color.WHITE);
        portText.setEnabled(true);
        portText.setBackground(Color.WHITE);
      } else {
        hostText.setEnabled(false);
        hostText.setBackground(Color.DARK_GRAY);
        portText.setEnabled(false);
        portText.setBackground(Color.DARK_GRAY);
      }
    };
    enableUserSettings.actionPerformed(null);
    userButton.addActionListener(enableUserSettings);
    noneButton.addActionListener(enableUserSettings);
    systemButton.addActionListener(enableUserSettings);

    return new SelectionComponent() {

      private static final long serialVersionUID = -8485825527073729683L;

      @Override
      JComponent getJComponent() {
        return radioPanel;
      }

      @Override
      boolean isValid() {
        return !userButton.isSelected() || (isHostTextValid() && isPortTextValid());
      }

      private boolean isHostTextValid() {
        return !Strings.nullToEmpty(hostText.getText()).trim().isEmpty();
      }

      private boolean isPortTextValid() {
        final String value = Strings.nullToEmpty(portText.getText()).trim();
        if (value.isEmpty()) {
          return false;
        }

        try {
          return Integer.parseInt(value) > 0;
        } catch (final NumberFormatException e) {
          return false;
        }
      }


      @Override
      String validValueDescription() {
        return "Proxy host can be a network name or an IP address, port should be number, usually 4 to 5 digits.";
      }

      @Override
      Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> values = new HashMap<>();
        if (noneButton.isSelected()) {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString());
        } else if (systemButton.isSelected()) {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS.toString());
          HttpProxy.updateSystemProxy();
        } else {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.USE_USER_PREFERENCES.toString());
          values.put(ClientSetting.PROXY_HOST, hostText.getText().trim());
          values.put(ClientSetting.PROXY_PORT, portText.getText().trim());
        }
        return values;
      }

      @Override
      void indicateError() {
        if (!isHostTextValid()) {
          hostText.setBackground(Color.RED);
        }
        if (!isPortTextValid()) {
          portText.setBackground(Color.RED);
        }
      }

      @Override
      void clearError() {
        hostText.setBackground(Color.WHITE);
        portText.setBackground(Color.WHITE);

      }

      @Override
      void resetToDefault() {
        ClientSetting.PROXY_CHOICE.restoreToDefaultValue();
        ClientSetting.PROXY_HOST.restoreToDefaultValue();
        ClientSetting.PROXY_PORT.restoreToDefaultValue();
        ClientSetting.flush();
        hostText.setText("");
        portText.setText("");
        noneButton.setSelected(true);
      }
    };
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static SelectionComponent intValueRange(final ClientSetting clientSetting, final int lo, final int hi) {
    final JTextField component = new JTextField(clientSetting.value(), String.valueOf(hi).length());

    return new SelectionComponent() {
      private static final long serialVersionUID = 8195633990481917808L;

      @Override
      JComponent getJComponent() {
        component.setToolTipText(validValueDescription());

        SwingComponents.addTextFieldFocusLostListener(component, () -> {
          if (isValid()) {
            clearError();
          } else {
            indicateError();
          }
        });

        return component;
      }

      @Override
      boolean isValid() {
        final String value = component.getText();

        if (value.trim().isEmpty()) {
          return true;
        }

        try {
          final int intValue = Integer.parseInt(value);
          return intValue >= lo && intValue <= hi;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      String validValueDescription() {
        return "Number between " + lo + " and " + hi;
      }

      @Override
      void indicateError() {
        component.setBackground(Color.RED);
      }

      @Override
      void clearError() {
        component.setBackground(Color.WHITE);
      }

      @Override
      Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> map = new HashMap<>();
        map.put(clientSetting, component.getText());
        return map;
      }

      @Override
      void resetToDefault() {
        clientSetting.restoreToDefaultValue();
        ClientSetting.flush();
        component.setText(clientSetting.value());
      }
    };
  }

  /**
   * yes/no radio buttons.
   */
  static SelectionComponent booleanRadioButtons(final ClientSetting clientSetting) {
    final boolean initialSelection = clientSetting.booleanValue();

    final JRadioButton yesButton = new JRadioButton("True");
    yesButton.setSelected(initialSelection);

    final JRadioButton noButton = new JRadioButton("False");
    noButton.setSelected(!initialSelection);

    SwingComponents.createButtonGroup(yesButton, noButton);

    final JPanel buttonPanel = JPanelBuilder.builder()
        .horizontalBoxLayout()
        .add(yesButton)
        .add(noButton)
        .build();

    return new AlwaysValidInputSelectionComponent() {
      private static final long serialVersionUID = 6104513062312556269L;

      @Override
      JComponent getJComponent() {
        return buttonPanel;
      }

      @Override
      Map<GameSetting, String> readValues() {
        final String value = yesButton.isSelected() ? String.valueOf(true) : String.valueOf(false);
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      void resetToDefault() {
        clientSetting.restoreToDefaultValue();
        ClientSetting.flush();
        yesButton.setSelected(clientSetting.booleanValue());
        noButton.setSelected(!clientSetting.booleanValue());
      }
    };
  }

  /**
   * File selection prompt.
   */
  static SelectionComponent filePath(final ClientSetting clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.FILES);
  }

  /**
   * Folder selection prompt.
   */
  static SelectionComponent folderPath(final ClientSetting clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.DIRECTORIES);
  }

  private static SelectionComponent selectFile(
      final ClientSetting clientSetting,
      final SwingComponents.FolderSelectionMode folderSelectionMode) {
    final int expectedLength = 20;
    final JTextField field = new JTextField(clientSetting.value(), expectedLength);
    field.setEditable(false);

    final JButton button = JButtonBuilder.builder()
        .title("Select")
        .actionListener(
            () -> SwingComponents.showJFileChooser(folderSelectionMode)
                .ifPresent(file -> field.setText(file.getAbsolutePath())))
        .build();

    return new AlwaysValidInputSelectionComponent() {
      private static final long serialVersionUID = -1775099967925891332L;

      @Override
      JComponent getJComponent() {
        return JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(field)
            .add(Box.createHorizontalStrut(10))
            .add(button)
            .build();
      }

      @Override
      Map<GameSetting, String> readValues() {
        final String value = field.getText();
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      void resetToDefault() {
        clientSetting.restoreToDefaultValue();
        ClientSetting.flush();
        field.setText(clientSetting.value());
      }
    };
  }


  static SelectionComponent selectionBox(final ClientSetting clientSetting, final List<String> availableOptions) {
    final JComboBox<String> comboBox = new JComboBox<>(availableOptions.toArray(new String[availableOptions.size()]));
    comboBox.setSelectedItem(clientSetting.value());

    return new AlwaysValidInputSelectionComponent() {
      private static final long serialVersionUID = -8969206423938554118L;

      @Override
      JComponent getJComponent() {
        return comboBox;
      }

      @Override
      Map<GameSetting, String> readValues() {
        final String value = String.valueOf(comboBox.getSelectedItem());
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      void resetToDefault() {
        clientSetting.restoreToDefaultValue();
        ClientSetting.flush();
        comboBox.setSelectedItem(clientSetting.value());
      }
    };
  }

  static SelectionComponent textField(final ClientSetting clientSetting) {
    final JTextField textField = new JTextField(clientSetting.value(), 20);
    return new AlwaysValidInputSelectionComponent() {
      private static final long serialVersionUID = 7549165488576728952L;

      @Override
      JComponent getJComponent() {
        return textField;
      }

      @Override
      Map<GameSetting, String> readValues() {
        final Map<GameSetting,String> map = new HashMap<>();
        map.put(clientSetting, textField.getText());
        return map;
      }

      @Override
      void resetToDefault() {
        clientSetting.restoreToDefaultValue();
        ClientSetting.flush();
        textField.setText(clientSetting.value());
      }
    };
  }


  private abstract static class AlwaysValidInputSelectionComponent extends SelectionComponent {
    private static final long serialVersionUID = 6848335387637901069L;

    @Override
    void indicateError() {
      // no-op, component only allows valid selections
    }

    @Override
    void clearError() {
      // also a no-op
    }

    @Override
    boolean isValid() {
      return true;
    }

    @Override
    String validValueDescription() {
      return "";
    }
  }

}
