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

package org.xipki.ca.api.mgmt;

import org.xipki.ca.api.InsuffientPermissionException;
import org.xipki.ca.api.NameId;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public interface RequestorInfo {

  static final String NAME_BY_USER = "BY-USER";

  static final String NAME_BY_CA = "BY-CA";

  NameId getIdent();

  boolean isRa();

  boolean isCertprofilePermitted(String certprofile);

  boolean isPermitted(int requiredPermission);

  void assertCertprofilePermitted(String certprofile) throws InsuffientPermissionException;

  void assertPermitted(int requiredPermission) throws InsuffientPermissionException;

}
