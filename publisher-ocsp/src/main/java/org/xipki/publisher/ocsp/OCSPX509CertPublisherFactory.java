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

package org.xipki.publisher.ocsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xipki.ca.api.publisher.X509CertPublisher;
import org.xipki.ca.api.publisher.X509CertPublisherFactory;
import org.xipki.common.ObjectCreationException;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

// CHECKSTYLE:SKIP
public class OCSPX509CertPublisherFactory implements X509CertPublisherFactory {

  private static final String TYPE = "ocsp";

  private static final Set<String> types = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(TYPE)));

  @Override
  public Set<String> getSupportedTypes() {
    return types;
  }

  @Override
  public boolean canCreatePublisher(String type) {
    return types.contains(type.toLowerCase());
  }

  @Override
  public X509CertPublisher newPublisher(String type) throws ObjectCreationException {
    if (TYPE.equalsIgnoreCase(type)) {
      return new OcspCertPublisher();
    } else {
      throw new ObjectCreationException("unknown publisher type " + type);
    }
  }

}
