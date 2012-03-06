/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.template.adapters.source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.AbstractTemplateDocument;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of {@link TemplateSourceDocument}. It mainly expect
 * from the underlying DocumentModel to have the "Template" facet.
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
 */
public class TemplateSourceDocumentAdapterImpl extends AbstractTemplateDocument
        implements Serializable, TemplateSourceDocument {

    public static final String TEMPLATE_DATA_PROP = "tmpl:templateData";

    public static final String TEMPLATE_TYPE_PROP = "tmpl:templateType";

    public static final String TEMPLATE_TYPE_AUTO = "auto";

    public static final String TEMPLATE_APPLICABLE_TYPES_PROP = "tmpl:applicableTypes";

    public static final String TEMPLATE_APPLICABLE_TYPES_ALL = "all";

    public static final String TEMPLATE_FORCED_TYPES_PROP = "tmpl:forcedTypes";

    public static final String TEMPLATE_FORCED_TYPES_NONE = "none";

    public static final String TEMPLATE_OUTPUT_PROP = "tmpl:outputFormat";

    public static final String TEMPLATE_OVERRIDE_PROP = "tmpl:allowOverride";

    public static final String TEMPLATE_USEASMAIN_PROP = "tmpl:useAsMainContent";

    public static final String TEMPLATE_RENDITION_PROP = "tmpl:targetRenditionName";

    public static final String TEMPLATE_FACET = "Template";

    private static final long serialVersionUID = 1L;

    public TemplateSourceDocumentAdapterImpl(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    protected String getTemplateParamsXPath() {
        return TEMPLATE_DATA_PROP;
    }

    public String getParamsAsString() throws PropertyException, ClientException {
        String dataPath = getTemplateParamsXPath();

        if (adaptedDoc.getPropertyValue(dataPath) == null) {
            return null;
        }
        return adaptedDoc.getPropertyValue(dataPath).toString();
    }

    public List<TemplateInput> addInput(TemplateInput input) throws Exception {

        List<TemplateInput> params = getParams();

        if (params == null) {
            params = new ArrayList<TemplateInput>();
        }

        params.add(input);
        saveParams(params, false);

        return params;
    }

    public String getTemplateType() {
        try {
            String ttype = (String) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_TYPE_PROP);
            if (TEMPLATE_TYPE_AUTO.equals(ttype)) {
                return null;
            }
            return ttype;
        } catch (Exception e) {
            log.error("Unable to read template type ", e);
            return null;
        }
    }

    public void initTemplate(boolean save) throws Exception {
        // avoid duplicate init
        if (getAdaptedDoc().getContextData(
                TemplateSourceDocument.INIT_DONE_FLAG) == null
                && getTemplateType() == null) {
            Blob blob = getTemplateBlob();
            if (blob != null) {
                TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
                String templateType = tps.findProcessorName(blob);
                if (templateType != null) {
                    getAdaptedDoc().setPropertyValue(TEMPLATE_TYPE_PROP,
                            templateType);
                }
                TemplateProcessor processor = getTemplateProcessor();
                if (processor != null) {
                    List<TemplateInput> params = processor.getInitialParametersDefinition(blob);
                    saveParams(params, save);
                }
                getAdaptedDoc().getContextData().put(
                        TemplateSourceDocument.INIT_DONE_FLAG, true);
            }
        }
    }

    public boolean allowInstanceOverride() {
        try {
            Boolean allowOverride = (Boolean) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_OVERRIDE_PROP);
            if (allowOverride == null) {
                allowOverride = true;
            }
            return allowOverride;
        } catch (Exception e) {
            log.error("Unable to read template allow override ", e);
            return false;
        }
    }

    public void initTypesBindings() throws Exception {

        // manage applicable types
        String[] applicableTypesArray = (String[]) getAdaptedDoc().getPropertyValue(
                TEMPLATE_APPLICABLE_TYPES_PROP);

        String[] newApplicableTypesArray = null;

        if (applicableTypesArray == null || applicableTypesArray.length == 0) {
            newApplicableTypesArray = new String[] { TEMPLATE_APPLICABLE_TYPES_ALL };
        } else if (applicableTypesArray.length > 1) {
            if (TEMPLATE_APPLICABLE_TYPES_ALL.equals(applicableTypesArray[0])) {
                List<String> at = Arrays.asList(applicableTypesArray);
                at.remove(0);
                newApplicableTypesArray = at.toArray(new String[at.size()]);
                ;
            }
        }
        if (newApplicableTypesArray != null) {
            getAdaptedDoc().setPropertyValue(TEMPLATE_APPLICABLE_TYPES_PROP,
                    newApplicableTypesArray);
        }

        // manage forcedTypes
        String[] forcedTypesArray = (String[]) getAdaptedDoc().getPropertyValue(
                TEMPLATE_FORCED_TYPES_PROP);
        String[] newForcedTypesArray = null;
        if (forcedTypesArray == null || forcedTypesArray.length == 0) {
            newForcedTypesArray = new String[] { TEMPLATE_FORCED_TYPES_NONE };
        } else if (forcedTypesArray.length > 1) {
            if (TEMPLATE_FORCED_TYPES_NONE.equals(forcedTypesArray[0])) {
                List<String> ft = Arrays.asList(forcedTypesArray);
                ft.remove(0);
                newForcedTypesArray = ft.toArray(new String[ft.size()]);
                ;
            }
        }
        if (newForcedTypesArray != null) {
            getAdaptedDoc().setPropertyValue(TEMPLATE_FORCED_TYPES_PROP,
                    newForcedTypesArray);
        }

    }

    public List<String> getApplicableTypes() {
        try {
            String[] applicableTypesArray = (String[]) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_APPLICABLE_TYPES_PROP);
            List<String> applicableTypes = new ArrayList<String>();
            if (applicableTypesArray != null) {
                applicableTypes.addAll((Arrays.asList(applicableTypesArray)));
            }
            if (applicableTypes.size() > 0
                    && applicableTypes.get(0).equals(
                            TEMPLATE_APPLICABLE_TYPES_ALL)) {
                applicableTypes.remove(0);
            }
            return applicableTypes;
        } catch (Exception e) {
            log.error("Error while reading applicable types");
            return new ArrayList<String>();
        }
    }

    public List<String> getForcedTypes() {
        try {
            String[] forcedTypesArray = (String[]) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_FORCED_TYPES_PROP);
            List<String> applicableTypes = new ArrayList<String>();
            if (forcedTypesArray != null) {
                applicableTypes.addAll((Arrays.asList(forcedTypesArray)));
            }
            if (applicableTypes.size() > 0
                    && applicableTypes.get(0).equals(TEMPLATE_FORCED_TYPES_NONE)) {
                applicableTypes.remove(0);
            }
            return applicableTypes;
        } catch (Exception e) {
            log.error("Error while reading applicable types");
            return new ArrayList<String>();
        }
    }

    public void removeForcedType(String type, boolean save)
            throws ClientException {
        List<String> types = getForcedTypes();
        if (types.contains(type)) {
            types.remove(type);
            String[] typesArray = types.toArray(new String[types.size()]);
            getAdaptedDoc().setPropertyValue(
                    TemplateSourceDocumentAdapterImpl.TEMPLATE_FORCED_TYPES_PROP,
                    typesArray);
            if (save) {
                adaptedDoc = getAdaptedDoc().getCoreSession().saveDocument(
                        getAdaptedDoc());
            }
        }
    }

    public void setForcedTypes(String[] forcedTypes, boolean save)
            throws ClientException {
        getAdaptedDoc().setPropertyValue(
                TemplateSourceDocumentAdapterImpl.TEMPLATE_FORCED_TYPES_PROP,
                forcedTypes);
        if (save) {
            adaptedDoc = getAdaptedDoc().getCoreSession().saveDocument(
                    getAdaptedDoc());
        }
    }

    public List<TemplateBasedDocument> getTemplateBasedDocuments()
            throws ClientException {
        return Framework.getLocalService(TemplateProcessorService.class).getLinkedTemplateBasedDocuments(
                adaptedDoc);
    }

    public String getOutputFormat() {
        try {
            return (String) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_OUTPUT_PROP);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean useAsMainContent() {
        try {
            Boolean useAsMain = (Boolean) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_USEASMAIN_PROP);
            if (useAsMain == null) {
                useAsMain = false;
            }
            return useAsMain;
        } catch (Exception e) {
            log.error("Unable to read template useAsMain prop ", e);
            return false;
        }
    }

    public Blob getTemplateBlob() throws ClientException {
        BlobHolder bh = getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh != null) {
            return bh.getBlob();
        }
        return null;
    }

    public String getName() {
        return getAdaptedDoc().getName();
    }

    public String getFileName() throws ClientException {
        Blob blob = getTemplateBlob();
        if (blob != null) {
            return blob.getFilename();
        }
        return null;
    }

    public String getTitle() throws ClientException {
        return getAdaptedDoc().getTitle();
    }

    public String getVersionLabel() {
        return getAdaptedDoc().getVersionLabel();
    }

    public String getId() {
        return getAdaptedDoc().getId();
    }

    public String getLabel() throws ClientException {
        StringBuffer sb = new StringBuffer(getTitle());
        if (!getTitle().equals(getFileName())) {
            sb.append(" (" + getFileName() + ")");
        }
        if (getVersionLabel() != null) {
            sb.append(" [" + getVersionLabel() + "]");
        }
        return sb.toString();
    }

    @Override
    public String getTargetRenditionName() throws ClientException {
        try {
            return (String) getAdaptedDoc().getPropertyValue(
                    TEMPLATE_RENDITION_PROP);
        } catch (Exception e) {
            log.error("Unable to get target rendition");
            return null;
        }
    }

}
