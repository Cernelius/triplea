package games.strategy.engine.delegate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.test.common.Assertions.assertNotThrows;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import games.strategy.util.Interruptibles;

public final class DelegateExecutionManagerTest {
  private final DelegateExecutionManager delegateExecutionManager = new DelegateExecutionManager();

  @Test
  public void shouldAllowSerialExecutionOfDelegatesOnSameThread() {
    // given: a delegate is executed on the current thread and runs to completion
    delegateExecutionManager.enterDelegateExecution();
    delegateExecutionManager.leaveDelegateExecution();

    // when: a second delegate is executed on the current thread after the first delegate's execution ends
    // then: no exception should be thrown to prevent the second delegate's execution
    assertNotThrows(delegateExecutionManager::enterDelegateExecution);
  }

  @Test
  public void shouldNotAllowNestedExecutionOfDelegatesOnSameThread() {
    // given: a delegate is executed on the current thread
    delegateExecutionManager.enterDelegateExecution();

    // when: a second delegate is executed on the current thread while the first delegate is still running
    // then: an exception should be thrown to prevent the second delegate's execution
    assertThrows(IllegalStateException.class, delegateExecutionManager::enterDelegateExecution);
  }

  @Test
  public void shouldAllowConcurrentExecutionOfDelegatesOnDifferentThreads() throws Exception {
    // given: a delegate is executed on some thread
    final CountDownLatch testCompleteLatch = new CountDownLatch(1);
    final CountDownLatch delegate1RunningLatch = new CountDownLatch(1);
    final Thread delegate1Thread = new Thread(() -> {
      delegateExecutionManager.enterDelegateExecution();
      delegate1RunningLatch.countDown();
      Interruptibles.await(testCompleteLatch);
    });
    delegate1Thread.start();
    delegate1RunningLatch.await();

    // when: a second delegate is executed on a different thread while the first delegate is still running
    // then: no exception should be thrown to prevent the second delegate's execution
    assertNotThrows(delegateExecutionManager::enterDelegateExecution);

    testCompleteLatch.countDown();
    delegate1Thread.join();
  }
}
