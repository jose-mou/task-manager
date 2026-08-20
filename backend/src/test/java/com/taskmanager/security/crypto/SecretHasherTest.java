package com.taskmanager.security.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretHasherTest {

	private final SecretHasher hasher = new SecretHasher();

	@Test
	void hashIsHexEncodedSha256() {
		String hash = hasher.hash("13DDBdX0UGRYiyKkg1Gm_XOMpE8Q4nzJZ4Ok70uHciY");

		assertThat(hash).matches("[0-9a-f]{64}");
	}

	@Test
	void hashingTheSameSecretTwiceIsDeterministic() {
		String secret = "some-secret-value";

		assertThat(hasher.hash(secret)).isEqualTo(hasher.hash(secret));
	}

	@Test
	void matchesReturnsTrueForTheCorrectSecret() {
		String secret = "13DDBdX0UGRYiyKkg1Gm_XOMpE8Q4nzJZ4Ok70uHciY";
		String storedHash = hasher.hash(secret);

		assertThat(hasher.matches(secret, storedHash)).isTrue();
	}

	@Test
	void matchesReturnsFalseForAWrongSecret() {
		String storedHash = hasher.hash("the-real-secret");

		assertThat(hasher.matches("a-different-secret", storedHash)).isFalse();
	}

	@Test
	void matchesReturnsFalseInsteadOfThrowingForAMalformedStoredHash() {
		assertThat(hasher.matches("anything", "not-hex-at-all")).isFalse();
	}

	@Test
	void matchesReturnsFalseForNullInputs() {
		assertThat(hasher.matches(null, "aabb")).isFalse();
		assertThat(hasher.matches("secret", null)).isFalse();
	}

}
