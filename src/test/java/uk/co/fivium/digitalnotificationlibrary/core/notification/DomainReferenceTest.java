package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainReferenceTest {

  @Test
  void from_verifyResultingDomainReference() {

    var resultingDomainReference = DomainReference.from("id", "type");

    assertThat(resultingDomainReference)
        .extracting(DomainReference::getId, DomainReference::getType)
        .contains("id", "type");
  }
}
