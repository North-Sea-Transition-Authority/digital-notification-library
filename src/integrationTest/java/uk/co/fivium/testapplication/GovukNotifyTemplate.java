package uk.co.fivium.testapplication;

enum GovukNotifyTemplate {
  // https://www.notifications.service.gov.uk/services/95f4e6d8-0261-40d2-89cc-b79049346011/templates/f71c347d-4d63-4721-aeb0-785f31708878
  EMAIL_TEMPLATE("f71c347d-4d63-4721-aeb0-785f31708878"),
  // https://www.notifications.service.gov.uk/services/95f4e6d8-0261-40d2-89cc-b79049346011/templates/737db34d-2392-41cb-95c8-ffdcd2062387
  SMS_TEMPLATE("737db34d-2392-41cb-95c8-ffdcd2062387");

  private final String govukNotifyTemplateId;

  GovukNotifyTemplate(String govukNotifyTemplateId) {
    this.govukNotifyTemplateId = govukNotifyTemplateId;
  }

  public String getGovukNotifyTemplateId() {
    return govukNotifyTemplateId;
  }
}
