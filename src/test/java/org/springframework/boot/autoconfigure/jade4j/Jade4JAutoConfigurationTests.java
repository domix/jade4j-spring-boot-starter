/**
 *
 * Copyright (C) 2014 Domingo Suarez Torres <domingo.suarez@gmail.com>
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
package org.springframework.boot.autoconfigure.jade4j;

import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.spring.view.JadeView;
import de.neuland.jade4j.spring.view.JadeViewResolver;
import de.neuland.jade4j.template.JadeTemplate;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.support.RequestContext;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Jade4JAutoConfiguration}.
 *
 * @author Domingo Suarez Torres
 */
public class Jade4JAutoConfigurationTests {
  private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

  @After
  public void close() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void shouldRenderTemplateAsExpected() throws Exception {
    EnvironmentTestUtils.addEnvironment(this.context, "spring.jade4j.mode:XHTML");
    this.context.register(Jade4JAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    JadeConfiguration engine = this.context.getBean(JadeConfiguration.class);
    JadeTemplate template = engine.getTemplate("demo.jade");
    Map<String, Object> params = Collections.emptyMap();
    String result = engine.renderTemplate(template, params);
    String expected = "<html><head><title>Jade</title></head><body><h1>Jade - Template engine</h1></body></html>";

    assertEquals(expected, result);
  }

  @Test
  public void shouldRenderTemplateWithParams() throws Exception {
    EnvironmentTestUtils.addEnvironment(this.context, "spring.jade4j.mode:XHTML");
    this.context.register(Jade4JAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    JadeConfiguration engine = this.context.getBean(JadeConfiguration.class);
    JadeTemplate template = engine.getTemplate("demo.jade");
    Map<String, Object> params = params();
    String result = engine.renderTemplate(template, params);
    String expected = "<html><head><title>Jade</title></head><body><h1>Jade - Template engine</h1><h2>With user</h2></body></html>";
    assertEquals(expected, result);
  }

  @Test
  public void shouldRenderPrettyTemplateTemplate() throws Exception {
    EnvironmentTestUtils.addEnvironment(this.context, "spring.jade4j.prettyPrint:true");
    this.context.register(Jade4JAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    JadeConfiguration engine = this.context.getBean(JadeConfiguration.class);
    JadeTemplate template = engine.getTemplate("demo.jade");
    Map<String, Object> params = Collections.emptyMap();
    String result = engine.renderTemplate(template, params);
    String expected = "<html>\n" +
        "  <head>\n" +
        "    <title>Jade</title>\n" +
        "  </head>\n" +
        "  <body>\n" +
        "    <h1>Jade - Template engine</h1>\n" +
        "  </body>\n" +
        "</html>";
    assertEquals(expected, result);
  }


  @Test(expected = BeanCreationException.class)
  public void templateLocationDoesNotExist() throws Exception {
    EnvironmentTestUtils.addEnvironment(this.context, "spring.jade4j.prefix:classpath:/no-such-directory/");
    this.context.register(Jade4JAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
  }

  @Test
  @Ignore
  public void templateLocationEmpty() throws Exception {
    new File("./build/classes/test/templates/empty-directory").mkdir();
    EnvironmentTestUtils.addEnvironment(this.context, "spring.jade4j.prefix:classpath:/templates/empty-directory/");
    this.context.register(Jade4JAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
  }

  @Test
  public void createLayoutFromConfigClass() throws Exception {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(Jade4JAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    MockServletContext servletContext = new MockServletContext();
    context.setServletContext(servletContext);
    context.refresh();

    JadeView view = (JadeView) context.getBean(JadeViewResolver.class).resolveViewName("demo", Locale.UK);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    view.render(params(), request, response);
    String result = response.getContentAsString();
    assertTrue("Wrong result: " + result, result.contains("<title>Jade</title>"));
    assertTrue("Wrong result: " + result, result.contains("<h2>With user</h2>"));
    context.close();
  }

  @Test
  @Ignore
  public void renderNonWebAppTemplate() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
        Jade4JAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class);
    assertEquals(0, context.getBeanNamesForType(ViewResolver.class).length);
    try {
      JadeConfiguration engine = this.context.getBean(JadeConfiguration.class);
      JadeTemplate template = engine.getTemplate("demo.jade");
      Map<String, Object> params = params();
      String result = engine.renderTemplate(template, params);

      assertThat(result, containsString("With user"));
    } finally {
      context.close();
    }
  }

  private Map<String, Object> params() {
    HashMap<String, Object> result = new HashMap<String, Object>();
    result.put("user", "domix");
    return result;
  }

}
