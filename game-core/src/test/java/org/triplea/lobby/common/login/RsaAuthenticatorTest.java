package org.triplea.lobby.common.login;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.security.TestSecurityUtils;

public final class RsaAuthenticatorTest {
  private RsaAuthenticator rsaAuthenticator;

  @BeforeEach
  public void setUp() throws Exception {
    rsaAuthenticator = new RsaAuthenticator(TestSecurityUtils.loadRsaKeyPair());
  }

  @Test
  public void testCanProcess() {
    assertTrue(RsaAuthenticator.canProcessChallenge(singletonMap(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY, "")));
    assertTrue(RsaAuthenticator.canProcessResponse(singletonMap(LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD, "")));

    // Adding a completely unrelated key shouldn't change the outcome
    assertFalse(RsaAuthenticator.canProcessResponse(singletonMap("someOtherResponseKey", "")));
    assertFalse(RsaAuthenticator.canProcessChallenge(singletonMap("someOtherChallengeKey", "")));
  }

  @Test
  public void testRoundTripPassword() {
    final String password = "password";
    final Map<String, String> challenge = new HashMap<>();
    final Map<String, String> response = new HashMap<>();
    @SuppressWarnings("unchecked")
    final Function<String, String> action = mock(Function.class);

    challenge.putAll(rsaAuthenticator.newChallenge());
    response.putAll(RsaAuthenticator.newResponse(challenge, password));
    rsaAuthenticator.decryptPasswordForAction(response, action);

    verify(action).apply(RsaAuthenticator.hashPasswordWithSalt(password));
  }
}
