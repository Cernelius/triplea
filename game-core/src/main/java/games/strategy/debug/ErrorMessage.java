package games.strategy.debug;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.triplea.settings.ClientSetting;
import swinglib.JButtonBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;

/**
 * Class for showing a modal error dialog to the user. The dialog has an 'ok' button to close it and a 'show details'
 * that will bring up the error console.
 * <p>
 * Note on threading: If we get an error while EDT thread is lock, we will not be able to create a new window.
 * If we do it tries to grab an EDT lock and we get into a deadlock situation. To avoid this we create the error
 * message window early and then show/hide it as needed.
 * </p>
 * <p>
 * Async behavior note: once the window is displayed, further error messages are ignored. The error message is intended
 * to be user friendly, clicking 'show details' would show full details of all error messages.
 * </p>
 */
@SuppressWarnings("ImmutableEnumChecker") // Enum singleton pattern
public enum ErrorMessage {
  INSTANCE;

  private final JFrame windowReference = new JFrame("TripleA Error");
  private final JLabel errorMessage = JLabelBuilder.builder().errorIcon().iconTextGap(10).build();
  private final AtomicBoolean isVisible = new AtomicBoolean(false);
  private volatile boolean enableErrorPopup = false;

  ErrorMessage() {
    windowReference.setAlwaysOnTop(true);
    windowReference.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
    windowReference.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        hide();
      }
    });
    windowReference.add(JPanelBuilder.builder()
        .borderLayout()
        .borderEmpty(10)
        .addCenter(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .addHorizontalGlue()
            .add(errorMessage)
            .addHorizontalGlue()
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .borderEmpty(20, 0, 0, 0)
            .addHorizontalGlue()
            .add(JButtonBuilder.builder()
                .okTitle()
                .actionListener(this::hide)
                .selected(true)
                .build())
            .addHorizontalStrut(5)
            .add(JButtonBuilder.builder()
                .title("Show Details")
                .toolTip("Shows the error console window with full error details.")
                .actionListener(() -> {
                  hide();
                  ClientSetting.SHOW_CONSOLE.saveAndFlush("true");
                })
                .build())
            .addHorizontalGlue()
            .build())
        .build());
  }

  /**
   * Set this to true on non-headless environments to actively notify user of errors via a pop-up message.
   */
  public static void enable() {
    Preconditions.checkState(
        !GraphicsEnvironment.isHeadless(),
        "Error, must not enable error pop-up in a headless environment, there will be errors rendering "
            + "swing components. Check the call flow to this point and make sure we do not enable error reporting "
            + "unless we are in a non-headless environment");
    INSTANCE.enableErrorPopup = true;
  }

  public static void show(final LogRecord record) {
    if (INSTANCE.enableErrorPopup && INSTANCE.isVisible.compareAndSet(false, true)) {
      SwingUtilities.invokeLater(() -> {
        INSTANCE.errorMessage.setText(TextUtils.textToHtml(Strings.nullToEmpty(record.getMessage())));
        INSTANCE.windowReference.pack();
        INSTANCE.windowReference.setLocationRelativeTo(null);
        INSTANCE.windowReference.setVisible(true);
      });
    }
  }

  private void hide() {
    windowReference.setVisible(false);
    isVisible.set(false);
  }
}
