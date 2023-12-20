package uk.co.fivium.digitalnotificationlibrary.configuration;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

@AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)
class NotificationLibraryEntityAutoConfiguration implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                      @NonNull BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(registry, "uk.co.fivium.digitalnotificationlibrary");
  }
}
