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

package org.xipki.ca.server;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.CertWithDbId;
import org.xipki.ca.api.mgmt.MgmtEntry;
import org.xipki.password.PasswordResolver;
import org.xipki.password.PasswordResolverException;
import org.xipki.security.HashAlgo;
import org.xipki.security.util.X509Util;
import org.xipki.util.Args;
import org.xipki.util.LogUtil;
import org.xipki.util.StringUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class RequestorEntryWrapper {
  private static final Logger LOG = LoggerFactory.getLogger(RequestorEntryWrapper.class);

  private MgmtEntry.Requestor dbEntry;

  private CertWithDbId cert;

  private byte[] keyId;

  private char[] password;

  public RequestorEntryWrapper() {
  }

  public void setDbEntry(MgmtEntry.Requestor dbEntry, PasswordResolver passwordResolver) {
    this.dbEntry = Args.notNull(dbEntry, "dbEntry");
    String type = dbEntry.getType();
    String conf = dbEntry.getConf();

    dbEntry.setFaulty(true);
    if (MgmtEntry.Requestor.TYPE_CERT.equalsIgnoreCase(type)) {
      try {
        X509Certificate x509Cert = X509Util.parseCert(StringUtil.toUtf8Bytes(conf));
        dbEntry.setFaulty(false);
        this.cert = new CertWithDbId(x509Cert);
      } catch (CertificateException ex) {
        LogUtil.error(LOG, ex, "error while parsing certificate of requestor" + dbEntry.getIdent());
      }
    } else if (MgmtEntry.Requestor.TYPE_PBM.equalsIgnoreCase(type)) {
      try {
        this.keyId = HashAlgo.SHA1.hash(StringUtil.toUtf8Bytes(dbEntry.getIdent().getName()));
        this.password = passwordResolver.resolvePassword(conf);
        dbEntry.setFaulty(false);
      } catch (PasswordResolverException ex) {
        LogUtil.error(LOG, ex, "error while resolve password of requestor" + dbEntry.getIdent());
      }
    }
  }

  public CertWithDbId getCert() {
    return cert;
  }

  public MgmtEntry.Requestor getDbEntry() {
    return dbEntry;
  }

  public boolean matchKeyId(byte[] keyId) {
    return Arrays.equals(keyId, this.keyId);
  }

  public char[] getPassword() {
    return password;
  }

}
