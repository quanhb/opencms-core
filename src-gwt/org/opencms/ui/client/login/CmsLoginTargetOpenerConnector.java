/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ui.client.login;

import org.opencms.ui.shared.login.I_CmsLoginTargetRpc;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.InputElement;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.shared.ui.Connect;

/**
 * Connector for the login target opener widget.<p>
 */
@Connect(org.opencms.ui.login.CmsLoginTargetOpener.class)
public class CmsLoginTargetOpenerConnector extends AbstractExtensionConnector {

    /** Default version id. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.<p>
     */
    public CmsLoginTargetOpenerConnector() {

    }

    /**
     * @see com.vaadin.client.extensions.AbstractExtensionConnector#extend(com.vaadin.client.ServerConnector)
     */
    @Override
    protected void extend(ServerConnector extendedComponent) {

        registerRpc(I_CmsLoginTargetRpc.class, new I_CmsLoginTargetRpc() {

            private static final long serialVersionUID = 1L;

            public void openTarget(String target) {

                // Post a hidden form with user name and password fields,
                // to hopefully trigger the browser's password manager
                Document doc = Document.get();
                FormElement formEl = (FormElement)doc.getElementById("opencms-login-form");

                // make sure user name and password are children of the form
                Element user = doc.getElementById("hidden-username");
                Element password = doc.getElementById("hidden-password");
                if (!formEl.isOrHasChild(user) || !formEl.isOrHasChild(password)) {
                    formEl.appendChild(user);
                    formEl.appendChild(password);
                }

                InputElement requestedResourceField = doc.createTextInputElement();
                requestedResourceField.setName("requestedResource");
                requestedResourceField.setValue(target);

                formEl.appendChild(requestedResourceField);
                formEl.submit();
            }
        });
    }

}
