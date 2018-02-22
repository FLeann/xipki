/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
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

package org.xipki.ca.server.mgmt.shell.completer;

import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.CaManager;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Service
public class ResponderNamePlusNullCompleter extends MgmtNameCompleter {

  @Override
  protected Set<String> getEnums() {
    Set<String> ret = new HashSet<>();
    ret.addAll(caManager.getResponderNames());
    ret.add(CaManager.NULL);
    return ret;
  }

}
