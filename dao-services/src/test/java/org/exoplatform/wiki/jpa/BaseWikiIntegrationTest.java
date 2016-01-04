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

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.rest.RestController;
import org.exoplatform.addons.es.dao.IndexingOperationDAO;
import org.exoplatform.addons.es.index.IndexingOperationProcessor;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.jpa.search.AttachmentIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 10/1/15
 */
public abstract class BaseWikiIntegrationTest extends BaseWikiJPAIntegrationTest {
  protected final URL fileResource = this.getClass().getClassLoader().getResource("AGT2010.DimitriBaeli.EnterpriseScrum-V1.2.pdf");
  private static final Log LOGGER = ExoLogger.getExoLogger(BaseWikiIntegrationTest.class);
  protected Node                       node;
  protected IndexingService            indexingService;
  protected IndexingOperationProcessor indexingOperationProcessor;
  protected JPADataStorage             storage;
  protected IndexingOperationDAO       indexingOperationDAO;

  /**
   * Port used for embedded Elasticsearch.
   * The port is needed to set the system properties exo.es.index.server.url and exo.es.search.server.url that
   * are used by the services ElasticIndexingClient and ElasticSearchingClient. These services read these
   * system properties at init (constructor) and cannot be changed later (no setter). Since the eXo Container is
   * created only once for all the tests, the same instance of these services are used for all the tests, so
   * the port must be the same for all the tests. That's why it must be static.
   */
  private static Integer esPort = null;

  @Override
  protected void beforeRunBare() {
    // start ES before creating the eXo Container since the ES port is necessary to initialize
    // the ElasticIndexingClient and ElasticSearchingClient services
    startEmbeddedES();

    super.beforeRunBare();
  }

  public void setUp() {
    super.setUp();

    // Init services
    indexingOperationDAO = PortalContainer.getInstance().getComponentInstanceOfType(IndexingOperationDAO.class);
    indexingService = PortalContainer.getInstance().getComponentInstanceOfType(IndexingService.class);
    indexingOperationProcessor = PortalContainer.getInstance().getComponentInstanceOfType(IndexingOperationProcessor.class);
    storage = PortalContainer.getInstance().getComponentInstanceOfType(JPADataStorage.class);

    // Init data
    deleteAllDocumentsInES();
    cleanDB();
    SecurityUtils.setCurrentUser("BCH", "*:/admin");
  }

  /**
   * Start an Elasticsearch instance
   */
  private void startEmbeddedES() {
    if(esPort == null) {
      try {
        esPort = getAvailablePort();
      } catch (IOException e) {
        fail("Cannot get available port : " + e.getMessage());
      }
    }

    // Init ES
    LOGGER.info("Embedded ES instance - Starting on port " + esPort);
    ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder()
            .put(RestController.HTTP_JSON_ENABLE, true)
            .put(InternalNode.HTTP_ENABLED, true)
            .put("network.host", "127.0.0.1")
            .put("http.port", esPort)
            .put("path.data", "target/data")
            .put("plugins."
                            + PluginsService.LOAD_PLUGIN_FROM_CLASSPATH,
                    true);
    node = nodeBuilder().local(true).settings(elasticsearchSettings.build()).node();
    node.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
    assertNotNull(node);
    assertFalse(node.isClosed());
    LOGGER.info("Embedded ES instance - Started");
    // Set URL of server in property
    NodesInfoRequest nodesInfoRequest = new NodesInfoRequest().transport(true);
    NodesInfoResponse response = node.client().admin().cluster().nodesInfo(nodesInfoRequest).actionGet();
    NodeInfo nodeInfo = response.iterator().next();
    InetSocketTransportAddress address = (InetSocketTransportAddress) nodeInfo.getHttp().getAddress().publishAddress();
    String url = "http://" + address.address().getHostName() + ":" + address.address().getPort();
    PropertyManager.setProperty("exo.es.index.server.url", url);
    PropertyManager.setProperty("exo.es.search.server.url", url);
  }


  public void tearDown() {

    super.tearDown();

    LOGGER.info("Clean Indexing database");
    cleanDB();

    // Close ES Node
    LOGGER.info("Embedded ES instance - Stopping");
    node.close();
    LOGGER.info("Embedded ES instance - Stopped");
  }

  /**
   * Get a random available port
   * @return
   * @throws IOException
   */
  private int getAvailablePort() throws IOException {
    ServerSocket ss = null;
    DatagramSocket ds = null;
    try {
      ss = new ServerSocket(0);
      ss.setReuseAddress(true);
      ds = new DatagramSocket(0);
      ds.setReuseAddress(true);
      return ss.getLocalPort();
    } finally {
      if (ds != null) {
        ds.close();
      }

      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
                /* should not be thrown */
        }
      }
    }
  }

  private void cleanDB() {
    indexingOperationDAO.deleteAll();
  }

  private void deleteAllDocumentsInES() {
    indexingService.unindexAll(WikiPageIndexingServiceConnector.TYPE);
    indexingService.unindexAll(AttachmentIndexingServiceConnector.TYPE);
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
  }

  protected PageEntity indexPage(String name, String title, String content, String comment, String owner,
                                 List<PermissionEntity> permissions) throws NoSuchFieldException, IllegalAccessException {
    WikiEntity wiki = new WikiEntity();
    wiki.setOwner("BCH");
    wiki.setType("test");
    wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setName(name);
    page.setTitle(title);
    page.setContent(content);
    page.setComment(comment);
    page.setOwner(owner);
    page.setPermissions(permissions);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setUrl("/url/to/my/wikiPage");
    page.setWiki(wiki);
    page = pageDAO.create(page);
    assertNotEquals(page.getId(), 0);
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, Long.toString(page.getId()));
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    return page;
  }

  protected PageAttachmentEntity indexAttachment(String title, String filePath, String downloadedUrl, String owner) throws NoSuchFieldException,
                                                                                     IllegalAccessException,
                                                                                     IOException {
    WikiEntity wiki = new WikiEntity();
    wiki.setOwner("BCH");
    wiki.setType("test");
    wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setUrl("/url/to/my/wikiPage");
    page.setWiki(wiki);
    pageDAO.create(page);
    PageAttachmentEntity attachment = new PageAttachmentEntity();
    attachment.setTitle(title);
    attachment.setContent(Files.readAllBytes(Paths.get(filePath)));
    attachment.setCreatedDate(new Date());
    attachment.setUpdatedDate(new Date());
    attachment.setCreator(owner);
    attachment.setPage(page);
    attachment = pageAttachmentDAO.create(attachment);
    assertNotEquals(attachment.getId(), 0);
    indexingService.index(AttachmentIndexingServiceConnector.TYPE, Long.toString(attachment.getId()));
    indexingOperationProcessor.process();
    node.client().admin().indices().prepareRefresh().execute().actionGet();
    return attachment;
  }
}