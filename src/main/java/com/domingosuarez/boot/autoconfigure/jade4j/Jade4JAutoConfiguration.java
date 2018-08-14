/**
 *
 * Copyright (C) 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.domingosuarez.boot.autoconfigure.jade4j;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.spring.template.SpringTemplateLoader;
import de.neuland.jade4j.spring.view.JadeViewResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;
import java.util.Map;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for jade4j.
 *
 * @author Domingo Suarez Torres
 */
@Configuration
@ConditionalOnClass(SpringTemplateLoader.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class Jade4JAutoConfiguration {
  public static final String DEFAULT_PREFIX = "classpath:/templates/";

  public static final String DEFAULT_SUFFIX = ".jade";

  @Configuration
  @ConditionalOnMissingBean(name = "defaultSpringTemplateLoader")
  public static class DefaultTemplateResolverConfiguration implements EnvironmentAware {

    @Autowired
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    @PostConstruct
    public void checkTemplateLocationExists() {
      Boolean checkTemplateLocation = this.environment.getProperty("spring.jade4j.checkTemplateLocation", Boolean.class, true);
      if (checkTemplateLocation) {
        Resource resource = this.resourceLoader.getResource(this.environment.getProperty("spring.jade4j.prefix", DEFAULT_PREFIX));
        Assert.state(resource.exists(), "Cannot find template location: "
          + resource + " (please add some templates or check your jade4j configuration)");
      }
    }

    @Bean
    public SpringTemplateLoader defaultSpringTemplateLoader() {
      SpringTemplateLoader resolver = new SpringTemplateLoader();

      resolver.setBasePath(this.environment.getProperty("spring.jade4j.prefix", DEFAULT_PREFIX));
      resolver.setSuffix(this.environment.getProperty("spring.jade4j.suffix", DEFAULT_SUFFIX));
      resolver.setEncoding(this.environment.getProperty("spring.jade4j.encoding", "UTF-8"));
      return resolver;
    }

    @Bean
    public JadeConfiguration defaultJadeConfiguration() {
      JadeConfiguration configuration = new JadeConfiguration();
      configuration.setCaching(this.environment.getProperty("spring.jade4j.caching", Boolean.class, true));
      configuration.setTemplateLoader(defaultSpringTemplateLoader());
      configuration.setPrettyPrint(this.environment.getProperty("spring.jade4j.prettyPrint", Boolean.class, false));
      configuration.setMode(this.environment.getProperty("spring.jade4j.mode", Jade4J.Mode.class, Jade4J.Mode.HTML));
      return configuration;
    }

  }


  @Configuration
  @ConditionalOnClass({Servlet.class})
  @ConditionalOnWebApplication
  protected static class Jade4JViewResolverConfiguration implements EnvironmentAware {

    private Environment environment;

    @Autowired
    private JadeConfiguration jadeConfiguration;

    @Autowired
    private SpringTemplateLoader templateEngine;

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jade4jViewResolver")
    public JadeViewResolver jade4jViewResolver() {
      JadeViewResolver resolver = new JadeViewResolver();
      resolver.setConfiguration(jadeConfiguration);

      resolver.setContentType(appendCharset(
        this.environment.getProperty("spring.jade4j.contentType", "text/html"),
        templateEngine.getEncoding()));

      resolver.setViewNames(this.environment.getProperty("spring.jade4j.viewNames", String[].class));
      // This resolver acts as a fallback resolver (e.g. like a
      // InternalResourceViewResolver) so it needs to have low precedence
      resolver.setOrder(this.environment.getProperty("spring.jade4j.resolver.order", Integer.class, Ordered.LOWEST_PRECEDENCE - 50));
      return resolver;
    }

    @Bean
    public BeanPostProcessor jade4jBeanPostProcessor() {
      return new BeanPostProcessor() {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
          return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
          JadeHelper annotation = AnnotationUtils.findAnnotation(bean.getClass(), JadeHelper.class);
          if (annotation != null) {
            Map<String, Object> variables = jadeConfiguration.getSharedVariables();
            variables.put(beanName, bean);
            jadeConfiguration.setSharedVariables(variables);
          }

          return bean;
        }
      };
    }


    private String appendCharset(String type, String charset) {
      if (type.contains("charset=")) {
        return type;
      }
      return type + ";charset=" + charset;
    }

  }

}
