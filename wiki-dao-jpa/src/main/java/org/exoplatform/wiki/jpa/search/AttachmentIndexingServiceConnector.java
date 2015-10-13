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

package org.exoplatform.wiki.jpa.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addons.es.domain.Document;
import org.exoplatform.addons.es.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.dao.AttachmentDAO;
import org.exoplatform.wiki.jpa.entity.Attachment;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 10/2/15
 */
public class AttachmentIndexingServiceConnector  extends ElasticIndexingServiceConnector {
    private static final Log LOGGER = ExoLogger.getExoLogger(AttachmentIndexingServiceConnector.class);
    public static final String TYPE = "wiki-attachment";
    private final AttachmentDAO dao;

    public AttachmentIndexingServiceConnector(InitParams initParams, AttachmentDAO dao) {
        super(initParams);
        this.dao = dao;
    }

    @Override
    public Document create(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Id is null");
        }
        //Get the wiki object from BD
        Attachment attachment = dao.find(Long.parseLong(id));
        if (attachment==null) {
            LOGGER.info("The attachment entity with id {} doesn't exist.", id);
            return null;
        }
        Map<String,String> fields = new HashMap<>();
        Document doc = new Document(TYPE, id, attachment.getDownloadURL(), attachment.getUpdatedDate(), computePermissions(attachment), fields);
        doc.addField("title", attachment.getTitle());
        doc.addField("file", attachment.getContent());
        return doc;
    }

    @Override
    public Document update(String id) {
        return create(id);
    }

    private String[] computePermissions(Attachment attachment) {
        List<String> permissions = new ArrayList<>();
        permissions.add(attachment.getCreator());
        //TODO Add the permissions
        String[] result = new String[permissions.size()];
        return permissions.toArray(result);
    }

    @Override
    public String getMapping() {
        return "{\"properties\" : {\n" +
                "      \"file\" : {\n" +
                "        \"type\" : \"attachment\",\n" +
                "        \"fields\" : {\n" +
                "          \"file\" : { \"term_vector\":\"with_positions_offsets\", \"store\":true }\n" +
                "        }\n" +
                "      },\n" +
                "      \"permissions\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\" }\n" +
                "    }" +
                "}";
    }

    @Override
    public List<String> getAllIds() {
        throw new RuntimeException("Not implemented");
    }
}