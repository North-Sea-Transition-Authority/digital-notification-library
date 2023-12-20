package uk.co.fivium.testapplication;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.fivium.digitalnotificationlibrary.core.notification.TemplateType;

class NotifyTemplateTestUtil {

  static Builder builder() {
    return new Builder();
  }

  static class Builder {

    private UUID id = UUID.randomUUID();

    private String name = "template-name";

    private String type = "email";

    private ZonedDateTime createdAt = ZonedDateTime.now();

    private String version = "1";

    private String body = "template-body";

    private String subject = "template-subject";

    private Map<String, Object> personalisation = new HashMap<>();

    Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    Builder withId(String id) {
      this.id = UUID.fromString(id);
      return this;
    }

    Builder withName(String name) {
      this.name = name;
      return this;
    }

    Builder withType(String type) {
      this.type = type;
      return this;
    }

    Builder withType(TemplateType type) {
      this.type = type.getNotifyTemplateType();
      return this;
    }

    Builder withCreatedAt(ZonedDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    Builder withBody(String body) {
      this.body = body;
      return this;
    }

    Builder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    Builder withPersonalisation(String key, Object value) {
      this.personalisation.put(key, value);
      return this;
    }
    
    Builder withPersonalisation(Map<String, Object> personalisation) {
      this.personalisation = personalisation;
      return this;
    }

    JSONObject buildAsJsonObject() {
      var templateJson = new JSONObject();
      try {

        templateJson.put("id", String.valueOf(id));
        templateJson.put("name", name);
        templateJson.put("type", type);

        templateJson.put("created_at", String.valueOf(createdAt));

        templateJson.put("version", version);
        templateJson.put("body", body);
        templateJson.put("subject", subject);
        templateJson.put("personalisation", personalisation);
      } catch (JSONException exception) {
        throw new IllegalArgumentException("Failed to construct json object for template");
      }

      return templateJson;
    }
  }
}
