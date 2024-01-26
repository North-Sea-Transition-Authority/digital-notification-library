package uk.co.fivium.testapplication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = TestApplication.class)
@Import(IntegrationTestConfiguration.class)
// There seems to be an issue relating to spring not stopping the
// scheduler between test class runs. Adding this to enforce a tear down
// to avoid the wrong GovukNotifySender bean getting included when the library
// mode changes.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface IntegrationTest {
}
