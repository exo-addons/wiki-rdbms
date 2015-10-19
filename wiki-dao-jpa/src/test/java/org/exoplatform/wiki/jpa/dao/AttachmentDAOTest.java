/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.jpa.entity.AttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.mow.api.PermissionType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public class AttachmentDAOTest extends BaseTest {

  public void testInsertDelete() throws IOException, URISyntaxException {
    //Given
    URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    AttachmentDAO attachmentDAO = getService(AttachmentDAO.class);
    AttachmentEntity att = new AttachmentEntity();
    att.setContent(Files.readAllBytes(Paths.get(fileResource.toURI())));
    //When
    attachmentDAO.create(att);
    Long id = att.getId();
    //Then
    AttachmentEntity got = attachmentDAO.find(id);
    assertNotNull(got.getContent());
    assertEquals(new File(fileResource.toURI()).length(), got.getWeightInBytes());
    //Delete
    attachmentDAO.delete(att);
    assertNull(attachmentDAO.find(id));
  }

  public void testUpdate() throws IOException, URISyntaxException {
    //Given
    URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
    AttachmentDAO attachmentDAO = getService(AttachmentDAO.class);
    AttachmentEntity att = new AttachmentEntity();
    att.setContent(Files.readAllBytes(Paths.get(fileResource.toURI())));
    //When
    attachmentDAO.create(att);
    Long id = att.getId();
    PermissionEntity per = new PermissionEntity();
    per.setIdentity("user");
    per.setPermissionType(PermissionType.ADMINPAGE);
    List<PermissionEntity> permissions = new ArrayList<>();
    permissions.add(per);
    att.setPermissions(permissions);
    att.setCreator("creator");
    att.setDownloadURL("http://exoplatform.com");
    att.setTitle("title");
    Date date = new Date();
    att.setUpdatedDate(date);
    //Then
    attachmentDAO.update(att);
    AttachmentEntity got = attachmentDAO.find(id);
    assertEquals("http://exoplatform.com", got.getDownloadURL());
    assertEquals("title", got.getTitle());
    assertEquals("creator", got.getCreator());
    assertEquals(date, got.getUpdatedDate());
    PermissionEntity got_per = got.getPermissions().get(0);
    assertEquals("user", got_per.getIdentity());
  }
}
