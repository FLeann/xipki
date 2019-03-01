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

package org.xipki.ca.server.cmp;

import org.xipki.ca.api.CertWithDbId;
import org.xipki.ca.api.InsuffientPermissionException;
import org.xipki.ca.api.NameId;
import org.xipki.ca.api.mgmt.MgmtEntry;
import org.xipki.ca.api.mgmt.PermissionConstants;
import org.xipki.ca.api.mgmt.RequestorInfo;
import org.xipki.util.Args;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CmpRequestorInfo implements RequestorInfo {

  private final MgmtEntry.CaHasRequestor caHasRequestor;

  private final CertWithDbId cert;

  private final char[] password;

  private final byte[] keyId;

  public CmpRequestorInfo(MgmtEntry.CaHasRequestor caHasRequestor, CertWithDbId cert) {
    this.caHasRequestor = Args.notNull(caHasRequestor, "caHasRequestor");
    this.cert = Args.notNull(cert, "cert");
    this.password = null;
    this.keyId = null;
  }

  public CmpRequestorInfo(MgmtEntry.CaHasRequestor caHasRequestor, char[] password, byte[] keyId) {
    this.caHasRequestor = Args.notNull(caHasRequestor, "caHasRequestor");
    this.cert = null;
    this.password = Args.notNull(password, "password");
    this.keyId = Args.notNull(keyId, "keyId");
  }

  public MgmtEntry.CaHasRequestor getCaHasRequestor() {
    return caHasRequestor;
  }

  public CertWithDbId getCert() {
    return cert;
  }

  public char[] getPassword() {
    return password;
  }

  public byte[] getKeyId() {
    return keyId;
  }

  @Override
  public NameId getIdent() {
    return caHasRequestor.getRequestorIdent();
  }

  @Override
  public boolean isRa() {
    return caHasRequestor.isRa();
  }

  @Override
  public boolean isCertprofilePermitted(String certprofile) {
    return caHasRequestor.isCertprofilePermitted(certprofile);
  }

  @Override
  public boolean isPermitted(int permission) {
    return caHasRequestor.isPermitted(permission);
  }

  @Override
  public void assertCertprofilePermitted(String certprofile)
      throws InsuffientPermissionException {
    if (!isCertprofilePermitted(certprofile)) {
      throw new  InsuffientPermissionException("Certprofile " + certprofile + " is not permitted");
    }
  }

  @Override
  public void assertPermitted(int permission) throws InsuffientPermissionException {
    if (!isPermitted(permission)) {
      throw new  InsuffientPermissionException("Permission "
          + PermissionConstants.getTextForCode(permission) + " is not permitted");
    }
  }

}
