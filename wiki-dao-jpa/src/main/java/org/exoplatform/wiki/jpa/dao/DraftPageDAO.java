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

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wiki.jpa.entity.DraftPage;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 24, 2015  
 */
public class DraftPageDAO extends GenericDAOJPAImpl<DraftPage, Long> {

  public List<DraftPage> findDraftPagesByUser(String username) {
    TypedQuery<DraftPage> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPagesByUser", DraftPage.class)
            .setParameter("username", username);
    return query.getResultList();
  }

  public DraftPage findLatestDraftPageByUser(String username) {
    TypedQuery<DraftPage> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPagesByUser", DraftPage.class)
            .setParameter("username", username).setMaxResults(1);
    List<DraftPage> draftPages = query.getResultList();
    return draftPages.size() > 0 ? draftPages.get(0) : null;
  }

  public List<DraftPage> findDraftPagesByUserAndTargetPage(String username, long targetPageId) {
    TypedQuery<DraftPage> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPageByUserAndTargetPage", DraftPage.class)
            .setParameter("username", username)
            .setParameter("targetPageId", targetPageId);
    return query.getResultList();
  }

  public void deleteDraftPagesByUserAndTargetPage(String username, long targetPageId) {
    EntityTransaction trans = getEntityManager().getTransaction();
    boolean active = false;
    if (!trans.isActive()) {
      trans.begin();
      active = true;
    }

    Query query = getEntityManager().createNamedQuery("wikiDraftPage.deleteDraftPagesByUserAndTargetPage")
            .setParameter("username", username)
            .setParameter("targetPageId", targetPageId);
    query.executeUpdate();

    if (active) {
      trans.commit();
    }
  }

  public void deleteDraftPagesByUserAndName(String draftName, String username) {
    EntityTransaction trans = getEntityManager().getTransaction();
    boolean active = false;
    if (!trans.isActive()) {
      trans.begin();
      active = true;
    }

    Query query = getEntityManager().createNamedQuery("wikiDraftPage.deleteDraftPagesByUserAndName")
            .setParameter("username", username)
            .setParameter("draftPageName", draftName);
    query.executeUpdate();

    if (active) {
      trans.commit();
    }
  }
}
