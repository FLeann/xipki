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

package org.xipki.ca.mgmt.shell;

import java.util.Set;

import org.xipki.ca.api.mgmt.PermissionConstants;
import org.xipki.password.PasswordResolver;
import org.xipki.security.SecurityFactory;
import org.xipki.shell.IllegalCmdParamException;
import org.xipki.util.Base64;
import org.xipki.util.ConfPairs;
import org.xipki.util.IoUtil;
import org.xipki.util.Args;
import org.xipki.util.StringUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class ShellUtil {

  private ShellUtil() {
  }

  public static String canonicalizeSignerConf(String keystoreType, String signerConf,
      PasswordResolver passwordResolver, SecurityFactory securityFactory) throws Exception {
    Args.notBlank(keystoreType, "keystoreType");
    Args.notBlank(signerConf, "signerConf");
    Args.notNull(securityFactory, "securityFactory");

    if (!signerConf.contains("file:") && !signerConf.contains("base64:")
        && !signerConf.contains("FILE:") && !signerConf.contains("BASE64:")) {
      return signerConf;
    }

    ConfPairs pairs = new ConfPairs(signerConf);
    String keystoreConf = pairs.value("keystore");
    String passwordHint = pairs.value("password");

    if (passwordHint == null) {
      throw new IllegalArgumentException("password is not set in " + signerConf);
    }

    byte[] keystoreBytes;
    if (StringUtil.startsWithIgnoreCase(keystoreConf, "file:")) {
      String keystoreFile = keystoreConf.substring("file:".length());
      keystoreBytes = IoUtil.read(keystoreFile);
    } else if (StringUtil.startsWithIgnoreCase(keystoreConf, "base64:")) {
      keystoreBytes = Base64.decode(keystoreConf.substring("base64:".length()));
    } else {
      return signerConf;
    }

    char[] password;
    if (passwordResolver == null) {
      password = passwordHint.toCharArray();
    } else {
      password = passwordResolver.resolvePassword(passwordHint);
    }

    String keyLabel = pairs.value("key-label");
    keystoreBytes = securityFactory.extractMinimalKeyStore(keystoreType, keystoreBytes,
        keyLabel, password, null);

    pairs.putPair("keystore", "base64:" + Base64.encodeToString(keystoreBytes));
    return pairs.getEncoded();
  } // method execute0

  public static int getPermission(Set<String> permissions) throws IllegalCmdParamException {
    int ret = 0;
    for (String permission : permissions) {
      Integer code = PermissionConstants.getPermissionForText(permission);
      if (code == null) {
        throw new IllegalCmdParamException("invalid permission '" + permission + "'");
      }
      ret |= code;
    }
    return ret;
  }

}
