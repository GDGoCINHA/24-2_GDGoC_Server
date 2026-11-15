package inha.gdgoc.domain.homecoming.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HomecomingCsvWriterTest {

  @Test
  void neutralizesFormulaAndQuotes() {
    assertThat(HomecomingCsvWriter.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
    assertThat(HomecomingCsvWriter.escape("홍길동, 주니어")).isEqualTo("\"홍길동, 주니어\"");
    assertThat(HomecomingCsvWriter.escape("010-1234-5678")).isEqualTo("010-1234-5678");
    assertThat(HomecomingCsvWriter.escape(null)).isEmpty();
  }
}
