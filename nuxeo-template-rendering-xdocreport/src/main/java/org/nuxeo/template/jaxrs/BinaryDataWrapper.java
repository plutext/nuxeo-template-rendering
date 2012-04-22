package org.nuxeo.template.jaxrs;

import org.nuxeo.ecm.core.api.Blob;

import fr.opensagres.xdocreport.remoting.resources.domain.BinaryData;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class BinaryDataWrapper {

    public static BinaryData wrap(Blob blob) throws Exception {
        return new BinaryData(blob.getStream(), blob.getFilename(),
                blob.getMimeType());
    }

}