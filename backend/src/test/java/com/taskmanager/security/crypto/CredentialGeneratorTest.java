package com.taskmanager.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialGeneratorTest {

	private final CredentialGenerator generator = new CredentialGenerator();

	@Test
	void generatesA43CharacterBase64UrlValueWithoutPadding() {
		String value = generator.generate();

		assertThat(value).matches("^[A-Za-z0-9_-]{43}$");
	}

	@Test
	void generatesADifferentValueEachTime() {
		String first = generator.generate();
		String second = generator.generate();

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void generatedValuesAreUniqueAcrossManyCalls() {
		long distinctValues = IntStream.range(0, 500).mapToObj(i -> generator.generate()).distinct().count();

		assertThat(distinctValues).isEqualTo(500);
	}

}
