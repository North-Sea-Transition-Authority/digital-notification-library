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
// There seems to be an issue when changing application properties that
// change the mode the library runs in. The @Scheduled job in the NotificationProcessor bean
// seems to run with the production mode bean then on the next iteration run with the test mode bean.
// As a result you can ge inconsistent failures if the wrong bean is used on that iteration.
// Some further investigation will need to be done as to why this is happening and how we can remove the context
// rest after each test class
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@interface IntegrationTest {
}
