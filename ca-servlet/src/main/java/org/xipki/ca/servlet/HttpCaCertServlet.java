/*
 *
 * Copyright (c) 2013 - 2019 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.server.CaManagerImpl;
import org.xipki.security.X509Cert;
import org.xipki.util.Args;
import org.xipki.util.HttpConstants;

/**
 * TODO.
 * @author Lijun Liao
 * @since 3.0.1
 */

@SuppressWarnings("serial")
public class HttpCaCertServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(HttpCaCertServlet.class);

  private static final String CT_RESPONSE = "application/pkix-cert";

  private CaManagerImpl responderManager;

  public void setResponderManager(CaManagerImpl responderManager) {
    this.responderManager = Args.notNull(responderManager, "responderManager");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doService(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doService(req, resp);
  }

  protected void doService(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      String caName = null;
      String path = (String) req.getAttribute(HttpConstants.ATTR_XIPKI_PATH);
      if (path.length() > 1) {
        // skip the first char which is always '/'
        String caAlias = path.substring(1);
        caName = responderManager.getCaNameForAlias(caAlias);
        if (caName == null) {
          caName = caAlias.toLowerCase();
        }
      }

      X509Cert cacert = null;

      if (caName != null) {
        cacert = responderManager.getCaCert(caName);
      }

      if (cacert == null) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      byte[] encoded = cacert.getEncodedCert();
      resp.setContentType(CT_RESPONSE);
      resp.setContentLength(encoded.length);
      resp.getOutputStream().write(encoded);
    } catch (Throwable th) {
      LOG.error("Throwable thrown, this should not happen!", th);
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      resp.flushBuffer();
    }
  } // method service

  private static void sendError(HttpServletResponse resp, int status) {
    resp.setStatus(status);
    resp.setContentLength(0);
  }

}
