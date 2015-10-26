/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wiki.mow.api.Page;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity
@ExoEntity
@Table(name = "WIKI_PAGE_VERSIONS")
@NamedQueries({
        @NamedQuery(name = "wikiPageVersion.getLastversionNumberOfPage", query = "SELECT max(p.versionNumber) FROM PageVersionEntity p  WHERE p.page.id = :pageId"),
        @NamedQuery(name = "wikiPageVersion.getPageversionByPageIdAndVersion", query = "SELECT p FROM PageVersionEntity p  WHERE p.page.id = :pageId AND p.versionNumber = :versionNumber")
})
public class PageVersionEntity extends BasePageEntity {
  @Id
  @Column(name = "PAGE_VERSION_ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @ManyToOne
  @JoinColumn(name = "PAGE_ID")
  private PageEntity page;

  @Column(name = "VERSION_NUMBER")
  private long versionNumber;

  public long getId() {
    return id;
  }

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }

  public long getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(long versionNumber) {
    this.versionNumber = versionNumber;
  }
}
