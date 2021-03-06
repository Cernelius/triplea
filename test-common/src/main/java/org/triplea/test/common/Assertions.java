package org.triplea.test.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/**
 * A collection of utility methods that support asserting conditions in tests.
 *
 * <p>
 * Unless otherwise noted, a failed assertion will throw an {@link AssertionFailedError} or a subclass thereof.
 * </p>
 */
public final class Assertions {
  private Assertions() {}

  /**
   * Asserts that execution of the supplied executable does not throw an exception.
   *
   * <p>
   * If an exception is thrown, this method will fail.
   * </p>
   */
  public static void assertNotThrows(final Executable executable) {
    checkNotNull(executable);

    try {
      executable.execute();
    } catch (final Throwable t) {
      throw new AssertionFailedError(
          String.format("Expected no exception to be thrown, but %s was thrown", getCanonicalName(t.getClass())));
    }
  }

  private static String getCanonicalName(final Class<?> type) {
    return Optional.ofNullable(type.getCanonicalName()).orElseGet(type::getName);
  }
}
