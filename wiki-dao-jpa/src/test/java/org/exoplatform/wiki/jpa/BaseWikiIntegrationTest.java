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

package org.exoplatform.wiki.jpa;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.rest.RestController;

import org.exoplatform.addons.es.index.IndexingOperationProcessor;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.persistence.impl.ChangeLogsPlugin;
import org.exoplatform.commons.persistence.impl.LiquibaseDataInitializer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.wiki.jpa.dao.*;
import org.exoplatform.wiki.jpa.entity.AttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.jpa.search.AttachmentIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 10/1/15
 */
public abstract class BaseWikiIntegrationTest extends BaseWikiJPAIntegrationTest {
  protected Node node;
  protected IndexingService indexingService;
  protected IndexingOperationProcessor indexingOperationProcessor;
  protected JPADataStorage storage;

  public void setUp() {
    super.setUp();
    //Init ES
    ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder()
            .put(RestController.HTTP_JSON_ENABLE, true)
            .put(InternalNode.HTTP_ENABLED, true)
            .put("network.host", "127.0.0.1")
            .put("path.data", "target/data")
            .put("plugins." + PluginsService.LOAD_PLUGIN_FROM_CLASSPATH, true);
    node = nodeBuilder()
            .local(true)
            .settings(elasticsearchSettings.build())
            .node();
    node.client().admin().cluster().prepareHealth()
            .setWaitForYellowStatus().execute().actionGet();
    assertNotNull(node);
    assertFalse(node.isClosed());
    //Init services
    indexingService = PortalContainer.getInstance().getComponentInstanceOfType(IndexingService.class);
    indexingOperationProcessor = PortalContainer.getInstance().getComponentInstanceOfType(IndexingOperationProcessor.class);
    storage = PortalContainer.getInstance().getComponentInstanceOfType(JPADataStorage.class);
    //Init data
    deleteAllDocumentsInES();
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
  }

  private void deleteAllDocumentsInES() {
    indexingService.unindexAll(WikiIndexingServiceConnector.TYPE);
    indexingService.unindexAll(WikiPageIndexingServiceConnector.TYPE);
    indexingService.unindexAll(AttachmentIndexingServiceConnector.TYPE);
  }

  public void tearDown() {
    super.tearDown();
    //Close ES Node
    node.close();
  }

  protected WikiEntity indexWiki(String name) throws NoSuchFieldException, IllegalAccessException {
    WikiEntity wiki = new WikiEntity();
    wiki.setName(name);
    wiki.setOwner("BCH");
    wiki = wikiDAO.create(wiki);
    assertNotEquals(wiki.getId(), 0);
    indexingService.index(WikiIndexingServiceConnector.TYPE, Long.toString(wiki.getId()));
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    return wiki;
  }

  protected PageEntity indexPage(String name, String title, String content, String comment)
          throws NoSuchFieldException, IllegalAccessException {
    PageEntity page = new PageEntity();
    page.setName(name);
    page.setTitle(title);
    page.setContent(content);
    page.setComment(comment);
    page.setOwner("BCH");
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page = pageDAO.create(page);
    assertNotEquals(page.getId(), 0);
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, Long.toString(page.getId()));
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    return page;
  }

  protected void indexAttachment(String title, String filePath, String downloadedUrl)
          throws NoSuchFieldException, IllegalAccessException, IOException {
    AttachmentEntity attachment = new AttachmentEntity();
    attachment.setDownloadURL(downloadedUrl);
    attachment.setTitle(title);
    attachment.setContent(Files.readAllBytes(Paths.get(filePath)));
    attachment.setCreatedDate(new Date());
    attachment.setUpdatedDate(new Date());
    attachment.setCreator("BCH");
    attachment = attachmentDAO.create(attachment);
    assertEquals(1, attachmentDAO.findAll().size());
    assertNotEquals(attachment.getId(), 0);
    indexingService.index(AttachmentIndexingServiceConnector.TYPE, Long.toString(attachment.getId()));
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
  }
}
