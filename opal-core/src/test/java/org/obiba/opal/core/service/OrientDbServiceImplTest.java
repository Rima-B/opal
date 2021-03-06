package org.obiba.opal.core.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.obiba.opal.core.domain.security.SubjectProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static org.fest.assertions.api.Assertions.assertThat;

@ContextConfiguration(classes = OrientDbServiceImplTest.Config.class)
public class OrientDbServiceImplTest  extends AbstractJUnit4SpringContextTests {

  @Autowired
  private OrientDbService orientDbService;

  @Autowired
  private OrientDbServerFactory orientDbServerFactory;

  private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Test
  public void testDateDeserialization () {
    Date date = parseDate("2015-01-01 00:00:00");

    try(ODatabaseDocumentTx tx = orientDbServerFactory.getDocumentTx()) {
      ODocument document = new ODocument(SubjectProfile.class.getSimpleName());
      SubjectProfile profile = orientDbService
          .fromDocument(SubjectProfile.class, document.fromJSON("{\"created\": \""+ df.format(date) +"\"}"));

      assertThat(profile.getCreated().getTime()).isEqualTo(date.getTime());

      profile = orientDbService
          .fromDocument(SubjectProfile.class, document.fromJSON("{\"created\": " + String.valueOf(date.getTime()) + " }"));

      assertThat(profile.getCreated().getTime()).isEqualTo(date.getTime());
    }
  }

  @Test
  public void testDateSerialization() {
    Date date = parseDate("2015-01-01 00:00:00");
    SubjectProfile profile = new SubjectProfile();
    profile.setCreated(date);

    try(ODatabaseDocumentTx tx = orientDbServerFactory.getDocumentTx()) {
      ODocument document = new ODocument(SubjectProfile.class.getSimpleName());
      orientDbService.copyToDocument(profile, document);
      assertThat(document.toJSON())
          .contains("\"" + df.format(date) + "\"");
    }
  }

  private Date parseDate(String date) {
    try {
      return df.parse(date);
    } catch(ParseException e) {
      throw Throwables.propagate(e);
    }
  }

  @Configuration
  public static class Config extends AbstractOrientDbTestConfig {

  }
}
