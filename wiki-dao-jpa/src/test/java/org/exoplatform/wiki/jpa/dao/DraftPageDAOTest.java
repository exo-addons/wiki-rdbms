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
import org.junit.Test;

import org.exoplatform.wiki.jpa.entity.DraftPage;
import org.exoplatform.wiki.jpa.entity.Page;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
public class DraftPageDAOTest extends BaseTest {

  @Test
  public void testInsert(){
    DraftPageDAO dao = new DraftPageDAO();
    PageDAO pageDAO = new PageDAO();
    DraftPage dp = new DraftPage();
    Page page = new Page();
    page.setName("name");
    dp.setTargetPage(page);
    dao.create(dp);
    
    assertNotNull(dao.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    
    DraftPage got =dao.find(dp.getId());
    got.getTargetPage().setName("name1");
    dao.update(got);
    assertEquals("name1",page.getName());
    dao.deleteAll();
    pageDAO.deleteAll();
    
    assertNull(dao.find(dp.getId()));
  }
}